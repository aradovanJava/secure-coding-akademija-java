# Lab15 – Mishandling of Exceptional Conditions

## 1. Cilj vježbe

Pokazati tri posljedice lošeg rukovanja iznimkama: fail-open sigurnosne provjere, nedostatak transakcije
(nekonzistentno stanje) i curenje detalja greške. Polaznik uvodi fail-closed, transakciju i generički
handler.

- **OWASP Top 10:2025:** A10 – Mishandling of Exceptional Conditions
- **CWE:** CWE-636 (Not Failing Securely / „fail open"), CWE-755 (Improper Handling of Exceptional Conditions), CWE-209 (Information Exposure Through an Error Message), CWE-460 (Improper Cleanup on Thrown Exception)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab15/Lab15Controller.java` → `GET /api/lab15/accounts`,
  `POST /api/lab15/transfer`
- `lab15/LimitService.java` (baca iznimku za nepoznatu valutu / neispravan iznos / prekoračen limit).
- Na rjesenjima: `lab15/Lab15ExceptionHandler.java` (generička poruka + korelacijski ID).
- Seed `lab15_accounts`: HR-ANA-001 (ana, 5000), HR-MARKO-001 (marko, 1000), HR-ADMIN-001 (admin, 10000).
- Test: `lab15/Lab15SecurityTest.java`

## 3. Opis ranjivosti

```java
try { limitService.check(amount, currency); } catch (Exception e) { /* log.debug i nastavi */ } // FAIL-OPEN
jdbc.update("... balance - ? WHERE account_no = ?", amount, from);  // terecenje
int updated = jdbc.update("... balance + ? WHERE account_no = ?", amount, to); // odobrenje (bez transakcije)
if (updated == 0) throw new IllegalStateException("... " + to + " ne postoji ..."); // detaljna poruka
```

- **Fail-open:** iznimka provjere limita se guta → transfer 50000 s valutom `XYZ` prolazi.
- **Bez transakcije:** ako odredišni račun ne postoji, terećenje se **ne** poništi → novac nestane.
- **Curenje:** poruka greške vraća naziv klase iznimke i detalje (SQL/naziv tablice).

## 4. Eksploatacija – korak po korak

Prijavi se kao ana, zapamti token (vidi `http/lab15.http`).

1. **Fail-open:**
   ```json
   {"fromAccount":"HR-ANA-001","toAccount":"HR-MARKO-001","amount":"50000.00","currency":"XYZ"}
   ```
   **Očekivano (ranjivo):** transfer prođe iako valuta nije dopuštena i iznos je iznad limita.
2. **Gubitak novca:** transfer na `HR-NEPOSTOJI-999` → saldo ane se smanji, novac nigdje ne stigne; odgovor
   sadrži `IllegalStateException` i naziv tablice.

## 5. Analiza uzroka

Sigurnosna provjera koja „pukne" tretira se kao uspjeh (fail-open); višekoračna izmjena stanja nije u
transakciji; globalni handler ne skriva detalje.

## 6. Popravak

- **Fail-closed:** svaka iznimka u sigurnosnoj provjeri = odbij (400), bez izvršavanja transfera.
- Validacija ulaza unaprijed (BigDecimal, valuta allowlista, iznos > 0).
- **Transakcija** (`@Transactional`) da se terećenje poništi ako odobrenje padne.
- Globalni handler (`@RestControllerAdvice`) s generičkom porukom i korelacijskim ID-om (detalji samo u log).

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
@PostMapping("/transfer")
@Transactional
public ResponseEntity<?> transfer(@RequestBody TransferRequest body, CurrentUser currentUser) {
    BigDecimal amount;
    try { amount = new BigDecimal(body.amount()); }
    catch (Exception e) { return bad("Neispravan iznos."); }

    try { limitService.check(amount, body.currency()); }      // FAIL-CLOSED
    catch (Exception e) { return bad("Transfer nije dopusten (provjera limita)."); }

    // provjera vlasnistva + dostatnog stanja ...
    jdbc.update("UPDATE lab15_accounts SET balance = balance - ? WHERE account_no = ?", amount, body.fromAccount());
    int updated = jdbc.update("UPDATE lab15_accounts SET balance = balance + ? WHERE account_no = ?", amount, body.toAccount());
    if (updated == 0) throw new IllegalStateException("odredisni racun ne postoji"); // @Transactional -> rollback
    return ResponseEntity.ok(Map.of("status", "izvrseno", "amount", amount));
}
```

```java
@RestControllerAdvice(basePackages = "hr.span.akademija.vulnshop.lab15")
public class Lab15ExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handle(Exception ex) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("Lab15 greska [{}]: {}", correlationId, ex.toString()); // detalji SAMO u log
        return ResponseEntity.status(400).body(Map.of(
                "error", "Zahtjev se ne moze obraditi.", "correlationId", correlationId));
    }
}
```

> Handler je namjerno ograničen na `basePackages = lab15` da ne presretne lab04 `/crash` (koji mora ostati
> 500 bez detalja).

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab15` (saldo se mjeri kao razlika prije/poslije).
   - Prije: 50000 XYZ prođe, transfer na nepostojeći račun gubi novac, poruka otkriva detalje → **padaju**.
   - Poslije: 50000 XYZ → 4xx (saldo nepromijenjen), transfer na nepostojeći → 4xx (saldo nepromijenjen),
     tijelo bez naziva klase/SQL-a. Funkcionalni (100 EUR → 200, saldo -100) prolazi.

## 8. Zadatak za samostalni rad

- Dodaj dnevni zbroj transfera po računu i odbij kad bi se limit prekoračio zbrojem; test.
- Pitanja:
  1. Što znači „fail securely" i zašto je default kod sigurnosnih provjera „odbij"?
  2. Zašto višekoračne izmjene stanja moraju biti atomske (transakcija)?
  3. Kako korelacijski ID pomaže podršci bez otkrivanja internih detalja korisniku?
