# Lab13 – Nesigurna deserijalizacija

## 1. Cilj vježbe

Pokazati zašto je deserijalizacija nepouzdanih Java objekata opasna (izvršavanje koda tijekom
`readObject`), te kako je ograničiti `ObjectInputFilter` allowlistom (ili, bolje, prijeći na JSON/DTO).

- **OWASP Top 10:2025:** A08 – Software or Data Integrity Failures
- **CWE:** CWE-502 (Deserialization of Untrusted Data)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab13/Lab13Controller.java` → `POST /api/lab13/cart/import`
  (tijelo je Base64 Java-serijaliziranog objekta).
- Legitimne klase: `lab13/Cart.java`, `lab13/CartItem.java`.
- „Gadget": `lab13/DiagnosticsTask.java` (`readObject` zapisuje **bezopasnu marker datoteku** na zadanu
  putanju — simulira proizvoljan zapis datoteke; nema pokretanja procesa ni mreže).
- Alat za generiranje payloada: `src/test/java/hr/span/akademija/vulnshop/lab13/tools/Lab13PayloadGenerator.java`.
- Test: `lab13/Lab13SecurityTest.java`

## 3. Opis ranjivosti

`importCart` radi `new ObjectInputStream(...).readObject()` bez filtra. `ObjectInputStream` može
instancirati **bilo koju** serijalizabilnu klasu s classpatha i pozvati njezin `readObject`. Napadač šalje
serijalizirani `DiagnosticsTask` umjesto `Cart` i time izvodi zapis datoteke **tijekom deserijalizacije**,
prije nego aplikacija uopće provjeri tip. U stvarnom svijetu gadget lanci (ysoserial, CommonsCollections)
vode do RCE-a.

## 4. Eksploatacija – korak po korak

1. Generiraj payloade. Najjednostavnije je iz IDE-a: otvori
   `src/test/java/hr/span/akademija/vulnshop/lab13/tools/Lab13PayloadGenerator.java` i pokreni `main`
   (desni klik → *Run*). Ispisuje dva Base64 niza: legitimnu košaricu i napadački `DiagnosticsTask` (ciljna
   putanja je u temp direktoriju).
   - Iz naredbenog retka možeš i pokrenuti test `Lab13SecurityTest`/`Lab13FunctionalTest`, koji sami grade
     payload preko `Lab13PayloadGenerator`, pa poseban korak nije nužan.
2. **Napad:** pošalji napadački payload kao tijelo:
   ```bash
   curl.exe -s -X POST http://localhost:8080/api/lab13/cart/import -H "Content-Type: text/plain" -d "<ATTACK_BASE64>"
   ```
   **Očekivano (ranjivo):** odgovor 200/ok i **marker datoteka nastane** na zadanoj putanji → dokaz izvršavanja.

## 5. Analiza uzroka

```java
try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
    Object obj = ois.readObject();   // RANJIVO: nema ObjectInputFilter-a
```

## 6. Popravak

- Postavi `ObjectInputFilter` **allowlistu** prije `readObject` (samo `Cart`, `CartItem`, nužne JDK klase),
  uz ograničenje dubine i veličine.
- Još bolje: ne koristiti Java serijalizaciju za nepouzdane podatke — JSON s eksplicitnim DTO-om (+ HMAC
  potpis podataka ako integritet mora biti zajamčen).

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
private static final ObjectInputFilter ALLOWLIST = ObjectInputFilter.Config.createFilter(
        "hr.span.akademija.vulnshop.lab13.Cart;"
                + "hr.span.akademija.vulnshop.lab13.CartItem;"
                + "java.util.ArrayList;java.lang.*;"
                + "maxdepth=5;maxbytes=10000;!*");

@PostMapping("/cart/import")
public ResponseEntity<?> importCart(@RequestBody String base64Body) {
    byte[] data = Base64.getDecoder().decode(base64Body.trim());
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
        ois.setObjectInputFilter(ALLOWLIST);   // nedopustene klase -> InvalidClassException
        Object obj = ois.readObject();
        // ...
    } catch (Exception e) {
        return ResponseEntity.status(400).body(Map.of("error", "Neispravan sadrzaj kosarice."));
    }
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab13`
   - Prije: napadački payload → datoteka nastane → test **pada**.
   - Poslije: napadački payload → **400**, datoteka **ne** nastane. Funkcionalni (legitimna košarica 2
     stavke → 200, itemCount=2) prolazi.

### Alati

- **Find Security Bugs** (`OBJECT_DESERIALIZATION`), **Semgrep** prepoznaju `ObjectInputStream.readObject`.

## 8. Zadatak za samostalni rad

- Prepiši endpoint da prima **JSON** `Cart` (Jackson + DTO) umjesto Java serijalizacije i prilagodi testove.
- Pitanja:
  1. Zašto filtar mora biti postavljen **prije** `readObject`?
  2. Što su gadget lanci i zašto je dovoljno imati „nedužnu" knjižnicu na classpathu?
  3. Kada je potreban HMAC potpis serijaliziranih podataka?
