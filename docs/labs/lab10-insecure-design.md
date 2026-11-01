# Lab10 – Insecure Design (poslovna logika)

## 1. Cilj vježbe

Pokazati propuste u dizajnu poslovne logike (povjerenje u cijenu/količinu s klijenta, višestruka primjena
kupona, nedostatak provjere stanja), te ih ispraviti autoritativnim izvorom podataka i validacijom.

- **OWASP Top 10:2025:** A06 – Insecure Design
- **CWE:** CWE-840 (Business Logic Errors), CWE-20 (Improper Input Validation)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab10/Lab10Controller.java`
  - `GET /api/lab10/wallet`, `POST /api/lab10/checkout`
- Seed: novčanici 1000.00 EUR po korisniku; proizvodi 1 Laptop 899.00, 2 Slušalice 99.00, 3 Kabel 9.99;
  kupon `AKADEMIJA10` = 10 %.
- Test: `lab10/Lab10SecurityTest.java`

## 3. Opis ranjivosti

Server vjeruje `unitPrice` i `quantity` iz zahtjeva (dopušta i negativne), primjenjuje **svaki** kupon iz
liste (pa i isti više puta) i mijenja stanje bez provjere. Posljedice:
- **negativna količina** → `unitPrice * quantity` je negativan → stanje **raste**,
- **lažna cijena** 0.01 za laptop → naplaćeno 0.01,
- **isti kupon 3×** → popust se slaže (0.9³).

## 4. Eksploatacija – korak po korak

Prijavi se (npr. ana), zapamti token. Vidi `http/lab10.http`.

1. **Negativna količina:**
   ```json
   {"items":[{"productId":1,"quantity":-5,"unitPrice":899.00}],"couponCodes":[]}
   ```
   **Očekivano (ranjivo):** stanje novčanika **poraste**.
2. **Lažna cijena:** `quantity:1, unitPrice:0.01` → naplaćeno 0.01.
3. **Kupon 3×:** `couponCodes:["AKADEMIJA10","AKADEMIJA10","AKADEMIJA10"]` → prekomjeran popust.

## 5. Analiza uzroka

```java
total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))); // cijena/kolicina s klijenta
for (String coupon : body.couponCodes()) { if (AKADEMIJA10) total = total.multiply(0.90); } // svaki kupon
BigDecimal newBalance = balance.subtract(total); // nema provjere total>0 ni stanja
```

## 6. Popravak

Cijene iz baze, `quantity` 1..10, najviše jedan kupon i jednokratna uporaba po korisniku, `total > 0`,
provjera dostatnog stanja, sve u transakciji. Napomena: modeliranje prijetnji (STRIDE), abuse cases.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
@PostMapping("/checkout")
@Transactional
public ResponseEntity<?> checkout(@RequestBody CheckoutRequest body, CurrentUser currentUser) {
    BigDecimal total = BigDecimal.ZERO;
    for (CheckoutItem item : body.items()) {
        if (item.quantity() < 1 || item.quantity() > 10) return bad("Kolicina mora biti izmedu 1 i 10.");
        BigDecimal price = jdbc.queryForObject(            // cijena IZ BAZE, ne iz zahtjeva
                "SELECT price FROM lab10_products WHERE id = ?", BigDecimal.class, item.productId());
        total = total.add(price.multiply(BigDecimal.valueOf(item.quantity())));
    }
    if (couponCodes != null && !couponCodes.isEmpty()) {
        if (couponCodes.size() > 1) return bad("Dopusten je najvise jedan kupon.");
        // provjera KNOWN_COUPON + lab10_coupon_usage (jednokratno po korisniku)
        total = total.multiply(new BigDecimal("0.90"));
    }
    total = total.setScale(2, RoundingMode.HALF_UP);
    if (total.signum() <= 0) return bad("Neispravan ukupni iznos.");
    if (balance.compareTo(total) < 0) return bad("Nedovoljno sredstava.");
    // UPDATE stanja
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab10` (testovi mjere razliku stanja prije/poslije jer se baza dijeli).
   - Prije: negativna količina poveća stanje, cijena 0.01 naplaćena, kupon 3× → **padaju**.
   - Poslije: negativna količina → 400 (stanje nepromijenjeno), laptop naplaćen 899.00, kupon jednom/400 →
     **prolaze**. Funkcionalni (ispravan checkout umanji stanje točno) prolazi.

## 8. Zadatak za samostalni rad

- Dodaj provjeru maksimalnog iznosa narudžbe i „rate limit" broja checkouta po minuti; napiši test.
- Pitanja:
  1. Zašto se cijena **nikad** ne smije uzeti s klijenta?
  2. Kako bi modelirao „abuse case" za kupone (jednokratnost, trajanje, kombiniranje)?
  3. Zašto je transakcija ovdje bitna (vidi i lab15)?
