package hr.span.akademija.vulnshop.lab02;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab02 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab02")
class Lab02FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Ana smije promijeniti svoje ime (displayName) kroz PUT /profile")
    void anaMijenjaDisplayName() {
        String token = login("ana", "Ana123!");
        ResponseEntity<Map> response = putWithToken("/api/lab02/profile", token,
                Map.of("displayName", "Ana Novo", "email", "ana@akademija.hr"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("displayName", "Ana Novo");
    }

    @Test
    @DisplayName("Administrator smije vidjeti popis korisnika (admin/users -> 200)")
    void adminVidiPopisKorisnika() {
        String token = login("admin", "Admin123!");
        ResponseEntity<Object[]> response = getWithToken("/api/lab02/admin/users", token, Object[].class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotEmpty();
    }
}
