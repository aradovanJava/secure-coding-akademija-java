# Lab06 – Cryptographic Failures

## 1. Cilj vježbe

Pokazati dvije česte kriptografske pogreške: (A) slabo hashiranje lozinki (MD5 bez soli) i (B) slabo
šifriranje podataka (AES/ECB s hardkodiranim ključem). Polaznik prelazi na BCrypt i AES-GCM s ključem iz
konfiguracije.

- **OWASP Top 10:2025:** A04 – Cryptographic Failures
- **CWE:** CWE-916 (Password Hash With Insufficient Effort), CWE-327 (Broken/Risky Crypto Algorithm), CWE-321 (Hard-coded Key), CWE-329 (Missing/Static IV)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab06/Lab06Controller.java`
  - Dio A: `POST /api/lab06/register`, `/login`, `GET /api/lab06/customers/export`
  - Dio B: `POST /api/lab06/cards`, `GET /api/lab06/cards`, `GET /api/lab06/cards/{id}/masked`
- Seed kupci (slabe lozinke): `common/SeedData.java` (`seedLab06`): ivan/123456, petra/password,
  luka/qwerty, maja/123456 (**isti hash kao ivan!**).
- Test: `lab06/Lab06SecurityTest.java`. Wordlista: `wordlists/top-100.txt`.

## 3. Opis ranjivosti

- **Dio A:** `register`/`login` koriste `MD5(hex, bez soli)`. MD5 je brz i bez soli, pa je offline
  razbijanje trivijalno; iste lozinke daju **isti** hash (ivan i maja). `customers/export` javno vraća
  dump (simulira procurjelu bazu).
- **Dio B:** brojevi kartica šifriraju se `AES/ECB/PKCS5Padding` s ključem `"AkademijaKljuc16"`
  hardkodiranim u kodu. ECB je deterministički (isti ulaz → isti ciphertext), a ključ iz koda znači da
  tko god vidi izvor može dešifrirati sve.

## 4. Eksploatacija – korak po korak

1. **Dump lozinki:** `GET /api/lab06/customers/export`. Hasheve ubaci u CrackStation ili:
   ```bash
   hashcat -m 0 hashes.txt wordlists/top-100.txt
   ```
   **Očekivano (ranjivo):** svi su 32-znamenkasti heksevi; ivan i maja imaju identičan hash → ista lozinka.
2. **ECB determinizam:** dvaput pošalji isti broj kartice:
   ```bash
   curl.exe -s -X POST http://localhost:8080/api/lab06/cards -H "Content-Type: application/json" -d "{\"cardNumber\":\"4111111111111111\"}"
   ```
   **Očekivano (ranjivo):** oba poziva vraćaju **isti** `ciphertext`. S ključem iz koda može se dešifrirati
   (CyberChef „AES Decrypt", ECB, ključ `AkademijaKljuc16` kao UTF-8 / hex).

## 5. Analiza uzroka

```java
String hash = SeedData.md5Hex(body.password());             // A: MD5 bez soli
Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");  // B: ECB
cipher.init(ENCRYPT_MODE, new SecretKeySpec(HARDCODED_KEY, "AES")); // hardkodirani kljuc
```

## 6. Popravak

- **Dio A:** `BCryptPasswordEncoder(12)` (soljeno, sporo); seed se hashira novom metodom.
- **Dio B:** `AES/GCM/NoPadding` s nasumičnim 12-bajtnim IV-om po zapisu, pohrani `IV || ciphertext+tag`;
  ključ iz konfiguracije/okoline (`app.lab06.encryption-key`), napomena o KMS/Key Vaultu.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
// Dio A:
private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
String hash = encoder.encode(body.password());
// login:
if (!encoder.matches(body.password(), stored)) { /* 401 */ }

// Dio B (kljuc iz konfiguracije):
public Lab06Controller(JdbcTemplate jdbc, @Value("${app.lab06.encryption-key}") String base64Key) {
    this.key = Base64.getDecoder().decode(base64Key);
}
private String encryptGcm(String plaintext) throws Exception {
    byte[] iv = new byte[12];
    random.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
    byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    byte[] combined = new byte[iv.length + encrypted.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
    return Base64.getEncoder().encodeToString(combined);
}
```

U `SeedData.seedLab06` seed kupci se hashiraju `encoder.encode(...)`.

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab06`
   - Prije: hashevi ivan/maja isti, hash je MD5, ECB deterministički → testovi **padaju**.
   - Poslije: iste lozinke daju **različite** hasheve, hash nije 32-heks MD5, ciphertext je duži od
     16+12 B i dešifriranje starim ECB ključem više ne daje broj. Funkcionalni (register/login, masked) prolaze.

### Alati

- Isti alati za ovisnosti kao lab05 (Dependency-Check, OSV-Scanner, Trivy); za hasheve CrackStation/hashcat.

## 8. Zadatak za samostalni rad

- Dodaj endpoint koji dešifrira karticu (samo za ADMIN) i test da se isti broj dešifrira ispravno iako su
  ciphertexti različiti.
- Pitanja:
  1. Zašto BCrypt/scrypt/Argon2 preferiramo nad SHA-256 za lozinke?
  2. Što točno ECB otkriva (sjeti se poznate „ECB pingvin" slike)?
  3. Gdje bi u produkciji čuvao ključ (KMS, HSM, Key Vault) i kako bi ga rotirao?
