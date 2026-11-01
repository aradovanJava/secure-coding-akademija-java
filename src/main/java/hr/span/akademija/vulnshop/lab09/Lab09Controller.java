package hr.span.akademija.vulnshop.lab09;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab09 – A05 Injection: OS command injection (CWE-78) (POPRAVLJENO).
 *
 * POPRAVAK:
 *  - stroga validacija ulaza (samo IP/hostname, regex {@code ^[A-Za-z0-9.-]{1,253}$}),
 *  - NE koristi se ljuska: naredba se pokrece preko {@link ProcessBuilder} s listom
 *    argumenata, pa metaznakovi ({@code &}, {@code ;}, {@code |}) nemaju posebno znacenje.
 */
@RestController
@RequestMapping("/api/lab09")
public class Lab09Controller {

    // Dopusteni su samo znakovi dozvoljeni u hostname/IP zapisu.
    private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]{1,253}$");

    @GetMapping("/ping")
    public ResponseEntity<?> ping(@RequestParam String host) {
        // POPRAVAK: ulaz koji ne odgovara uzorku odbijamo prije ikakvog izvrsavanja.
        if (host == null || !HOST_PATTERN.matcher(host).matches()) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan host."));
        }
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            List<String> command = new ArrayList<>();
            if (windows) {
                command.add("ping");
                command.add("-n");
                command.add("1");
                command.add(host);
            } else {
                command.add("ping");
                command.add("-c");
                command.add("1");
                command.add(host);
            }
            // POPRAVAK: ProcessBuilder s listom argumenata (bez sh -c / cmd /c), host je jedan argument.
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor(10, TimeUnit.SECONDS);
            return ResponseEntity.ok(Map.of("host", host, "output", output.toString()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Izvrsavanje nije uspjelo."));
        }
    }
}
