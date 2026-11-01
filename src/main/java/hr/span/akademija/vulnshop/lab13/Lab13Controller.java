package hr.span.akademija.vulnshop.lab13;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputFilter;
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
 * POPRAVAK: deserijalizacija je ograничena {@link ObjectInputFilter} allowlistom
 * koja dopusta samo {@link Cart}, {@link CartItem} i nuzne JDK kolekcije; sve ostalo
 * (ukljucujuci {@link DiagnosticsTask}) se odbija prije instanciranja. Dodatno su
 * ograniceni dubina i velicina grafа.
 *
 * Napomena: u stvarnom projektu Java serijalizaciju za nepouzdane podatke treba
 * posve izbjeci i koristiti JSON s eksplicitnim DTO-om (uz HMAC potpis po potrebi).
 */
@RestController
@RequestMapping("/api/lab13")
public class Lab13Controller {

    // POPRAVAK: allowlista dopustenih klasa + ograniceni dubina/velicina.
    private static final ObjectInputFilter ALLOWLIST = ObjectInputFilter.Config.createFilter(
            "hr.span.akademija.vulnshop.lab13.Cart;"
                    + "hr.span.akademija.vulnshop.lab13.CartItem;"
                    + "java.util.ArrayList;java.lang.*;"
                    + "maxdepth=5;maxbytes=10000;!*");

    @PostMapping("/cart/import")
    public ResponseEntity<?> importCart(@RequestBody String base64Body) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Body.trim());
            try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
                // POPRAVAK: postavi filtar prije readObject; nedopustene klase bacaju InvalidClassException.
                ois.setObjectInputFilter(ALLOWLIST);
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
