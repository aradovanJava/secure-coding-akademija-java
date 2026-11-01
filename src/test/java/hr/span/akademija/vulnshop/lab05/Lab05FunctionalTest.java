package hr.span.akademija.vulnshop.lab05;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab05 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab05")
class Lab05FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Običan pozdrav vraća očekivani tekst s imenom")
    void obicanPozdravRadi() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab05/greeting"),
                Map.of("name", "Ana"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String greeting = String.valueOf(response.getBody().get("greeting"));
        assertThat(greeting).contains("Ana");
    }
}
