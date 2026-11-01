package hr.span.akademija.vulnshop.lab03;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Interni endpoint koji vraca tajne SAMO ako zahtjev dolazi s loopback adrese.
 * Sluzi kao meta za SSRF vjezbu (lab03): napadac preko {@code /api/lab03/preview}
 * natjera server da sam sebi posalje zahtjev i tako procita tajne.
 *
 * Ovaj endpoint nije zasticen Bearer tokenom jer simulira "interni" servis
 * dostupan samo s lokalne mreze/hosta.
 */
@RestController
public class InternalController {

    @GetMapping("/internal/admin/secrets")
    public ResponseEntity<?> secrets(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isLoopback(remote)) {
            return ResponseEntity.status(403).body(Map.of("error", "Dostupno samo s loopback adrese."));
        }
        return ResponseEntity.ok(Map.of(
                "dbPassword", "Sup3rTajn0!",
                "paymentApiKey", "sk_live_akademija_123"));
    }

    private boolean isLoopback(String remote) {
        if (remote == null) {
            return false;
        }
        return remote.equals("127.0.0.1")
                || remote.equals("0:0:0:0:0:0:0:1")
                || remote.equals("::1")
                || remote.startsWith("127.");
    }
}
