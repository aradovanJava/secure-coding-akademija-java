package hr.span.akademija.vulnshop.lab09;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab09 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab09")
class Lab09FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Ispravan host 127.0.0.1 vraca 200 (neovisno o uspjehu pinga)")
    void ispravanHost200() {
        ResponseEntity<String> response = rest.getForEntity(uri("/api/lab09/ping", "host", "127.0.0.1"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
