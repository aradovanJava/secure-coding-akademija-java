package hr.span.akademija.vulnshop.lab09;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab09 – A05 Injection: OS command injection (CWE-78).
 *
 * RANJIVO: endpoint sastavlja naredbu ljuske ({@code sh -c "ping -c 1 <host>"}
 * ili {@code cmd.exe /c ping -n 1 <host>}) i ubacuje korisnicki unos bez
 * validacije. Napadac moze nadovezati naredbe: {@code 127.0.0.1 & whoami},
 * {@code 127.0.0.1; cat /etc/passwd}.
 */
@RestController
@RequestMapping("/api/lab09")
public class Lab09Controller {

    @GetMapping("/ping")
    public ResponseEntity<?> ping(@RequestParam String host) {
        try {
            boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
            ProcessBuilder pb;
            if (windows) {
                // RANJIVO: host se umece u naredbenu liniju ljuske.
                pb = new ProcessBuilder("cmd.exe", "/c", "ping -n 1 " + host);
            } else {
                pb = new ProcessBuilder("sh", "-c", "ping -c 1 " + host);
            }
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
