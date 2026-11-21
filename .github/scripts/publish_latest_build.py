"""Publica el último build de main y después elimina solo sus releases automáticas anteriores."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess


PREFIX = "build-main-"


def gh(repo, *args):
    result = subprocess.run(["gh", *args, "--repo", repo] if args[0] == "release"
                            else ["gh", *args], check=True, capture_output=True, text=True)
    return result.stdout.strip()


def is_current(repo, sha):
    return gh(repo, "api", f"repos/{repo}/git/ref/heads/main", "--jq", ".object.sha") == sha


def publish(repo, sha, run_id, attempt, asset):
    # Los reintentos de commits antiguos nunca reemplazan una descarga más reciente.
    if not is_current(repo, sha):
        print("Se conserva la descarga anterior: este commit ya no es la punta de main.")
        return
    tag = f"{PREFIX}{run_id}-{attempt}"
    notes = (f"Último build comprobado de main.\n\nCommit: `{sha}`\n\n"
             f"CI: https://github.com/{repo}/actions/runs/{run_id}\n\n"
             "Esta descarga no caduca; se reemplaza después del próximo CI exitoso de main.")
    # El borrador permite subir todo antes de cambiar la última descarga pública.
    gh(repo, "release", "create", tag, str(asset), "--draft", "--target", sha,
       "--title", "Última compilación de main", "--notes", notes)
    if not is_current(repo, sha):
        print("main cambió durante la subida; se conserva la descarga publicada anterior.")
        gh(repo, "release", "delete", tag, "--yes", "--cleanup-tag")
        return
    gh(repo, "release", "edit", tag, "--draft=false", "--latest")
    pages = json.loads(gh(repo, "api", f"repos/{repo}/releases?per_page=100", "--paginate", "--slurp"))
    for release in (item for page in pages for item in page):
        old = release["tag_name"]
        match = re.fullmatch(r"build-main-(\d+)-(\d+)", old)
        if match and tuple(map(int, match.groups())) < (int(run_id), int(attempt)):
            # Nunca toca releases manuales ni una compilación posterior.
            gh(repo, "release", "delete", old, "--yes", "--cleanup-tag")
    url = f"https://github.com/{repo}/releases/latest"
    print(f"Última descarga: {url}")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with Path(os.environ["GITHUB_STEP_SUMMARY"]).open("a") as output:
            output.write(f"## Última compilación\n\n[Descargar]({url})\n\nCommit: `{sha}`\n")


def prepare_asset(source, name, zipped):
    source = Path(source)
    files = sorted(p for p in source.rglob("*") if p.is_file())
    if not files:
        raise ValueError("No hay archivos para publicar")
    destination = Path("release-output")
    destination.mkdir(exist_ok=True)
    asset = destination / name
    if zipped:
        shutil.make_archive(str(asset.with_suffix("")), "zip", source)
    else:
        if len(files) != 1 or files[0].suffix != asset.suffix:
            raise ValueError("Se esperaba un único APK o JAR")
        shutil.copy2(files[0], asset)
    return asset


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--zip", action="store_true")
    args = parser.parse_args()
    if os.environ["GITHUB_REF"] != "refs/heads/main" or os.environ["GITHUB_EVENT_NAME"] not in {"push", "workflow_dispatch"}:
        raise ValueError("Solo se publican builds de main, nunca de pull requests")
    asset = prepare_asset(args.source, args.name, args.zip)
    publish(os.environ["GITHUB_REPOSITORY"], os.environ["GITHUB_SHA"],
            os.environ["GITHUB_RUN_ID"], os.environ["GITHUB_RUN_ATTEMPT"], asset)


if __name__ == "__main__":
    main()
