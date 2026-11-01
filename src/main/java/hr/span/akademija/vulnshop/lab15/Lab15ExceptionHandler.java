package hr.span.akademija.vulnshop.lab15;

import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Lab15 – POPRAVAK: globalni rukovatelj iznimkama ograничen na lab15 paket.
 *
 * Vraca genericku poruku s korelacijskim ID-om (bez naziva klase iznimke, SQL-a ili
 * stack tracea). Detalji se zapisuju samo na posluzitelju (log), uz isti ID. Status
 * je 400 jer u ovoj vjezbi iznimke proizlaze iz neispravnog zahtjeva (npr. nepostojeci
 * odredisni racun).
 *
 * Namjerno je ograничen na {@code basePackages} = lab15 kako NE bi presreo lab04
 * {@code /crash} (koji mora ostati 500 bez detalja) ni druge vjezbe.
 */
@RestControllerAdvice(basePackages = "hr.span.akademija.vulnshop.lab15")
public class Lab15ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Lab15ExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        // Detalji idu SAMO u log (na posluzitelju), ne u odgovor.
        log.warn("Lab15 greska [{}]: {}", correlationId, ex.toString());
        return ResponseEntity.status(400).body(Map.of(
                "error", "Zahtjev se ne moze obraditi.",
                "correlationId", correlationId));
    }
}
