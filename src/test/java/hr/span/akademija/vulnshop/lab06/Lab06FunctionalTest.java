package hr.span.akademija.vulnshop.lab06;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab06 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab06")
class Lab06FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Registracija i prijava tocnom lozinkom rade (200), kriva lozinka 401")
    void registracijaIPrijava() {
        String username = "func_" + System.nanoTime();
        ResponseEntity<Map> reg = rest.postForEntity(url("/api/lab06/register"),
                Map.of("username", username, "password", "Lozinka123!"), Map.class);
        assertThat(reg.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map> ok = rest.postForEntity(url("/api/lab06/login"),
                Map.of("username", username, "password", "Lozinka123!"), Map.class);
        assertThat(ok.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<Map> bad = rest.postForEntity(url("/api/lab06/login"),
                Map.of("username", username, "password", "KrivaLozinka"), Map.class);
        assertThat(bad.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("Maskirani prikaz kartice vraca zadnje 4 znamenke")
    void maskiranaKarticaRadi() {
        ResponseEntity<Map> store = rest.postForEntity(url("/api/lab06/cards"),
                Map.of("cardNumber", "4242424242424242"), Map.class);
        assertThat(store.getStatusCode().value()).isEqualTo(200);
        Object id = store.getBody().get("id");

        ResponseEntity<Map> masked = rest.getForEntity(url("/api/lab06/cards/" + id + "/masked"), Map.class);
        assertThat(masked.getStatusCode().value()).isEqualTo(200);
        assertThat(String.valueOf(masked.getBody().get("masked"))).endsWith("4242");
    }
}
