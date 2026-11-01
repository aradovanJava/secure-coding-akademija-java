package hr.span.akademija.vulnshop.lab13;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab13 – A08 Software or Data Integrity Failures: nesigurna deserijalizacija
 * (CWE-502).
 *
 * RANJIVO: tijelo zahtjeva je Base64 Java-serijaliziranog objekta koji se
 * deserijalizira preko {@code ObjectInputStream.readObject()} BEZ ikakvog filtra.
 * Napadac moze poslati {@link DiagnosticsTask} umjesto {@link Cart} i izvrsiti
 * proizvoljan zapis datoteke jos tijekom deserijalizacije.
 */
@RestController
@RequestMapping("/api/lab13")
public class Lab13Controller {

    @PostMapping("/cart/import")
    public ResponseEntity<?> importCart(@RequestBody String base64Body) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Body.trim());
            // RANJIVO: readObject bez ObjectInputFilter-a deserijalizira bilo koju klasu.
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                Object obj = ois.readObject();
                if (obj instanceof Cart cart) {
                    return ResponseEntity.ok(Map.of("status", "ok", "itemCount", cart.getItems().size()));
                }
                return ResponseEntity.ok(Map.of("status", "ok", "type", obj.getClass().getSimpleName()));
            }
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan sadrzaj kosarice."));
        }
    }
}
