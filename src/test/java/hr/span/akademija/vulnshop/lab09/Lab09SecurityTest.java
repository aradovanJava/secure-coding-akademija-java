package hr.span.akademija.vulnshop.lab09;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab09 – sigurnosni testovi (OS command injection).
 */
@Tag("security")
@Tag("lab09")
class Lab09SecurityTest extends AbstractLabTest {

    private ResponseEntity<String> ping(String host) {
        String encoded = java.net.URLEncoder.encode(host, java.nio.charset.StandardCharsets.UTF_8);
        return rest.getForEntity(url("/api/lab09/ping?host=" + encoded), String.class);
    }

    @Test
    @DisplayName("Payload '127.0.0.1 & echo PWNED_LAB09' mora vratiti 400 bez izvrsenja naredbe")
    void nadovezivanjeAmpersandBlokirano() {
        ResponseEntity<String> response = ping("127.0.0.1 & echo PWNED_LAB09");
        String body = response.getBody() == null ? "" : response.getBody();
        assertThat(response.getStatusCode().value())
                .as("Nije odbijen host s metaznakovima ljuske")
                .isEqualTo(400);
        assertThat(body)
                .as("Naredba echo je izvrsena - command injection")
                .doesNotContain("PWNED_LAB09");
    }

    @Test
    @DisplayName("Payload '127.0.0.1;id' mora vratiti 400")
    void nadovezivanjeTockaZarezBlokirano() {
        ResponseEntity<String> response = ping("127.0.0.1;id");
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
