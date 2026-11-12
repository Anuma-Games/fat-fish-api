package com.fatfish.api.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Rangos de IP de proxies en los que se confía al leer X-Forwarded-For. Firebase Hosting reenvía /api/** a Cloud
 * Run desde IPs de Google, así que esa IP no es la del jugador: la del jugador va justo antes.
 */
final class TrustedProxies {

    /** Rangos propios de Google (no de clientes de Google Cloud) desde los que llega el reenvío de Firebase Hosting. */
    static final String GOOGLE = String.join(",",
            "64.233.160.0/19", "66.102.0.0/20", "66.249.64.0/19", "72.14.192.0/18", "74.125.0.0/16",
            "108.177.8.0/21", "108.177.96.0/19", "142.250.0.0/15", "172.217.0.0/16", "172.253.0.0/16",
            "173.194.0.0/16", "192.178.0.0/15", "209.85.128.0/17", "216.58.192.0/19", "216.239.32.0/19",
            "35.191.0.0/16", "130.211.0.0/22",
            "2001:4860::/32", "2404:6800::/32", "2607:f8b0::/32", "2800:3f0::/32", "2a00:1450::/32",
            "2c0f:fb50::/32");

    /** Solo literales de IP: así InetAddress nunca hace una consulta de DNS con lo que mande el cliente. */
    private static final Pattern IPV4 = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]*:[0-9a-fA-F:.]*");

    private record Range(byte[] network, int prefix) {
    }

    private final List<Range> ranges;

    private TrustedProxies(List<Range> ranges) {
        this.ranges = ranges;
    }

    /** Lee una lista de rangos CIDR separados por comas, por ejemplo "74.125.0.0/16,2001:4860::/32". */
    static TrustedProxies parse(String cidrs) {
        return new TrustedProxies(Arrays.stream(cidrs.split(","))
                .map(String::strip)
                .filter(cidr -> !cidr.isEmpty())
                .map(TrustedProxies::range)
                .toList());
    }

    /** True si la IP cae en alguno de los rangos. Lo que no sea una IP literal nunca es de confianza. */
    boolean contains(String ip) {
        byte[] address = parseIp(ip);
        return address != null && ranges.stream().anyMatch(range -> matches(range, address));
    }

    /** Falla al arrancar si un rango está mal escrito, en lugar de fallar después en cada petición. */
    private static Range range(String cidr) {
        String[] parts = cidr.split("/");
        byte[] network = parts.length == 2 ? parseIp(parts[0]) : null;
        if (network == null || !parts[1].matches("\\d{1,3}")) {
            throw new IllegalArgumentException("Rango de proxy inválido: " + cidr);
        }
        int prefix = Integer.parseInt(parts[1]);
        if (prefix > network.length * 8) {
            throw new IllegalArgumentException("Rango de proxy inválido: " + cidr);
        }
        return new Range(network, prefix);
    }

    private static boolean matches(Range range, byte[] address) {
        if (address.length != range.network().length) {
            return false;
        }
        int fullBytes = range.prefix() / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (address[i] != range.network()[i]) {
                return false;
            }
        }
        int remainingBits = range.prefix() % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (address[fullBytes] & mask) == (range.network()[fullBytes] & mask);
    }

    private static byte[] parseIp(String ip) {
        if (ip == null) {
            return null;
        }
        if (IPV4.matcher(ip).matches()) {
            byte[] address = new byte[4];
            String[] octets = ip.split("\\.");
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(octets[i]);
                if (octet > 255) {
                    return null;
                }
                address[i] = (byte) octet;
            }
            return address;
        }
        if (IPV6.matcher(ip).matches()) {
            try {
                // Entre corchetes Java exige una IPv6 literal y no consulta el DNS
                return InetAddress.getByName("[" + ip + "]").getAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        }
        return null;
    }
}
