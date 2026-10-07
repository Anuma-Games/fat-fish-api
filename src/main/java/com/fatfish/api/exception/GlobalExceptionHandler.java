package com.fatfish.api.exception;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Translates exceptions into RFC 9457 problem details with user-facing messages in Spanish. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PlayerNotFoundException.class)
    ProblemDetail handlePlayerNotFound(PlayerNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Jugador no encontrado", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex) {
        return validationProblem(ex.getBindingResult().getAllErrors());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex) {
        return validationProblem(ex.getAllErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El cuerpo de la solicitud no es un JSON válido o tiene campos con formato incorrecto");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "El parámetro '" + ex.getName() + "' tiene un formato inválido");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation while saving", ex);
        return problem(HttpStatus.CONFLICT, "Conflicto al guardar",
                "Los datos entran en conflicto con información existente; intenta enviar el lote de nuevo");
    }

    private static ProblemDetail validationProblem(List<? extends MessageSourceResolvable> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Solicitud inválida",
                "La solicitud no pasó las validaciones");
        problem.setProperty("errors", errors.stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .sorted()
                .toList());
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
