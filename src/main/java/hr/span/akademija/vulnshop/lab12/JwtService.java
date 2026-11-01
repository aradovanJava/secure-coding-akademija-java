package hr.span.akademija.vulnshop.lab12;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Lab12 – rucna implementacija JWT-a (HS256) BEZ vanjskih biblioteka.
 *
 * RANJIVI verifikator ({@link #verifyVulnerable(String)}):
 *  - cita {@code alg} iz zaglavlja i ako je {@code none} preskace provjeru potpisa,
 *  - ne provjerava {@code exp} (istekli token prolazi),
 *  - usporeduje potpis obicnim {@code equals} (nije konstantno vrijeme).
 *
 * Tajna je hardkodirana/slaba ({@code secret123}) i cita se iz konfiguracije.
 */
@Service
public class JwtService {

    private final String secret;

    public JwtService(@Value("${app.lab12.jwt-secret}") String secret) {
        this.secret = secret;
    }

    /** Izraduje HS256 token s proizvoljnim vremenom isteka (korisno za testove). */
    public String createToken(String username, String role, Instant expiresAt) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        long now = Instant.now().getEpochSecond();
        String payloadJson = "{\"sub\":\"" + username + "\",\"role\":\"" + role
                + "\",\"iat\":" + now + ",\"exp\":" + expiresAt.getEpochSecond() + "}";
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        String signature = base64Url(hmacSha256(signingInput, secret));
        return signingInput + "." + signature;
    }

    /** Rezultat verifikacije: subject (sub) i uloga (role) ili {@code null} ako token nije valjan. */
    public record VerifiedToken(String subject, String role) {
    }

    /**
     * RANJIVA verifikacija. NE koristi se na grani rjesenja.
     */
    public VerifiedToken verifyVulnerable(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String headerJson = new String(base64UrlDecode(parts[0]), StandardCharsets.UTF_8);
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            String alg = extractString(headerJson, "alg");

            // RANJIVO: alg:none preskace provjeru potpisa.
            if (!"none".equalsIgnoreCase(alg)) {
                if (parts.length != 3) {
                    return null;
                }
                String expected = base64Url(hmacSha256(parts[0] + "." + parts[1], secret));
                // RANJIVO: obicna usporedba stringova (nije konstantno vrijeme).
                if (!expected.equals(parts[2])) {
                    return null;
                }
            }
            // RANJIVO: exp se ne provjerava.
            String sub = extractString(payloadJson, "sub");
            String role = extractString(payloadJson, "role");
            return new VerifiedToken(sub, role);
        } catch (Exception e) {
            return null;
        }
    }

    // ----------------------- pomocne metode -----------------------

    public static byte[] hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC greska", e);
        }
    }

    public static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static byte[] base64UrlDecode(String s) {
        return Base64.getUrlDecoder().decode(s);
    }

    /** Vrlo jednostavno izvlacenje string vrijednosti iz plosnog JSON-a (dovoljno za JWT claimove). */
    static String extractString(String json, String key) {
        String needle = "\"" + key + "\"";
        int idx = json.indexOf(needle);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + needle.length());
        if (colon < 0) {
            return null;
        }
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        if (i >= json.length() || json.charAt(i) != '"') {
            return null;
        }
        int start = i + 1;
        int end = json.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return json.substring(start, end);
    }
}
