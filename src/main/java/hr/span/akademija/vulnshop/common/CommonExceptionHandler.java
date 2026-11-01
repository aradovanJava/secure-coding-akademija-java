package hr.span.akademija.vulnshop.common;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Zajednicki rukovatelj iznimkama autentikacije/autorizacije.
 *
 * Namjerno obraduje SAMO {@link UnauthorizedException} (401) i
 * {@link ForbiddenException} (403). Sve ostale iznimke prepustaju se Springovom
 * zadanom rukovanju greskama (BasicErrorController), jer vjezbe lab04 i lab15
 * namjerno pokazuju razliku izmedu "procurivanja" detalja greske i generickih
 * poruka, sto se ovdje NE smije presresti.
 */
@RestControllerAdvice
public class CommonExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }
}
