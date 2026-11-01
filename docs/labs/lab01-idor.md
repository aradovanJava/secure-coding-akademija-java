# Lab01 – IDOR (Insecure Direct Object Reference)

## 1. Cilj vježbe

Naučiti prepoznati i ispraviti **Broken Access Control** tipa IDOR: kada aplikacija dohvaća objekt
po identifikatoru iz zahtjeva, a ne provjerava smije li ga baš taj korisnik vidjeti. Polaznik
eksploatira ranjivost, analizira uzrok, popravlja provjeru vlasništva i piše jedinični test.

- **OWASP Top 10:2025:** A01 – Broken Access Control
- **CWE:** CWE-639 (Authorization Bypass Through User-Controlled Key)

## 2. Gdje je kod

- Kontroler: `src/main/java/hr/span/akademija/vulnshop/lab01/Lab01OrderController.java`
  - metoda `getOrder(long id, CurrentUser currentUser)` → `GET /api/lab01/orders/{id}`
- Seed podaci: `common/SeedData.java` (`seedLab01`), tablica `lab01_orders`
  (narudžbe 1001, 1002 = *ana*; 1003 = *marko*; 1004 = *admin*).
- Test: `src/test/java/hr/span/akademija/vulnshop/lab01/Lab01SecurityTest.java`

## 3. Opis ranjivosti

Endpoint vraća narudžbu isključivo na temelju `id` iz URL-a. Identifikatori su predvidljivi (redni
brojevi), pa prijavljeni korisnik jednostavno mijenja broj u URL-u i čita tuđe narudžbe (imena, adrese,
iznose). Ovo je jedan od najčešćih propusta u stvarnim aplikacijama (npr. poznati slučajevi curenja
računa mijenjanjem ID-a u URL-u).

## 4. Eksploatacija – korak po korak

Koristi `http/lab01.http` ili `curl.exe`.

1. Prijavi se kao **ana** i zapamti token:
   ```bash
   curl.exe -s -X POST http://localhost:8080/api/auth/login ^
     -H "Content-Type: application/json" -d "{\"username\":\"ana\",\"password\":\"Ana123!\"}"
   ```
2. Dohvati vlastitu narudžbu (OK): `GET /api/lab01/orders/1001` s `Authorization: Bearer <token>`.
3. **Napad:** dohvati Markovu narudžbu `1003` istim tokenom:
   ```bash
   curl.exe -s http://localhost:8080/api/lab01/orders/1003 -H "Authorization: Bearer <token>"
   ```
   **Očekivani rezultat (ranjivo):** HTTP 200 i Markovi podaci (owner `marko`, adresa, iznos) iako
   si prijavljen kao ana → dokaz IDOR-a.

## 5. Analiza uzroka

U `getOrder` se nakon dohvaćanja retka odmah vraća DTO:

```java
// RANJIVO: ne provjerava se je li currentUser vlasnik narudzbe niti je li ADMIN.
Map<String, Object> row = rows.get(0);
return ResponseEntity.ok(toDto(row));
```

Nedostaje autorizacijska provjera: je li `row.owner_username` jednak `currentUser.username()` ili je
korisnik administrator.

## 6. Popravak

Dodaj provjeru vlasništva. Ako narudžba nije korisnikova, a korisnik nije ADMIN, vrati **404** (ne 403) —
tako ne otkrivamo ni postojanje tuđe narudžbe (sprječavamo enumeraciju).

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
if (rows.isEmpty()) {
    return ResponseEntity.status(404).body(Map.of("error", "Narudzba nije pronadena."));
}
Map<String, Object> row = rows.get(0);
// POPRAVAK: provjera vlasnistva. Ako narudzba nije korisnikova, a korisnik nije ADMIN,
// vracamo 404 (ne 403) kako ne bismo otkrili postojanje tude narudzbe.
String owner = (String) row.get("owner_username");
if (!currentUser.username().equals(owner) && !currentUser.isAdmin()) {
    return ResponseEntity.status(404).body(Map.of("error", "Narudzba nije pronadena."));
}
return ResponseEntity.ok(toDto(row));
```

</details>

## 7. Provjera popravka

1. **Testovi vježbe:**
   ```bash
   mvn test -Dgroups=lab01
   ```
   U IDE-u: pokreni `Lab01SecurityTest` i `Lab01FunctionalTest`.
   - Prije popravka: `Lab01SecurityTest.anaNeSmijeDohvatitiTuduNarudzbu` **pada**.
   - Poslije popravka: **prolazi**, a svi funkcionalni i dalje prolaze.
2. **Ponovi napad:** `GET /api/lab01/orders/1003` kao ana sada vraća **404**.
3. Dohvat vlastite (`1001`) i dalje radi (200); admin smije sve (200).

## 8. Zadatak za samostalni rad

- Dodaj endpoint `GET /api/lab01/orders` koji vraća **samo** narudžbe trenutnog korisnika i napiši test
  da ana vidi točno 2 narudžbe, a marko 1.
- Pitanja za razmišljanje:
  1. Zašto je kod IDOR-a bolje vratiti 404 nego 403?
  2. Kako bi spriječio enumeraciju ID-eva (npr. UUID umjesto rednih brojeva) i koja su ograničenja te mjere?
  3. Gdje u slojevitoj arhitekturi (controller/service/repository) pripada autorizacijska provjera?
