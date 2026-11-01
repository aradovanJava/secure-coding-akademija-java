package hr.span.akademija.vulnshop.lab07;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab07 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab07")
class Lab07FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Pretraga q=Laptop vraca tocno jedan rezultat")
    void pretragaLaptop() {
        ResponseEntity<List> response = rest.getForEntity(
                uri("/api/lab07/products/search", "q", "Laptop"), List.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Ispravna prijava racuna admin/SuperTajna2026 vraca 200")
    void ispravnaPrijava() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab07/login"),
                Map.of("username", "admin", "password", "SuperTajna2026"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
