package hr.span.akademija.vulnshop.lab01;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab01 – funkcionalni testovi (uvijek prolaze; dokazuju da popravak ne lomi legitimno ponasanje).
 */
@Tag("functional")
@Tag("lab01")
class Lab01FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Ana smije dohvatiti vlastitu narudzbu 1001 (200)")
    void anaDohvacaVlastituNarudzbu() {
        String token = login("ana", "Ana123!");
        ResponseEntity<Map> response = getWithToken("/api/lab01/orders/1001", token, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("owner", "ana");
    }

    @Test
    @DisplayName("Administrator smije dohvatiti bilo koju narudzbu 1003 (200)")
    void adminDohvacaBiloKojuNarudzbu() {
        String token = login("admin", "Admin123!");
        ResponseEntity<Map> response = getWithToken("/api/lab01/orders/1003", token, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Bez tokena dohvat narudzbe vraća 401")
    void bezTokena401() {
        ResponseEntity<Map> response = getWithToken("/api/lab01/orders/1001", null, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
