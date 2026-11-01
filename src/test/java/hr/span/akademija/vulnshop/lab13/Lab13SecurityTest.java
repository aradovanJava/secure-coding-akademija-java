package hr.span.akademija.vulnshop.lab13;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import hr.span.akademija.vulnshop.lab13.tools.Lab13PayloadGenerator;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab13 – sigurnosni testovi (nesigurna deserijalizacija).
 *
 * Napad: salje se serijalizirani {@link DiagnosticsTask} koji bi pri
 * deserijalizaciji zapisao marker datoteku. Siguran kod mora odbiti payload (4xx)
 * i datoteka NE smije nastati.
 */
@Tag("security")
@Tag("lab13")
class Lab13SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Napadacki payload (DiagnosticsTask) mora vratiti 400 i NE smije stvoriti marker datoteku")
    void napadackiPayloadOdbijen() throws Exception {
        Path marker = Files.createTempFile("lab13_test_", ".txt");
        Files.deleteIfExists(marker); // zelimo da je NEMA prije napada

        String payload = Lab13PayloadGenerator.attackPayload(marker.toString());
        ResponseEntity<String> response = rest.postForEntity(url("/api/lab13/cart/import"), payload, String.class);

        boolean datotekaNastala = Files.exists(marker);
        Files.deleteIfExists(marker); // pospremi

        assertThat(datotekaNastala)
                .as("Deserijalizacija je izvrsila gadget i zapisala datoteku - nesigurna deserijalizacija")
                .isFalse();
        assertThat(response.getStatusCode().value())
                .as("Napadacki payload nije odbijen sa 400")
                .isEqualTo(400);
    }
}
