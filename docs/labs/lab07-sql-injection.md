# Lab07 – SQL injection

## 1. Cilj vježbe

Pokazati klasičnu SQL injekciju kroz konkatenaciju korisničkog unosa u upit (pretraga i prijava) i
ispraviti je parametriziranim upitima.

- **OWASP Top 10:2025:** A05 – Injection
- **CWE:** CWE-89 (SQL Injection)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab07/Lab07Controller.java`
  - `GET /api/lab07/products/search?q=`, `POST /api/lab07/login`
- Seed: `lab07_products` (vidljivi Laptop/Miš/Tipkovnica/Monitor + skriveni „Interni prototip X"),
  `lab07_accounts` (lozinke u čistom tekstu, namjerno).
- Test: `lab07/Lab07SecurityTest.java`

## 3. Opis ranjivosti

Pretraga gradi: `... WHERE hidden = FALSE AND name LIKE '%" + q + "%'`. Prijava gradi:
`... WHERE username='" + u + "' AND password='" + p + "'`. Korisnik može promijeniti strukturu upita.

## 4. Eksploatacija – korak po korak

1. **Skriveni proizvodi:** `q = %' OR '1'='1`
   ```
   GET /api/lab07/products/search?q=%25%27%20OR%20%271%27%3D%271
   ```
   **Očekivano (ranjivo):** u rezultatima se pojavi „Interni prototip X".
2. **UNION – izvlačenje računa:** `q = %' UNION SELECT id, username, password, 0 FROM lab07_accounts --`
   → vraća `SuperTajna2026`, `Shop!2026`.
3. **Zaobilazak prijave:** `username = admin'--`, bilo koja lozinka → (ranjivo) 200.

Vidi `http/lab07.http` za točno URL-enkodirane vrijednosti.

## 5. Analiza uzroka

```java
String sql = "SELECT ... FROM lab07_products WHERE hidden = FALSE AND name LIKE '%" + q + "%'"; // RANJIVO
String sql = "SELECT ... FROM lab07_accounts WHERE username = '" + u + "' AND password = '" + p + "'"; // RANJIVO
```

Korisnički unos postaje dio SQL koda umjesto da ostane podatak.

## 6. Popravak

Parametrizirani upiti (`?` placeholderi), escape LIKE metaznakova (`%`, `_`, `\`) uz `ESCAPE`, validacija
duljine. Napomena: ORM i načelo najmanjih ovlasti DB korisnika dodatno smanjuju rizik.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
@GetMapping("/products/search")
public ResponseEntity<?> search(@RequestParam String q) {
    if (q.length() > 100) {
        return ResponseEntity.status(400).body(Map.of("error", "Preduga pretraga."));
    }
    String pattern = "%" + escapeLike(q) + "%";
    List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, name, description, price FROM lab07_products "
                    + "WHERE hidden = FALSE AND name LIKE ? ESCAPE '\\'", pattern);
    return ResponseEntity.ok(rows);
}

@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest body) {
    List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT id, username FROM lab07_accounts WHERE username = ? AND password = ?", u, p);
    // ...
}

private String escapeLike(String input) {
    return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab07`
   - Prije: `' OR '1'='1` otkriva skriveni proizvod, UNION izvlači lozinke, `admin'--` prolazi → **padaju**.
   - Poslije: skriveni proizvod se ne pojavljuje, UNION ne vraća lozinke, `admin'--` → **401**. Funkcionalni
     (`q=Laptop` → 1 rezultat; ispravna prijava → 200) prolaze.

### Alati

- **OWASP ZAP** aktivni scan / **Burp** Repeater/Intruder nad `q`; **sqlmap** (`sqlmap -u ".../search?q=1"`).
- SAST: **SpotBugs + Find Security Bugs** (`mvn -Psast spotbugs:check`), **Semgrep** (`p/owasp-top-ten`).

## 8. Zadatak za samostalni rad

- Dodaj parametar `maxPrice` i implementiraj ga parametrizirano; napiši test da injekcija kroz `maxPrice`
  ne radi.
- Pitanja:
  1. Zašto parametrizacija pobjeđuje „escaping" ulaza?
  2. Kako bi ograničio DB korisnika (least privilege) da UNION na druge tablice ne uspije?
  3. Zašto lozinke u `lab07_accounts` nikad ne bi smjele biti u čistom tekstu (vidi lab06)?
