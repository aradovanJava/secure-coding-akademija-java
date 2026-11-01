package hr.span.akademija.vulnshop.lab08;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab08 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab08")
class Lab08FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Obicna pretraga prikazuje uneseni pojam")
    void obicnaPretraga() {
        ResponseEntity<String> response = rest.getForEntity(uri("/lab08/search", "q", "Laptop"), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Laptop");
    }

    @Test
    @DisplayName("Obicna recenzija se sprema i prikazuje")
    void obicnaRecenzija() {
        long productId = 700 + (System.nanoTime() % 1000);
        ResponseEntity<Map> add = rest.postForEntity(url("/api/lab08/reviews"),
                Map.of("productId", productId, "author", "Ana", "text", "Odlican proizvod"), Map.class);
        assertThat(add.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<String> response = rest.getForEntity(
                url("/lab08/products/" + productId + "/reviews"), String.class);
        assertThat(response.getBody()).contains("Odlican proizvod").contains("Ana");
    }
}
