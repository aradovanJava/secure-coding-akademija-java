package hr.span.akademija.vulnshop.lab03;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab03 – sigurnosni testovi (SSRF).
 *
 * Napad cilja interni endpoint na samom posluzitelju (loopback), cloud metadata
 * adresu i (Java) file:// shemu. Siguran kod mora vratiti 4xx i NIKAD procuriti tajnu.
 */
@Tag("security")
@Tag("lab03")
class Lab03SecurityTest extends AbstractLabTest {

    private static final String SECRET = "Sup3rTajn0!";

    private ResponseEntity<String> preview(String token, String targetUrl) {
        return getWithToken(uri("/api/lab03/preview", "url", targetUrl), token, String.class);
    }

    @Test
    @DisplayName("SSRF na 127.0.0.1 interni endpoint mora biti blokiran (400) i bez tajne")
    void ssrfLoopbackBlokiran() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = preview(token,
                "http://127.0.0.1:" + port + "/internal/admin/secrets");
        assertThat(response.getStatusCode().value())
                .as("SSRF na loopback nije blokiran")
                .isEqualTo(400);
        assertThat(response.getBody() == null ? "" : response.getBody())
                .as("Tajna je procurila kroz SSRF")
                .doesNotContain(SECRET);
    }

    @Test
    @DisplayName("SSRF preko imena localhost mora biti blokiran (400) i bez tajne")
    void ssrfLocalhostBlokiran() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = preview(token,
                "http://localhost:" + port + "/internal/admin/secrets");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody() == null ? "" : response.getBody()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("SSRF na [::1] (IPv6 loopback) mora biti blokiran (400)")
    void ssrfIpv6LoopbackBlokiran() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = preview(token,
                "http://[::1]:" + port + "/internal/admin/secrets");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody() == null ? "" : response.getBody()).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("SSRF na cloud metadata 169.254.169.254 mora biti blokiran (400)")
    void ssrfMetadataBlokiran() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = preview(token, "http://169.254.169.254/latest/meta-data/");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("SSRF preko file:// sheme mora biti blokiran (400)")
    void ssrfFileShemaBlokirana() {
        String token = login("ana", "Ana123!");
        ResponseEntity<String> response = preview(token, "file:///etc/passwd");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody() == null ? "" : response.getBody()).doesNotContain("root:");
    }
}
