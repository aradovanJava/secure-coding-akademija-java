package hr.span.akademija.vulnshop.lab03;

import hr.span.akademija.vulnshop.common.CurrentUser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lab03 – A01 Broken Access Control: Server-Side Request Forgery (CWE-918).
 *
 * POPRAVAK:
 *  - dopustene su samo sheme http i https,
 *  - host se razrjesava i blokira se loopback, privatni rasponi (10/8, 172.16/12,
 *    192.168/16), link-local (169.254/16 – cloud metadata), ::1, fc00::/7 i 0.0.0.0,
 *  - preusmjeravanja su iskljucena, postavljen je timeout,
 *  - u slucaju nedopustenog odredista vraca se 400.
 */
@RestController
@RequestMapping("/api/lab03")
public class Lab03PreviewController {

    private static final int MAX_CHARS = 2000;

    @GetMapping("/preview")
    public ResponseEntity<?> preview(@RequestParam String url, CurrentUser currentUser) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan URL."));
        }

        String scheme = uri.getScheme();
        // POPRAVAK: dopustamo samo http/https (blokira file://, ftp://, gopher:// itd.).
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return ResponseEntity.status(400).body(Map.of("error", "Dopustene su samo http i https sheme."));
        }

        String host = uri.getHost();
        if (host == null) {
            return ResponseEntity.status(400).body(Map.of("error", "Neispravan host."));
        }

        try {
            // POPRAVAK: razrijesi SVE adrese hosta i provjeri da nijedna nije zabranjena
            // (sprjecava DNS rebinding na jednu dopustenu i jednu internu adresu).
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlocked(address)) {
                    return ResponseEntity.status(400)
                            .body(Map.of("error", "Odrediste nije dopusteno (interna/loopback adresa)."));
                }
            }

            URL target = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(false); // POPRAVAK: bez preusmjeravanja
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            // Preusmjeravanje bi moglo voditi na internu adresu -> odbij.
            if (status >= 300 && status < 400) {
                return ResponseEntity.status(400).body(Map.of("error", "Preusmjeravanja nisu dopustena."));
            }

            try (InputStream in = connection.getInputStream()) {
                byte[] data = in.readNBytes(MAX_CHARS);
                String body = new String(data, StandardCharsets.UTF_8);
                return ResponseEntity.ok(Map.of("url", url, "content", body));
            }
        } catch (Exception e) {
            // Ne otkrivamo detalje (DNS, konekcija); genericka greska.
            return ResponseEntity.status(400).body(Map.of("error", "Dohvat nije uspio."));
        }
    }

    /** Vraca true ako adresu NE smijemo dohvatiti (loopback, privatna, link-local, 0.0.0.0, ULA). */
    private boolean isBlocked(InetAddress address) {
        if (address.isLoopbackAddress()       // 127.0.0.0/8, ::1
                || address.isAnyLocalAddress() // 0.0.0.0, ::
                || address.isLinkLocalAddress()  // 169.254/16 (cloud metadata), fe80::/10
                || address.isSiteLocalAddress()) { // 10/8, 172.16/12, 192.168/16
            return true;
        }
        byte[] bytes = address.getAddress();
        // IPv6 Unique Local Address fc00::/7 (fc.. i fd..).
        if (bytes.length == 16) {
            int first = bytes[0] & 0xFF;
            return (first & 0xFE) == 0xFC;
        }
        return false;
    }
}
