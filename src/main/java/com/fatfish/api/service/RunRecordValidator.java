package com.fatfish.api.service;

import com.fatfish.api.dto.RunRecord;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validates a single RunRecord and returns the reasons (in Spanish) why it is rejected.
 * An empty list means the record is valid.
 */
@Component
public class RunRecordValidator {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final Validator validator;

    public RunRecordValidator(Validator validator) {
        this.validator = validator;
    }

    public List<String> validate(RunRecord record) {
        if (record == null) {
            return List.of("La partida no puede ser nula");
        }

        List<String> reasons = new ArrayList<>(validator.validate(record).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList());

        if (record.schemaVersion() != null && record.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            reasons.add("schemaVersion " + record.schemaVersion() + " no soportada (se espera "
                    + SUPPORTED_SCHEMA_VERSION + ")");
        }
        if (record.startedAt() != null && record.endedAt() != null
                && !record.endedAt().isAfter(record.startedAt())) {
            reasons.add("endedAt debe ser posterior a startedAt");
        }
        return reasons;
    }
}
