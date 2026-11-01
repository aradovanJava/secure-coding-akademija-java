package hr.span.akademija.vulnshop.lab03;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab03 – funkcionalni testovi.
 */
@Tag("functional")
@Tag("lab03")
class Lab03FunctionalTest extends AbstractLabTest {

    @Test
    @DisplayName("Nepodrzana shema ftp:// ne smije vratiti 200")
    void ftpShemaNije200() {
        String token = login("ana", "Ana123!");
        // 127.0.0.1 na FTP portu nema slusatelja -> veza se odbija odmah (brz i stabilan test).
        String encoded = java.net.URLEncoder.encode("ftp://127.0.0.1/datoteka",
                java.nio.charset.StandardCharsets.UTF_8);
        ResponseEntity<String> response = getWithToken("/api/lab03/preview?url=" + encoded, token, String.class);
        // Funkcionalni test smije provjeriti samo da nije 200 (ranjivi i sigurni kod oba vracaju gresku).
        assertThat(response.getStatusCode().value()).isNotEqualTo(200);
    }

    @Test
    @DisplayName("Bez tokena pregled poveznice vraca 401")
    void bezTokena401() {
        String encoded = java.net.URLEncoder.encode("http://example.com",
                java.nio.charset.StandardCharsets.UTF_8);
        ResponseEntity<String> response = getWithToken("/api/lab03/preview?url=" + encoded, null, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
