package hr.span.akademija.vulnshop.lab02;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab02 – sigurnosni testovi (mass assignment / eskalacija privilegija).
 */
@Tag("security")
@Tag("lab02")
class Lab02SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Marko ne smije sam sebi postaviti ulogu ADMIN kroz PUT /profile")
    void markoNeSmijePostatiAdmin() {
        String token = login("marko", "Marko123!");
        // Napad: salje role=ADMIN u tijelu zahtjeva.
        putWithToken("/api/lab02/profile", token,
                Map.of("displayName", "Marko", "email", "m@x.hr", "role", "ADMIN"), Map.class);

        // Profil nakon PUT-a i dalje mora imati ulogu USER.
        ResponseEntity<Map> profile = getWithToken("/api/lab02/profile", token, Map.class);
        assertThat(profile.getBody())
                .as("Marko je uspio promijeniti vlastitu ulogu (mass assignment)")
                .containsEntry("role", "USER");
    }

    @Test
    @DisplayName("Marko ne smije pristupiti admin/users ni nakon pokusaja eskalacije (403)")
    void markoNemaPristupAdminUsers() {
        String token = login("marko", "Marko123!");
        putWithToken("/api/lab02/profile", token,
                Map.of("displayName", "Marko", "email", "m@x.hr", "role", "ADMIN"), Map.class);

        // Citamo kao String da izbjegnemo deserijalizaciju (na main se vraca JSON lista, na rjesenja 403 objekt).
        ResponseEntity<String> response = getWithToken("/api/lab02/admin/users", token, String.class);
        assertThat(response.getStatusCode().value())
                .as("Marko je dobio pristup admin popisu korisnika")
                .isEqualTo(403);
    }
}
