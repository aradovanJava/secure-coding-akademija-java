package hr.span.akademija.vulnshop.lab05;

import java.util.Map;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab05 – A03 Software Supply Chain Failures (CWE-1104, CWE-1395).
 *
 * RANJIVO: koristi se commons-text 1.9 (ranjiva verzija, CVE-2022-42889
 * "Text4Shell") i korisnicki unos se interpolira preko
 * {@link StringSubstitutor#createInterpolator()}. Napadac moze ubaciti obrasce
 * poput {@code ${sys:user.home}}, {@code ${env:PATH}}, {@code ${file:UTF-8:pom.xml}}
 * ili {@code ${url:UTF-8:http://127.0.0.1:8080/internal/admin/secrets}}.
 * Na starim JDK-ovima (<=14, Nashorn) {@code ${script:javascript:...}} znacio je RCE.
 */
@RestController
@RequestMapping("/api/lab05")
public class Lab05GreetingController {

    public record GreetingRequest(String name) {
    }

    @PostMapping("/greeting")
    public ResponseEntity<?> greeting(@RequestBody GreetingRequest request) {
        String name = request.name() == null ? "" : request.name();
        // RANJIVO: interpolator obraduje ${...} obrasce iz korisnickog unosa.
        StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
        String greeting = interpolator.replace("Pozdrav, " + name + "!");
        return ResponseEntity.ok(Map.of("greeting", greeting));
    }
}
