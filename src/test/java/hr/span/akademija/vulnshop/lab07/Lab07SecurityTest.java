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
 * Lab07 – sigurnosni testovi (SQL injection).
 */
@Tag("security")
@Tag("lab07")
class Lab07SecurityTest extends AbstractLabTest {

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> search(String q) {
        // uri(...) enkodira TOCNO jednom; TestRestTemplate ne dira gotov URI.
        ResponseEntity<List> response = rest.getForEntity(
                uri("/api/lab07/products/search", "q", q), List.class);
        return response.getBody();
    }

    @Test
    @DisplayName("Payload ' OR '1'='1' -- ne smije otkriti skriveni proizvod (Interni prototip X)")
    void orInjectionNeOtkrivaSkriveno() {
        List<Map<String, Object>> rows = search("' OR '1'='1' --");
        boolean sadrziSkriveni = rows != null && rows.stream()
                .anyMatch(r -> String.valueOf(r.get("name")).contains("prototip")
                        || String.valueOf(r.get("NAME")).contains("prototip"));
        assertThat(sadrziSkriveni)
                .as("SQL injection je otkrio skriveni proizvod")
                .isFalse();
    }

    @Test
    @DisplayName("UNION payload ne smije vratiti korisnička imena/lozinke iz lab07_accounts")
    void unionNeOtkrivaRacune() {
        List<Map<String, Object>> rows = search("%' UNION SELECT id, username, password, 0 FROM lab07_accounts --");
        boolean sadrziLozinku = rows != null && rows.stream()
                .anyMatch(r -> String.valueOf(r.values()).contains("SuperTajna2026")
                        || String.valueOf(r.values()).contains("Shop!2026"));
        assertThat(sadrziLozinku)
                .as("UNION injekcija je izvukla lozinke racuna")
                .isFalse();
    }

    @Test
    @DisplayName("Prijava s admin'-- i bilo kojom lozinkom mora biti odbijena (401)")
    void loginBypassOdbijen() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab07/login"),
                Map.of("username", "admin'--", "password", "bilosto"), Map.class);
        assertThat(response.getStatusCode().value())
                .as("SQL injection je zaobisao prijavu")
                .isEqualTo(401);
    }
}
