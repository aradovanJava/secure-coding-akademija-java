package hr.span.akademija.vulnshop.lab12;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Lab12 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab12")
class Lab12FunctionalTest extends AbstractLabTest {

    private String lab12Login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab12/login"),
                Map.of("username", username, "password", password), Map.class);
        return String.valueOf(response.getBody().get("token"));
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    @DisplayName("Prijava pa /me vraća 200 s valjanim tokenom")
    void loginPaMe() {
        String token = lab12Login("ana", "Ana123!");
        ResponseEntity<String> response = get("/api/lab12/me", token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("USER na /admin dobiva 403")
    void userNaAdmin403() {
        String token = lab12Login("ana", "Ana123!");
        ResponseEntity<String> response = get("/api/lab12/admin", token);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @DisplayName("ADMIN prijava pa /admin vraća 200")
    void adminNaAdmin200() {
        String token = lab12Login("admin", "Admin123!");
        ResponseEntity<String> response = get("/api/lab12/admin", token);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
