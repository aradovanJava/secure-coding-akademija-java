# Lab09 – OS command injection

## 1. Cilj vježbe

Pokazati kako ubacivanje korisničkog unosa u naredbu ljuske omogućuje izvršavanje proizvoljnih naredbi, te
ispraviti validacijom i izbjegavanjem ljuske.

- **OWASP Top 10:2025:** A05 – Injection
- **CWE:** CWE-78 (OS Command Injection)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab09/Lab09Controller.java` → `GET /api/lab09/ping?host=`
- Test: `lab09/Lab09SecurityTest.java`

## 3. Opis ranjivosti

Endpoint pokreće `sh -c "ping -c 1 <host>"` (Linux/macOS) ili `cmd.exe /c ping -n 1 <host>` (Windows), a
`host` dolazi izravno od korisnika. Metaznakovi ljuske (`&`, `;`, `|`, `` ` ``) omogućuju nadovezivanje
naredbi.

## 4. Eksploatacija – korak po korak

1. **Nadovezivanje (radi u cmd i sh):** `host = 127.0.0.1 & echo PWNED_LAB09`
   ```
   GET /api/lab09/ping?host=127.0.0.1%20%26%20echo%20PWNED_LAB09
   ```
   **Očekivano (ranjivo):** u izlazu se vidi `PWNED_LAB09` → naredba izvršena.
2. **Linux/macOS:** `host = 127.0.0.1; cat /etc/passwd`.
3. **Windows:** `host = 127.0.0.1 & type C:\Windows\win.ini`.

## 5. Analiza uzroka

```java
pb = new ProcessBuilder("sh", "-c", "ping -c 1 " + host);   // RANJIVO: ljuska interpretira metaznakove
```

Pokretanjem kroz `sh -c` / `cmd /c` ljuska tumači cijeli string, pa `&`/`;` razdvajaju naredbe.

## 6. Popravak

- Stroga validacija (IP/hostname regex `^[A-Za-z0-9.-]{1,253}$`) — odbij sve ostalo prije izvršavanja.
- **Ne koristi ljusku:** `ProcessBuilder` s listom argumenata (host je jedan argument), ili
  `InetAddress.getByName(host).isReachable(...)`.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
private static final Pattern HOST_PATTERN = Pattern.compile("^[A-Za-z0-9.-]{1,253}$");

@GetMapping("/ping")
public ResponseEntity<?> ping(@RequestParam String host) {
    if (host == null || !HOST_PATTERN.matcher(host).matches()) {
        return ResponseEntity.status(400).body(Map.of("error", "Neispravan host."));
    }
    boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    List<String> command = new ArrayList<>();
    if (windows) { command.add("ping"); command.add("-n"); command.add("1"); command.add(host); }
    else { command.add("ping"); command.add("-c"); command.add("1"); command.add(host); }
    ProcessBuilder pb = new ProcessBuilder(command);   // bez sh -c / cmd /c
    // ... citanje izlaza, waitFor(10, SECONDS)
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab09`
   - Prije: `127.0.0.1 & echo PWNED_LAB09` izvrši echo, `127.0.0.1;id` prolazi → **padaju**.
   - Poslije: oba vraćaju **400**, izlaz ne sadrži `PWNED_LAB09`. Funkcionalni (`host=127.0.0.1` → 200) prolazi.

### Alati

- **Semgrep** (`p/owasp-top-ten`) i **Find Security Bugs** prepoznaju `ProcessBuilder`/`Runtime.exec` s
  konkatenacijom.

## 8. Zadatak za samostalni rad

- Zamijeni poziv ljuske s `InetAddress.getByName(host).isReachable(2000)` i prilagodi test (ne ovisi o
  stvarnom pingu).
- Pitanja:
  1. Zašto validacija (allowlist) i izbjegavanje ljuske trebaju ići zajedno?
  2. Koji metaznakovi su opasni u `sh`, a koji u `cmd.exe`?
  3. Kako bi riješio potrebu za stvarnim pozivom vanjskog alata (sandbox, ograničeni korisnik)?
