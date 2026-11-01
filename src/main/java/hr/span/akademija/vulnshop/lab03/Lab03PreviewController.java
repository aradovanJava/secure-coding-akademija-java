package hr.span.akademija.vulnshop.lab03;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab03 – A01 Broken Access Control: Server-Side Request Forgery (CWE-918).
 *
 * RANJIVO: endpoint "pregled poveznice" dohvaca proizvoljan URL koji je zadao
 * korisnik i vraca pocetak sadrzaja. Nema provjere sheme ni odredista, pa
 * napadac moze dosegnuti interne servise (loopback, cloud metadata 169.254.169.254)
 * ili lokalne datoteke (file://).
 */
@RestController
@RequestMapping("/api/lab03")
public class Lab03PreviewController {

    private static final int MAX_CHARS = 2000;

    @GetMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String url, CurrentUser currentUser) {
        try {
            // RANJIVO: nikakva validacija sheme/hosta; slijede se i preusmjeravanja.
            URL target = URI.create(url).toURL();
            URLConnection connection = target.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            try (InputStream in = connection.getInputStream()) {
                byte[] data = in.readNBytes(MAX_CHARS);
                String body = new String(data, StandardCharsets.UTF_8);
                return ResponseEntity.ok(Map.of("url", url, "content", body));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan URL."));
        } catch (Exception e) {
            // Nepostojeca shema (npr. ftp) ili greska dohvata -> 502.
            return ResponseEntity.status(502).body(Map.of("error", "Dohvat nije uspio: " + e.getMessage()));
        }
    }
}
