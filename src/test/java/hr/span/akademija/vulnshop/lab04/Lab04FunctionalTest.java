package hr.span.akademija.vulnshop.lab04;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab04 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab04")
class Lab04FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("/actuator/health je dostupan (200)")
    void healthDostupan() {
        ResponseEntity<String> response = rest.getForEntity(url("/actuator/health"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("/api/lab04/ping vraca 200")
    void pingRadi() {
        ResponseEntity<String> response = rest.getForEntity(url("/api/lab04/ping"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
