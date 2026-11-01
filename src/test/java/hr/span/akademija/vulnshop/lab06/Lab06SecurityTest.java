package hr.span.akademija.vulnshop.lab06;

import static org.assertj.core.api.Assertions.assertThat;

import hr.span.akademija.vulnshop.common.AbstractLabTest;
import hr.span.akademija.vulnshop.common.SeedData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Lab06 – sigurnosni testovi (kriptografski propusti).
 *
 * Dio A: hashevi lozinki moraju biti soljeni i sporo izracunljivi (ne MD5 bez soli).
 * Dio B: sifriranje mora biti nedeterministicko (ne ECB) i ne smije se moci
 * desifrirati hardkodiranim kljucem u ECB modu.
 */
@Tag("security")
@Tag("lab06")
class Lab06SecurityTest extends AbstractLabTest {

    // ----------------------------- Dio A -----------------------------

    @Test
    @DisplayName("Dva korisnika s istom lozinkom moraju imati različite hasheve (sol)")
    void istaLozinkaRazlicitiHashevi() {
        // Registriraj dva nova korisnika s istom lozinkom.
        rest.postForEntity(url("/api/lab06/register"),
                Map.of("username", "test_a_" + System.nanoTime(), "password", "ZajednickaLozinka1!"), Map.class);
        rest.postForEntity(url("/api/lab06/register"),
                Map.of("username", "test_b_" + System.nanoTime(), "password", "ZajednickaLozinka1!"), Map.class);

        ResponseEntity<List> export = rest.getForEntity(url("/api/lab06/customers/export"), List.class);
        List<Map<String, Object>> rows = export.getBody();
        // ivan i maja (seed) imaju istu lozinku 123456 -> u ranjivoj verziji isti hash.
        String ivanHash = hashOf(rows, "ivan");
        String majaHash = hashOf(rows, "maja");
        assertThat(ivanHash).isNotNull();
        assertThat(majaHash).isNotNull();
        assertThat(ivanHash)
                .as("ivan i maja imaju identican hash - nema soli (MD5)")
                .isNotEqualTo(majaHash);
    }

    @Test
    @DisplayName("Hash lozinke ne smije biti MD5 heks (32 heks znaka) niti MD5 te lozinke")
    void hashNijeMd5() {
        ResponseEntity<List> export = rest.getForEntity(url("/api/lab06/customers/export"), List.class);
        List<Map<String, Object>> rows = export.getBody();
        String ivanHash = hashOf(rows, "ivan");
        assertThat(ivanHash).isNotNull();
        assertThat(ivanHash.matches("[0-9a-fA-F]{32}"))
                .as("Hash izgleda kao MD5 (32 heks znaka)")
                .isFalse();
        assertThat(ivanHash)
                .as("Hash je tocno MD5 lozinke 123456")
                .isNotEqualTo(SeedData.md5Hex("123456"));
    }

    private String hashOf(List<Map<String, Object>> rows, String username) {
        if (rows == null) {
            return null;
        }
        return rows.stream()
                .filter(r -> username.equals(r.get("username")) || username.equals(r.get("USERNAME")))
                .map(r -> {
                    Object h = r.get("password_hash");
                    return h == null ? r.get("PASSWORD_HASH") : h;
                })
                .map(String::valueOf)
                .findFirst().orElse(null);
    }

    // ----------------------------- Dio B -----------------------------

    @Test
    @DisplayName("Dvaput isti broj kartice mora dati različite ciphertexte (ne ECB)")
    void istaKarticaRazlicitiCiphertext() {
        String c1 = storeCard("4111111111111111");
        String c2 = storeCard("4111111111111111");
        assertThat(c1).isNotNull();
        assertThat(c2).isNotNull();
        assertThat(c1)
                .as("Isti broj kartice daje isti ciphertext - ECB nacin")
                .isNotEqualTo(c2);
    }

    @Test
    @DisplayName("Ciphertext kartice mora biti dulji od 16+12 B (IV + tag, dakle ne goli ECB blok)")
    void ciphertextSadrziIvITag() {
        String c1 = storeCard("4111111111111111");
        byte[] raw = java.util.Base64.getDecoder().decode(c1);
        assertThat(raw.length)
                .as("Ciphertext je prekratak za IV (12B) + podatke + GCM tag (16B)")
                .isGreaterThan(16 + 12);
    }

    @Test
    @DisplayName("Dešifriranje hardkodiranim ključem u ECB modu više ne daje originalni broj")
    void ecbKljucViseNeRadi() {
        String number = "4111111111111111";
        String ciphertext = storeCard(number);
        String decrypted = tryDecryptEcb(ciphertext);
        assertThat(decrypted)
                .as("Ciphertext se moze desifrirati hardkodiranim ECB kljucem")
                .isNotEqualTo(number);
    }

    private String storeCard(String number) {
        ResponseEntity<Map> response = rest.postForEntity(url("/api/lab06/cards"),
                Map.of("cardNumber", number), Map.class);
        if (response.getBody() == null) {
            return null;
        }
        return String.valueOf(response.getBody().get("ciphertext"));
    }

    /** Pokusava desifrirati Base64 ciphertext starim ECB kljucem; vraca null ako ne uspije. */
    private String tryDecryptEcb(String base64) {
        try {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                    new javax.crypto.spec.SecretKeySpec(
                            "AkademijaKljuc16".getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES"));
            byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(base64));
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
