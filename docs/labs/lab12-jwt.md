# Lab12 – JWT propusti

## 1. Cilj vježbe

Pokazati tri klasična JWT propusta u ručno napisanom verifikatoru: prihvaćanje `alg:none`, slaba
hardkodirana tajna i neprovjeravanje isteka (`exp`). Polaznik uvodi allowlistu algoritma, usporedbu u
konstantnom vremenu, jaku tajnu iz konfiguracije i provjeru `exp`.

- **OWASP Top 10:2025:** A07 – Authentication Failures
- **CWE:** CWE-347 (Improper Verification of Cryptographic Signature), CWE-798 (Hard-coded Credentials), CWE-613 (Insufficient Session Expiration)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab12/JwtService.java` (ručni HS256; metode `createToken`,
  `verifyVulnerable`, a na rjesenjima i `verify`).
- `lab12/Lab12Controller.java` → `POST /api/lab12/login`, `GET /api/lab12/me`, `GET /api/lab12/admin`.
- Tajna: `app.lab12.jwt-secret` (`secret123` na `main`, jaka iz okoline na `rjesenja`).
- Test: `lab12/Lab12SecurityTest.java`

## 3. Opis ranjivosti

`verifyVulnerable`:
- čita `alg` iz zaglavlja i ako je `none` **preskače** provjeru potpisa,
- uspoređuje potpis običnim `equals` (nije konstantno vrijeme),
- **ne** provjerava `exp`.

Tajna `secret123` je slaba i nalazi se u wordlisti, pa se da razbiti.

## 4. Eksploatacija – korak po korak

1. **alg:none:** pošalji token s headerom `{"alg":"none"}`, payloadom `{"role":"ADMIN",...}` i praznim
   potpisom na `GET /api/lab12/admin` (gotov token je u `http/lab12.http`).
   **Očekivano (ranjivo):** HTTP 200.
2. **Slaba tajna:** razbij tajnu
   ```bash
   hashcat -m 16500 token.txt wordlists/top-100.txt
   ```
   (`secret123` je u listi), pa alatom `jwt_tool` krivotvori token s `role:ADMIN` potpisan tom tajnom →
   `/admin` vraća 200.
3. **Istekli token:** token s prošlim `exp` i dalje prolazi na `/me`.

## 5. Analiza uzroka

```java
if (!"none".equalsIgnoreCase(alg)) { ... if (!expected.equals(parts[2])) return null; } // alg:none preskace
// exp se NE cita
```

## 6. Popravak

- Allowlista algoritma: prihvaćaj **samo HS256**.
- Usporedba potpisa u konstantnom vremenu (`MessageDigest.isEqual`).
- Jaka 256-bitna tajna iz konfiguracije/okoline.
- Provjeri `exp` (i po želji `iss`/`aud`).
- Preporuka: koristi provjerenu biblioteku (jjwt, Nimbus) umjesto ručne implementacije.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
public VerifiedToken verify(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) return null;
    String alg = extractString(new String(base64UrlDecode(parts[0]), UTF_8), "alg");
    if (!"HS256".equals(alg)) return null;                     // allowlista
    byte[] expected = hmacSha256(parts[0] + "." + parts[1], secret);
    byte[] actual = base64UrlDecode(parts[2]);
    if (!MessageDigest.isEqual(expected, actual)) return null; // konstantno vrijeme
    String payloadJson = new String(base64UrlDecode(parts[1]), UTF_8);
    Long exp = extractLong(payloadJson, "exp");
    if (exp == null || Instant.now().getEpochSecond() > exp) return null; // provjera isteka
    return new VerifiedToken(extractString(payloadJson, "sub"), extractString(payloadJson, "role"));
}
```

U kontroleru se poziva `jwtService.verify(token)`, a tajna dolazi iz
`app.lab12.jwt-secret=${LAB12_JWT_SECRET:<jaka base64 vrijednost>}`.

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab12`
   - Prije: alg:none → 200, token potpisan `secret123` → 200, istekli token → prolazi → **padaju**.
   - Poslije: sva tri → **401**. Funkcionalni (login→/me 200, USER→/admin 403, ADMIN→/admin 200) prolaze.

> `JwtService.createToken(username, role, Instant expiresAt)` omogućuje testu da napravi **ispravno potpisan,
> ali istekli** token.

## 8. Zadatak za samostalni rad

- Zamijeni ručni JWT s bibliotekom jjwt i potvrdi da svi testovi i dalje prolaze.
- Pitanja:
  1. Zašto je `alg:none` tako opasan i zašto ga biblioteke danas odbijaju po defaultu?
  2. Zašto usporedba potpisa mora biti u konstantnom vremenu?
  3. Koja je razlika između `exp`, `nbf` i `iat` i zašto provjeravati sve?
