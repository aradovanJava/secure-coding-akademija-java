# Lab08 – Cross-Site Scripting (XSS)

## 1. Cilj vježbe

Pokazati reflektirani i pohranjeni XSS kada se HTML gradi konkatenacijom bez kodiranja izlaza, te
ispraviti kontekstualnim kodiranjem i CSP-om.

- **OWASP Top 10:2025:** A05 – Injection
- **CWE:** CWE-79 (Cross-site Scripting)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab08/Lab08Controller.java`
  - Reflektirani: `GET /lab08/search?q=` (vraća `text/html`)
  - Pohranjeni: `POST /api/lab08/reviews`, `GET /lab08/products/{id}/reviews`
- Test: `lab08/Lab08SecurityTest.java`

## 3. Opis ranjivosti

HTML stranice nastaju spajanjem stringova s korisničkim unosom (`"...za: " + q`, autor i tekst recenzije),
bez ikakvog kodiranja. Preglednik tada izvršava ubačeni `<script>` ili `onerror` handler.

## 4. Eksploatacija – korak po korak

> Najbolje otvoriti u **pregledniku** (REST klijent neće izvršiti skriptu).

1. **Reflektirani:** otvori
   `http://localhost:8080/lab08/search?q=%3Cscript%3Ealert(document.cookie)%3C%2Fscript%3E`
   **Očekivano (ranjivo):** iskoči `alert` s kolačićem.
2. **Pohranjeni:** spremi recenziju s `"text":"<img src=x onerror=alert('XSS')>"` (POST `/api/lab08/reviews`),
   zatim otvori `http://localhost:8080/lab08/products/1/reviews` → skripta se izvrši pri svakom učitavanju.

## 5. Analiza uzroka

```java
"<h1>Rezultati pretrage za: " + q + "</h1>"                 // RANJIVO (reflektirani)
html.append(row.get("author")).append(": ").append(row.get("text")); // RANJIVO (pohranjeni)
```

Izlaz se ne kodira za HTML kontekst, a nema ni CSP-a kao druge crte obrane.

## 6. Popravak

Kontekstualno kodiranje izlaza (`HtmlUtils.htmlEscape`), validacija duljine ulaza, te zaglavlje
`Content-Security-Policy: default-src 'self'; script-src 'self'; object-src 'none'`. Napomena: predlošci
(Thymeleaf `th:text`, Razor `@`) kodiraju automatski; `th:utext`/`Html.Raw` su opasni.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
private static final String CSP = "default-src 'self'; script-src 'self'; object-src 'none'";

@GetMapping(value = "/lab08/search", produces = MediaType.TEXT_HTML_VALUE)
public ResponseEntity<String> search(@RequestParam(defaultValue = "") String q) {
    String safe = HtmlUtils.htmlEscape(q);                   // POPRAVAK: kodiranje izlaza
    String html = "...<h1>Rezultati pretrage za: " + safe + "</h1>...";
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
            .header("Content-Security-Policy", CSP).body(html);
}

// recenzije:
String author = HtmlUtils.htmlEscape(String.valueOf(row.get("author")));
String text = HtmlUtils.htmlEscape(String.valueOf(row.get("text")));
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab08`
   - Prije: odgovor sadrži nekodirani `<script>`/`<img ... onerror=`, nema CSP-a → **padaju**.
   - Poslije: payload je `&lt;script&gt;...`, postoji CSP sa `script-src` → **prolaze**. Funkcionalni
     (obična pretraga/recenzija) prolaze.
2. Ponovi napad u pregledniku: skripta se više ne izvršava.

### Alati

- **OWASP ZAP** (passive + active scan prepoznaje reflektirani XSS), **Burp** Repeater.

## 8. Zadatak za samostalni rad

- Prebaci prikaz recenzija na Thymeleaf predložak (`th:text`) i pokaži da je automatski siguran; dodaj test.
- Pitanja:
  1. Koja je razlika između HTML, atributnog i JavaScript konteksta kodiranja?
  2. Zašto je CSP „defense in depth", a ne zamjena za kodiranje izlaza?
  3. Što je DOM-based XSS i zašto ga server-side kodiranje ne pokriva?
