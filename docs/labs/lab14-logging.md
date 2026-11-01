# Lab14 – Security Logging & Alerting Failures

## 1. Cilj vježbe

Pokazati tri propusta logiranja: lozinka u logu, log injection kroz CR/LF i izostanak sigurnosnih
događaja/alertiranja. Polaznik uvodi sigurno logiranje i detekciju brute-forcea.

- **OWASP Top 10:2025:** A09 – Security Logging & Alerting Failures
- **CWE:** CWE-778 (Insufficient Logging), CWE-117 (Improper Output Neutralization for Logs), CWE-532 (Insertion of Sensitive Information into Log File)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab14/Lab14Controller.java` → `POST /api/lab14/login`
- Test: `lab14/Lab14SecurityTest.java` (hvata log kroz `OutputCaptureExtension` / `CapturedOutput`).

## 3. Opis ranjivosti

```java
log.info("Login attempt user=" + username + " password=" + password); // lozinka + bez CR/LF sanitizacije
```

- **CWE-532:** lozinka završava u logu.
- **CWE-117:** korisničko ime s `\n` ubacuje lažni redak (npr. `...\n2026-... INFO Login successful user=admin`).
- **CWE-778:** neuspjela prijava se ne bilježi kao sigurnosni događaj → nema ni detekcije brute-forcea.

## 4. Eksploatacija – korak po korak

> Posljedice se vide u **logu aplikacije** (konzola), ne u HTTP odgovoru.

1. **Lozinka u logu:** pošalji login s poznatom lozinkom i pogledaj konzolu — vidi se `password=...`.
2. **Log injection:** `username = "zlo\nLOGIN_SUCCESS_FORGED user=admin"` (vidi `http/lab14.http`) →
   u logu nastane redak koji počinje krivotvorenim tekstom.

## 5. Analiza uzroka

Spajanje neprovjerenog unosa (uključujući tajne) u poruku loga, bez neutralizacije kontrolnih znakova i bez
posebnih sigurnosnih zapisa.

## 6. Popravak

- Nikad ne logiraj tajne.
- Neutraliziraj CR/LF (zamjena s `_`).
- Bilježi `SECURITY_EVENT LOGIN_FAILED user=... ip=...` (WARN) i `LOGIN_SUCCESS` (INFO).
- Nakon 5 neuspjeha za istog korisnika u 5 min: `SECURITY_ALERT BRUTE_FORCE_SUSPECTED ...` (ERROR).
- Napomena: strukturirano (JSON) logiranje, centralizacija (SIEM), korelacijski ID.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
String safeUser = sanitize(rawUsername);   // CR/LF -> '_'
String ip = sanitize(request.getRemoteAddr());
// ... provjera lozinke (bez logiranja lozinke)
if (!ok) {
    log.warn("SECURITY_EVENT LOGIN_FAILED user={} ip={}", safeUser, ip);
    registerFailureAndMaybeAlert(safeUser, ip);     // nakon 5 neuspjeha: log.error("SECURITY_ALERT BRUTE_FORCE_SUSPECTED ...")
    return ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci."));
}
log.info("SECURITY_EVENT LOGIN_SUCCESS user={} ip={}", safeUser, ip);

private String sanitize(String input) {
    StringBuilder sb = new StringBuilder();
    for (char c : input.toCharArray()) sb.append(Character.isISOControl(c) ? '_' : c);
    return sb.toString();
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab14`
   - Prije: lozinka u logu, CR/LF stvara lažni redak, nema `SECURITY_EVENT`/`SECURITY_ALERT` → **padaju**.
   - Poslije: lozinka se ne pojavljuje, nema krivotvorenog retka, postoji `SECURITY_EVENT LOGIN_FAILED` i
     (nakon 5 neuspjeha) `SECURITY_ALERT`. Funkcionalni (ispravan login → 200) prolazi.

### Alati

- **ELK/Graylog/SIEM** za centralizaciju; za vježbu je dovoljan ispis u konzolu koji testovi hvataju.

## 8. Zadatak za samostalni rad

- Dodaj korelacijski ID (MDC) u svaki zapis i test da se isti ID pojavljuje kroz obradu jednog zahtjeva.
- Pitanja:
  1. Koje podatke nikad ne logirati (lozinke, tokeni, OIB, brojevi kartica)?
  2. Zašto je log injection opasan za integritet istrage nakon incidenta?
  3. Što čini „dovoljno" logiranja za detekciju napada bez pretjeranog šuma?
