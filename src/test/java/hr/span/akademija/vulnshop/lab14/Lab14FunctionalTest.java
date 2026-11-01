package hr.span.akademija.vulnshop.lab14;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab14 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab14")
class Lab14FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Ispravna prijava vraća 200")
    void ispravnaPrijava() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab14/login"),
                Map.of("username", "ana", "password", "Ana123!"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
