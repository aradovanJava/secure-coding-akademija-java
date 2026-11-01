# Lab05 – Software Supply Chain Failures (Text4Shell)

## 1. Cilj vježbe

Pokazati rizik ranjive ovisnosti (third-party library) na primjeru `commons-text` 1.9 i
CVE-2022-42889 („Text4Shell"), te kako ga otkriti alatima i ispraviti nadogradnjom + ispravnom uporabom.

- **OWASP Top 10:2025:** A03 – Software Supply Chain Failures
- **CWE:** CWE-1104 (Use of Unmaintained Third Party Components), CWE-1395 (Dependency on Vulnerable Component)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab05/Lab05GreetingController.java` → `POST /api/lab05/greeting`
- Verzija ovisnosti: `pom.xml` → svojstvo `commons-text.version` (1.9 na `main`, 1.13.0 na `rjesenja`).
- Test: `lab05/Lab05SecurityTest.java`

## 3. Opis ranjivosti

Kontroler poziva `StringSubstitutor.createInterpolator().replace("Pozdrav, " + name + "!")`. U
commons-text ≤ 1.9 interpolator po defaultu uključuje opasne *lookup*-ove (`script`, `url`, `dns`,
`file`, …). Ubacivanjem obrazaca `${...}` napadač izvodi:
- `${sys:user.home}`, `${env:PATH}` – curenje okoline,
- `${file:UTF-8:pom.xml}` – čitanje datoteka,
- `${url:UTF-8:http://127.0.0.1:8080/internal/admin/secrets}` – SSRF,
- na starim JDK-ovima (≤ 14, Nashorn) `${script:javascript:...}` je značio **RCE**.

## 4. Eksploatacija – korak po korak

```bash
curl.exe -s -X POST http://localhost:8080/api/lab05/greeting ^
  -H "Content-Type: application/json" -d "{\"name\":\"${sys:java.version}\"}"
```
**Očekivano (ranjivo):** odgovor sadrži stvarnu verziju Jave (npr. `Pozdrav, 21.0.x!`) umjesto doslovnog
`${sys:java.version}`. Isprobaj i `${env:PATH}`, `${file:UTF-8:pom.xml}`, `${url:...}` (vidi `http/lab05.http`).

## 5. Analiza uzroka

```java
StringSubstitutor interpolator = StringSubstitutor.createInterpolator(); // ukljucuje opasne lookupove
String greeting = interpolator.replace("Pozdrav, " + name + "!");         // + ranjiva verzija 1.9
```

Dva su problema: (1) ranjiva verzija knjižnice, (2) interpolacija **korisničkog** unosa.

## 6. Popravak

1. Nadogradi `commons-text` na ≥ 1.10.0 (koristimo 1.13.0).
2. **Ne interpoliraj** korisnički unos — obična konkatenacija / `String.format`.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

`pom.xml`:
```xml
<commons-text.version>1.13.0</commons-text.version>
```

`Lab05GreetingController`:
```java
@PostMapping("/greeting")
public ResponseEntity<?> greeting(@RequestBody GreetingRequest request) {
    String name = request.name() == null ? "" : request.name();
    // POPRAVAK: korisnicki unos se NE interpolira (obicna konkatenacija).
    String greeting = "Pozdrav, " + name + "!";
    return ResponseEntity.ok(Map.of("greeting", greeting));
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab05`
   - Prije: `interpolacijaNijeIzvrsena` i `commonsTextVerzijaNijeRanjiva` **padaju**.
   - Poslije: **prolaze** (odgovor sadrži doslovni `${sys:java.version}`, verzija ≥ 1.10).
2. Ponovi napad: payloadi ostaju doslovni tekst.

### Kako otkriti ranjivu ovisnost alatima

- **GitHub Dependabot** / Dependency graph (automatsko upozorenje na CVE-2022-42889).
- **OWASP Dependency-Check:** `mvn org.owasp:dependency-check-maven:check` (treba NVD API ključ za bržu bazu).
- **OSV-Scanner:** `osv-scanner scan source -r .`
- **Trivy:** `trivy fs .`
- **SBOM (CycloneDX):** `mvn -Psca cyclonedx:makeAggregateBom` → `target/bom.xml`/`bom.json`.
- Zaključavanje verzija i provjera izvora paketa (samo pouzdani repozitoriji).

## 8. Zadatak za samostalni rad

- Pokreni OWASP Dependency-Check ili OSV-Scanner nad granom `main` i pronađi točan CVE; zatim potvrdi da na
  grani `rjesenja` nestaje.
- Pitanja:
  1. Zašto nadogradnja verzije sama po sebi nije dovoljna ako i dalje interpoliramo korisnički unos?
  2. Što je „tranzitivna" ovisnost i kako je vidjeti (`mvn dependency:tree`)?
  3. Kako SBOM pomaže pri reagiranju na novi 0-day u nekoj knjižnici?
