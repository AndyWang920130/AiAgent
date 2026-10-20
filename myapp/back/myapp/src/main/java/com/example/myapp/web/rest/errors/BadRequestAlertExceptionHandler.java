package com.example.myapp.web.rest.errors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Renders {@link BadRequestAlertException} as a body the frontend can show.
 *
 * <p>The exception builds a {@code ProblemDetail}, but without this advice Spring falls back
 * to the default error attributes, which carry no message at all — so a rejected request
 * reached the browser as a bare 400 and the reason was lost. The body uses {@code message},
 * the key the rest of the API already returns for errors, plus {@code entityName} and
 * {@code errorKey} for callers that want to branch on the specific rule.
 */
@RestControllerAdvice
public class BadRequestAlertExceptionHandler {

    @ExceptionHandler(BadRequestAlertException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestAlert(BadRequestAlertException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", ex.getBody().getDetail());
        body.put("entityName", ex.getEntityName());
        body.put("errorKey", ex.getErrorKey());
        return ResponseEntity.badRequest().body(body);
    }
}
