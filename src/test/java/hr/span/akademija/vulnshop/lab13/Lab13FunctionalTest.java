package hr.span.akademija.vulnshop.lab13;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import hr.span.akademija.vulnshop.lab13.tools.Lab13PayloadGenerator;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab13 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab13")
class Lab13FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Legitimna košarica s 2 stavke vraća 200 i itemCount=2")
    void legitimnaKosarica() {
        String payload = Lab13PayloadGenerator.legitimateCartPayload();
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab13/cart/import"), payload, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(String.valueOf(response.getBody().get("itemCount"))).isEqualTo("2");
    }
}
