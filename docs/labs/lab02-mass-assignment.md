# Lab02 – Mass assignment / eskalacija privilegija

## 1. Cilj vježbe

Pokazati kako slijepo vezivanje (binding) cijelog JSON tijela na entitet omogućuje korisniku da postavi
polje koje ne bi smio (npr. `role`) i tako eskalira privilegije. Polaznik uvodi DTO s dopuštenim poljima
i validaciju.

- **OWASP Top 10:2025:** A01 – Broken Access Control
- **CWE:** CWE-915 (Improperly Controlled Modification of Dynamically-Determined Object Attributes), CWE-269 (Improper Privilege Management)

## 2. Gdje je kod

- Kontroler: `src/main/java/hr/span/akademija/vulnshop/lab02/Lab02ProfileController.java`
  - `PUT /api/lab02/profile`, `GET /api/lab02/profile`, `GET /api/lab02/admin/users`
- Tablica `lab02_profiles(user_id, display_name, email, role)` (seed u `common/SeedData.java`).
- Test: `lab02/Lab02SecurityTest.java`

## 3. Opis ranjivosti

`PUT /profile` prima objekt `ProfileUpdate` koji sadrži i polje `role` te ga izravno zapisuje u bazu.
Administratorska ovlast (`GET /admin/users`) provjerava se prema `role` iz te iste tablice. Dakle korisnik
može poslati `"role":"ADMIN"` i dobiti administratorski pristup.

## 4. Eksploatacija – korak po korak

1. Prijavi se kao **marko** (`Marko123!`), zapamti token.
2. Provjeri profil: `GET /api/lab02/profile` → `role: USER`.
3. **Napad:** pošalji PUT s ulogom ADMIN:
   ```bash
   curl.exe -s -X PUT http://localhost:8080/api/lab02/profile ^
     -H "Authorization: Bearer <token>" -H "Content-Type: application/json" ^
     -d "{\"displayName\":\"Marko\",\"email\":\"m@x.hr\",\"role\":\"ADMIN\"}"
   ```
4. Pristupi admin popisu: `GET /api/lab02/admin/users`.
   **Očekivani rezultat (ranjivo):** nakon PUT-a profil ima `role: ADMIN`, a `/admin/users` vraća 200 i popis.

## 5. Analiza uzroka

```java
public record ProfileUpdate(String displayName, String email, String role) { }
// ...
jdbc.update("UPDATE lab02_profiles SET display_name = ?, email = ?, role = COALESCE(?, role) WHERE user_id = ?",
        update.displayName(), update.email(), update.role(), currentUser.id());
```

DTO sadrži `role` i prosljeđuje ga u `UPDATE`. Korisnik kontrolira polje koje bi smio mijenjati samo
administrator kroz poseban proces.

## 6. Popravak

DTO smije sadržavati **samo** polja koja korisnik smije mijenjati (`displayName`, `email`), uz Bean
Validation. Uloga se ne dira kroz ovaj endpoint.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
public record ProfileUpdate(
        @NotBlank @Size(max = 200) String displayName,
        @NotBlank @Email @Size(max = 200) String email) {
}

@PutMapping("/profile")
public ResponseEntity<?> updateProfile(@Valid @RequestBody ProfileUpdate update, CurrentUser currentUser) {
    // POPRAVAK: azuriramo SAMO displayName i email; role se nikad ne dira kroz ovaj endpoint.
    jdbc.update(
            "UPDATE lab02_profiles SET display_name = ?, email = ? WHERE user_id = ?",
            update.displayName(), update.email(), currentUser.id());
    return ResponseEntity.ok(toDto(loadProfile(currentUser.id())));
}
```

Uz to dodaj `import jakarta.validation...` (`@Valid`, `@NotBlank`, `@Email`, `@Size`).
Promjena uloge ide isključivo kroz zaseban administratorski proces.

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab02`
   - Prije: `markoNeSmijePostatiAdmin` i `markoNemaPristupAdminUsers` **padaju**.
   - Poslije: **prolaze**; funkcionalni (ana mijenja ime, admin vidi popis) i dalje prolaze.
2. Ponovi napad: nakon PUT-a s `role=ADMIN`, profil i dalje ima `USER`, `/admin/users` → **403**.

## 8. Zadatak za samostalni rad

- Dodaj administratorski endpoint `PUT /api/lab02/admin/users/{id}/role` koji smije mijenjati ulogu samo
  ako je pozivatelj ADMIN, i napiši test.
- Pitanja:
  1. Koja je razlika između „allowlist" i „denylist" pristupa poljima DTO-a?
  2. Kako bi isti problem izgledao uz JPA entitet i `@RequestBody Entity`?
  3. Zašto validacija ulaza nije dovoljna bez autorizacije (i obrnuto)?
