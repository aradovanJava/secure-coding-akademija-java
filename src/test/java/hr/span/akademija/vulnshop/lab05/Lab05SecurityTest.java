package hr.span.akademija.vulnshop.lab05;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab05 – sigurnosni testovi (ranjiva ovisnost / nesigurna interpolacija).
 */
@Tag("security")
@Tag("lab05")
class Lab05SecurityTest extends AbstractLabTest {

    @Test
    @DisplayName("Odgovor ne smije interpretirati ${sys:java.version} nego vratiti doslovni tekst")
    void interpolacijaNijeIzvrsena() {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab05/greeting"),
                Map.of("name", "${sys:java.version}"), Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        String greeting = String.valueOf(response.getBody().get("greeting"));
        assertThat(greeting)
                .as("Interpolator je izvrsio ${sys:...} obrazac (Text4Shell ponasanje)")
                .contains("${sys:java.version}");
    }

    @Test
    @DisplayName("Verzija commons-text na classpathu mora biti >= 1.10.0")
    void commonsTextVerzijaNijeRanjiva() throws Exception {
        String version = readCommonsTextVersion();
        assertThat(version).as("Ne mogu procitati verziju commons-text").isNotNull();
        assertThat(isAtLeast110(version))
                .as("commons-text verzija " + version + " je ranjiva (< 1.10.0)")
                .isTrue();
    }

    private String readCommonsTextVersion() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/META-INF/maven/org.apache.commons/commons-text/pom.properties")) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version");
        }
    }

    private boolean isAtLeast110(String version) {
        String[] parts = version.split("[.\\-]");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return major > 1 || (major == 1 && minor >= 10);
    }
}
