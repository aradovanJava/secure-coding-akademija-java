package hr.span.akademija.vulnshop.lab08;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab08 – sigurnosni testovi (XSS).
 */
@Tag("security")
@Tag("lab08")
class Lab08SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Reflektirani XSS: odgovor ne smije sadrzavati nekodirani <script>")
    void reflektiraniXssKodiran() {
        String payload = "<script>alert(1)</script>";
        String encoded = java.net.URLEncoder.encode(payload, java.nio.charset.StandardCharsets.UTF_8);
        ResponseEntity<String> response = rest.getForEntity(url("/lab08/search?q=" + encoded), String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        assertThat(body)
                .as("Nekodirani <script> u odgovoru - reflektirani XSS")
                .doesNotContain("<script>alert(1)</script>");
        assertThat(body)
                .as("Payload mora biti HTML-kodiran")
                .contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Pohranjeni XSS: spremljena recenzija s <img onerror> mora biti kodirana u izlazu")
    void pohranjeniXssKodiran() {
        long productId = 500 + (System.nanoTime() % 1000);
        rest.postForEntity(url("/api/lab08/reviews"),
                Map.of("productId", productId, "author", "napadac",
                        "text", "<img src=x onerror=alert('XSS')>"), Map.class);

        ResponseEntity<String> response = rest.getForEntity(
                url("/lab08/products/" + productId + "/reviews"), String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        assertThat(body)
                .as("Nekodirani <img onerror> u odgovoru - pohranjeni XSS")
                .doesNotContain("<img src=x onerror=");
    }

    @Test
    @DisplayName("Odgovor mora imati Content-Security-Policy zaglavlje sa script-src")
    void cspZaglavljePostoji() {
        String encoded = java.net.URLEncoder.encode("test", java.nio.charset.StandardCharsets.UTF_8);
        ResponseEntity<String> response = rest.getForEntity(url("/lab08/search?q=" + encoded), String.class);
        String csp = response.getHeaders().getFirst("Content-Security-Policy");
        assertThat(csp)
                .as("Nedostaje CSP zaglavlje")
                .isNotNull();
        assertThat(csp)
                .as("CSP ne sadrzi script-src")
                .contains("script-src");
    }
}
