package hr.span.akademija.vulnshop.lab11;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab11 – sigurnosni testovi (brute force i enumeracija korisnika).
 */
@Tag("security")
@Tag("lab11")
class Lab11SecurityTest extends AbstractLabTest {

    private ResponseEntity<String> loginAttempt(String username, String password) {
        return rest.postForEntity(url("/api/lab11/login"),
                Map.of("username", username, "password", password), String.class);
    }

    @Test
    @DisplayName("Nepostojeci korisnik i kriva lozinka moraju dati isti status i isto tijelo (bez enumeracije)")
    void nemaEnumeracije() {
        ResponseEntity<String> nepostojeci = loginAttempt("nepostojeci_korisnik_xyz", "bilosto");
        ResponseEntity<String> krivaLozinka = loginAttempt("ivana", "krivaLozinka123");

        assertThat(nepostojeci.getStatusCode().value())
                .as("Razliciti status za nepostojeceg korisnika vs krivu lozinku - enumeracija")
                .isEqualTo(krivaLozinka.getStatusCode().value());
        assertThat(normalize(nepostojeci.getBody()))
                .as("Razlicito tijelo odgovora omogucuje enumeraciju korisnika")
                .isEqualTo(normalize(krivaLozinka.getBody()));
    }

    @Test
    @DisplayName("Nakon 5 neuspjelih pokušaja za korisnika 'lockme' i ispravna lozinka mora vratiti 429")
    void zakljucavanjeNakonPetPokusaja() {
        // 5 neuspjelih pokusaja za zasebnog korisnika lockme.
        for (int i = 0; i < 5; i++) {
            loginAttempt("lockme", "krivo" + i);
        }
        // Sesti pokusaj, cak i s ISPRAVNOM lozinkom, mora biti blokiran (429).
        ResponseEntity<String> response = loginAttempt("lockme", "Lock!Me2026");
        assertThat(response.getStatusCode().value())
                .as("Nema zakljucavanja nakon 5 neuspjelih pokusaja (brute force moguc)")
                .isEqualTo(429);
    }

    private String normalize(String body) {
        return body == null ? "" : body.trim();
    }
}
