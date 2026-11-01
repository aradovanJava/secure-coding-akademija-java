package hr.span.akademija.vulnshop.lab01;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab01 – sigurnosni testovi (IDOR). Na ranjivom kodu PADAJU, nakon popravka PROLAZE.
 */
@Tag("security")
@Tag("lab01")
class Lab01SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Ana ne smije dohvatiti Markovu narudzbu 1003 (mora vratiti 404)")
    void anaNeSmijeDohvatitiTuduNarudzbu() {
        String token = login("ana", "Ana123!");
        ResponseEntity<Map> response = getWithToken("/api/lab01/orders/1003", token, Map.class);
        // Sigurno ponasanje: tuda narudzba ne smije biti dostupna; vraca se 404 (ne otkrivamo postojanje).
        assertThat(response.getStatusCode().value())
                .as("Ana je uspjela procitati tudu narudzbu - IDOR ranjivost")
                .isEqualTo(404);
    }
}
