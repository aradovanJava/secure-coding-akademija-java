package hr.span.akademija.vulnshop.lab14;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Lab14 – sigurnosni testovi (logging & alerting failures).
 *
 * Koristi {@link OutputCaptureExtension} za hvatanje izlaza loga.
 */
@Tag("security")
@Tag("lab14")
@ExtendWith(OutputCaptureExtension.class)
class Lab14SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Lozinka se NE smije pojaviti u logu")
    void lozinkaNijeULogu(CapturedOutput output) {
        String tajnaLozinka = "TajnaLozinka_" + System.nanoTime();
        rest.postForEntity(url("/api/lab14/login"),
                Map.of("username", "ana", "password", tajnaLozinka), Map.class);
        assertThat(output.getAll())
                .as("Lozinka je zapisana u log (CWE-532)")
                .doesNotContain(tajnaLozinka);
    }

    @Test
    @DisplayName("Korisnicko ime s \\n ne smije proizvesti krivotvoreni redak u logu (log injection)")
    void nemaLogInjectiona(CapturedOutput output) {
        // Payload pokusava ubaciti lazni redak "LOGIN_SUCCESS user=admin".
        String payload = "zlonamjerni\nLOGIN_SUCCESS_FORGED user=admin";
        rest.postForEntity(url("/api/lab14/login"),
                Map.of("username", payload, "password", "x"), Map.class);
        // Niti jedan redak loga ne smije POCETI krivotvorenim tekstom.
        boolean krivotvoreniRedak = output.getAll().lines()
                .anyMatch(line -> line.startsWith("LOGIN_SUCCESS_FORGED"));
        assertThat(krivotvoreniRedak)
                .as("CR/LF u korisnickom imenu stvorio je krivotvoreni redak (CWE-117)")
                .isFalse();
    }

    @Test
    @DisplayName("Neuspjela prijava mora proizvesti SECURITY_EVENT ... LOGIN_FAILED zapis")
    void neuspjehProizvodiSigurnosniDogadaj(CapturedOutput output) {
        rest.postForEntity(url("/api/lab14/login"),
                Map.of("username", "marko", "password", "pogresno"), Map.class);
        String log = output.getAll();
        assertThat(log)
                .as("Neuspjela prijava ne biljezi sigurnosni dogadaj (CWE-778)")
                .contains("SECURITY_EVENT")
                .contains("LOGIN_FAILED");
    }

    @Test
    @DisplayName("Nakon 5 neuspjeha za istog korisnika mora postojati SECURITY_ALERT (brute force)")
    void petNeuspjehaDajeAlert(CapturedOutput output) {
        String korisnik = "bruteforce_meta_" + (System.nanoTime() % 100000);
        for (int i = 0; i < 6; i++) {
            rest.postForEntity(url("/api/lab14/login"),
                    Map.of("username", korisnik, "password", "pogresno" + i), Map.class);
        }
        assertThat(output.getAll())
                .as("Nema SECURITY_ALERT upozorenja nakon 5 neuspjeha")
                .contains("SECURITY_ALERT");
    }
}
