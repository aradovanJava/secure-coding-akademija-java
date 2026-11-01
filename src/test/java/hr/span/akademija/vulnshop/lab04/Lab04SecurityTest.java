package hr.span.akademija.vulnshop.lab04;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Lab04 – sigurnosni testovi (Security Misconfiguration).
 */
@Tag("security")
@Tag("lab04")
class Lab04SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("/actuator/env ne smije biti dostupan niti otkrivati tajne")
    void actuatorEnvNijeIzlozen() {
        ResponseEntity<String> response = rest.getForEntity(url("/actuator/env"), String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        // Sigurno: env endpoint nije dostupan (404/401/403) i ne otkriva API kljuc.
        assertThat(response.getStatusCode().value())
                .as("/actuator/env je izlozen")
                .isIn(404, 401, 403);
        assertThat(body)
                .as("Tajni kljuc procurio kroz /actuator/env")
                .doesNotContain("sk_live_akademija_123");
    }

    @Test
    @DisplayName("/api/lab04/crash vraća 500 bez stack tracea i naziva paketa")
    void crashBezStackTracea() {
        ResponseEntity<String> response = rest.getForEntity(url("/api/lab04/crash"), String.class);
        String body = response.getBody() == null ? "" : response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(body)
                .as("Stack trace / detalji iznimke procurili u odgovoru")
                .doesNotContain("hr.span.akademija")
                .doesNotContain("Exception")
                .doesNotContain("at ")
                .doesNotContain("jdbc:h2");
    }

    @Test
    @DisplayName("CORS preflight sa zlonamjernog origina ne smije biti dopusten")
    void corsNeDopustaEvilOrigin() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Origin", "https://evil.example");
        headers.add("Access-Control-Request-Method", "GET");
        ResponseEntity<String> response = rest.exchange(url("/api/lab04/ping"),
                HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);
        String allowOrigin = response.getHeaders().getFirst("Access-Control-Allow-Origin");
        assertThat(allowOrigin)
                .as("CORS reflektira zlonamjerni origin")
                .isNotEqualTo("https://evil.example")
                .isNotEqualTo("*");
    }

    @Test
    @DisplayName("/api/lab04/ping mora imati sigurnosna zaglavlja (nosniff, X-Frame-Options DENY)")
    void pingImaSigurnosnaZaglavlja() {
        ResponseEntity<String> response = rest.getForEntity(url("/api/lab04/ping"), String.class);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options"))
                .as("Nedostaje X-Content-Type-Options: nosniff")
                .isEqualTo("nosniff");
        assertThat(response.getHeaders().getFirst("X-Frame-Options"))
                .as("Nedostaje X-Frame-Options: DENY")
                .isEqualTo("DENY");
    }
}
