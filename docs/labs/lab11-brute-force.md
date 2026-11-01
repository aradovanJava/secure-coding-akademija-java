# Lab11 – Authentication Failures (brute force i enumeracija)

## 1. Cilj vježbe

Pokazati kako različiti odgovori za nepostojećeg korisnika i pogrešnu lozinku omogućuju enumeraciju
korisničkih imena, te kako nedostatak ograničenja pokušaja omogućuje brute force. Polaznik uvodi jedinstven
odgovor i zaključavanje.

- **OWASP Top 10:2025:** A07 – Authentication Failures
- **CWE:** CWE-307 (Improper Restriction of Excessive Authentication Attempts), CWE-204 (Observable Response Discrepancy)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab11/Lab11Controller.java` → `POST /api/lab11/login`
- Seed `lab11_users`: `ivana`/`sunshine1` (lozinka je u `wordlists/top-100.txt`), `tomislav`/`T0m!slav#2026`,
  te `lockme`/`Lock!Me2026` (za test zaključavanja).
- Test: `lab11/Lab11SecurityTest.java`

## 3. Opis ranjivosti

Za nepostojećeg korisnika vraća se **404 „Korisnik ne postoji"**, a za pogrešnu lozinku **401 „Pogrešna
lozinka"**. Razlika otkriva postoji li korisničko ime (enumeracija). Nema ograničenja broja pokušaja, pa je
brute force neometan.

## 4. Eksploatacija – korak po korak

> Napad se izvodi **profesionalnim alatima** (Burp Suite Intruder ili OWASP ZAP Fuzzer) uz
> `wordlists/top-100.txt`. Ne pišemo vlastite napadačke skripte. U `http/lab11.http` je nekoliko ručnih
> zahtjeva za usporedbu.

1. **Enumeracija:** pošalji login za postojećeg (`ivana`) i nepostojećeg korisnika s krivom lozinkom i
   usporedi status/tijelo:
   - `ivana` + kriva → **401 „Pogrešna lozinka"**
   - `ne_postojim` + bilo što → **404 „Korisnik ne postoji"**
   Razlika = dokaz enumeracije.
2. **Brute force (Burp Intruder):** mету `password` postavi na payload set iz `wordlists/top-100.txt`,
   korisnik `ivana`. Jer je `sunshine1` u listi, dobit ćeš **200** na tom pokušaju. (U ZAP-u: desni klik na
   zahtjev → *Fuzz* → *File* payload = wordlists/top-100.txt.)

## 5. Analiza uzroka

```java
if (rows.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Korisnik ne postoji.")); // RANJIVO
if (!encoder.matches(...)) return ResponseEntity.status(401).body(Map.of("error", "Pogresna lozinka.")); // RANJIVO
// nema brojaca pokusaja
```

## 6. Popravak

- Jedinstven odgovor **401 „Neispravno korisničko ime ili lozinka"** za oba slučaja.
- Provjeri lozinku i za nepostojećeg korisnika (dummy hash) radi ujednačenog vremena odgovora.
- Zaključaj nakon 5 neuspjelih pokušaja (15 min) → **429**.
- Napomena: MFA, CAPTCHA, rate limiting po IP-u.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
Attempts state = attempts.computeIfAbsent(username, k -> new Attempts());
synchronized (state) {
    if (state.lockedUntil != null && Instant.now().isBefore(state.lockedUntil)) {
        return ResponseEntity.status(429).body(Map.of("error", "Previse neuspjelih pokusaja. Pokusajte kasnije."));
    }
    List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT password_hash FROM lab11_users WHERE username = ?", username);
    boolean ok;
    if (rows.isEmpty()) { encoder.matches(password, DUMMY_HASH); ok = false; } // ujednaceno vrijeme
    else { ok = encoder.matches(password, (String) rows.get(0).get("password_hash")); }
    if (!ok) {
        state.count++;
        if (state.count >= 5) state.lockedUntil = Instant.now().plus(Duration.ofMinutes(15));
        return ResponseEntity.status(401).body(Map.of("error", "Neispravno korisnicko ime ili lozinka."));
    }
    state.count = 0; state.lockedUntil = null;
    return ResponseEntity.ok(Map.of("status", "prijavljen", "username", username));
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab11`
   - Prije: nepostojeći korisnik (404) ≠ kriva lozinka (401); nema zaključavanja → **padaju**.
   - Poslije: oba slučaja daju **isti** 401 i isto tijelo; nakon 5 krivih za `lockme` i točna lozinka → **429**.
     Funkcionalni (ispravan login → 200) prolazi.

### Alati

- **Burp Suite Community** – Intruder (Sniper, payload = wordlista).
- **OWASP ZAP** – Fuzzer (File payload = `wordlists/top-100.txt`).

## 8. Zadatak za samostalni rad

- Dodaj rate limiting po IP adresi (npr. token bucket) uz postojeće zaključavanje po korisniku; test.
- Pitanja:
  1. Zašto i vremenska razlika u odgovoru može odati postoji li korisnik (timing)?
  2. Koje su mane zaključavanja po korisniku (DoS na tuđi račun) i kako ih ublažiti?
  3. Gdje MFA mijenja jednadžbu rizika?
