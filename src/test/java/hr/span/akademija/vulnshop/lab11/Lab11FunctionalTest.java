package hr.span.akademija.vulnshop.lab11;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab11 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab11")
class Lab11FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Ispravna prijava korisnika tomislav vraca 200")
    void ispravnaPrijava() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab11/login"),
                Map.of("username", "tomislav", "password", "T0m!slav#2026"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
