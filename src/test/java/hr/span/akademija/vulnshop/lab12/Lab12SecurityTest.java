package hr.span.akademija.vulnshop.lab12;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Lab12 – sigurnosni testovi (JWT propusti).
 */
@Tag("security")
@Tag("lab12")
class Lab12SecurityTest extends AbstractLabTest {

    @Autowired
    private JwtService jwtService;

    private ResponseEntity<String> callAdmin(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(url("/api/lab12/admin"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    @Test
    @DisplayName("Token s alg:none i role:ADMIN mora biti odbijen (401)")
    void algNoneOdbijen() {
        String header = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"napadac\",\"role\":\"ADMIN\",\"iat\":0,\"exp\":9999999999}");
        // alg:none token - prazan potpis.
        String token = header + "." + payload + ".";
        ResponseEntity<String> response = callAdmin(token);
        assertThat(response.getStatusCode().value())
                .as("alg:none token je prihvacen - zaobidena provjera potpisa")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Token potpisan slabom tajnom 'secret123' s role:ADMIN mora biti odbijen (401)")
    void slabaTajnaOdbijena() {
        // Napadac je razbio slabu tajnu i sam kreira ADMIN token njome.
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        long now = Instant.now().getEpochSecond();
        String payload = base64Url("{\"sub\":\"napadac\",\"role\":\"ADMIN\",\"iat\":" + now
                + ",\"exp\":" + (now + 3600) + "}");
        String signingInput = header + "." + payload;
        String signature = JwtService.base64Url(JwtService.hmacSha256(signingInput, "secret123"));
        String token = signingInput + "." + signature;

        ResponseEntity<String> response = callAdmin(token);
        assertThat(response.getStatusCode().value())
                .as("Token potpisan hardkodiranom tajnom secret123 je prihvacen")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Istekli token (ispravno potpisan) mora biti odbijen (401)")
    void istekliTokenOdbijen() {
        // Napravi token koji je istekao prije sat vremena, ispravnim kljucem servisa.
        String token = jwtService.createToken("ana", "USER", Instant.now().minus(1, ChronoUnit.HOURS));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = rest.exchange(url("/api/lab12/me"), HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value())
                .as("Istekli token je prihvacen - ne provjerava se exp")
                .isEqualTo(401);
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
