package hr.span.akademija.vulnshop.lab04;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab04 – A02 Security Misconfiguration (CWE-16, CWE-209, CWE-942, CWE-693).
 *
 * Vecina ranjivosti ove vjezbe je u konfiguraciji ({@code application.properties}:
 * izlozeni actuator endpointi, {@code /actuator/env} otkriva tajne, stack trace u
 * odgovorima) i u {@link Lab04CorsConfig} (previse popustljiv CORS, nedostatak
 * sigurnosnih zaglavlja).
 *
 * Ovdje su samo demonstracijski endpointi: {@code /ping} (zdrav) i {@code /crash}
 * (baca iznimku s detaljnom porukom kako bi se vidjelo procurivanje stack tracea).
 */
@RestController
@RequestMapping("/api/lab04")
public class Lab04Controller {

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "pong"));
    }

    @GetMapping("/crash")
    public ResponseEntity<?> crash() {
        // Namjerno baca iznimku s osjetljivom porukom (simulira internu gresku).
        throw new IllegalStateException(
                "Neocekivana greska pri obradi narudzbe: veza s bazom jdbc:h2:mem:vulnshop je pala");
    }
}
