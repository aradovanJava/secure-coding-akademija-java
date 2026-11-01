package hr.span.akademija.vulnshop.lab08;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab08 – A05 Injection: Cross-Site Scripting (CWE-79).
 *
 * RANJIVO: HTML stranice se grade konkatenacijom stringova s korisnickim unosom,
 * bez kodiranja izlaza. Reflektirani XSS u pretrazi ({@code /lab08/search}) i
 * pohranjeni XSS u recenzijama ({@code /lab08/products/{id}/reviews}).
 */
@RestController
public class Lab08Controller {

    private final JdbcTemplate jdbc;

    public Lab08Controller(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** RANJIVO: reflektirani XSS – q se upisuje izravno u HTML. */
    @GetMapping(value = "/lab08/search", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> search(@RequestParam(defaultValue = "") String q) {
        String html = "<!DOCTYPE html><html lang=\"hr\"><head><meta charset=\"UTF-8\">"
                + "<title>Pretraga</title></head><body>"
                + "<h1>Rezultati pretrage za: " + q + "</h1>"
                + "</body></html>";
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    public record ReviewRequest(long productId, String author, String text) {
    }

    @PostMapping("/api/lab08/reviews")
    public ResponseEntity<?> addReview(@RequestBody ReviewRequest body) {
        jdbc.update("INSERT INTO lab08_reviews (product_id, author, text) VALUES (?, ?, ?)",
                body.productId(), body.author(), body.text());
        return ResponseEntity.ok(Map.of("status", "spremljeno"));
    }

    /** RANJIVO: pohranjeni XSS – author i text se upisuju izravno u HTML. */
    @GetMapping(value = "/lab08/products/{id}/reviews", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> reviews(@PathVariable long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT author, text FROM lab08_reviews WHERE product_id = ? ORDER BY id", id);
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"hr\"><head><meta charset=\"UTF-8\">")
                .append("<title>Recenzije</title></head><body>")
                .append("<h1>Recenzije za proizvod ").append(id).append("</h1>");
        for (Map<String, Object> row : rows) {
            html.append("<div class=\"review\"><strong>").append(row.get("author")).append("</strong>: ")
                    .append(row.get("text")).append("</div>");
        }
        html.append("</body></html>");
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html.toString());
    }
}
