# 00 – Postavljanje okruženja (Java / Spring Boot)

Ovaj dokument vodi te kroz postavljanje razvojnog okruženja za Java verziju VulnShop Akademije.
Cilj je da u 15-ak minuta imaš pokrenutu aplikaciju i da možeš pokretati testove po vježbama.

> Paralelna .NET verzija ima vlastite upute u svom repozitoriju. Vježbe, URL-ovi i seed podaci su identični.

## 1. Preduvjeti

| Alat | Verzija | Napomena |
|---|---|---|
| JDK | **21** (Temurin / OpenJDK) | obavezno 21 (koristimo `record`, `switch pattern`, itd.) |
| Maven | 3.9+ | ili ugrađeni Maven u IDE-u |
| Git | bilo koja novija | za kloniranje |
| IDE | IntelliJ IDEA / VS Code | po izboru |

### Instalacija JDK 21 (Temurin)

- **Windows / macOS:** preuzmi s <https://adoptium.net> (Temurin 21, LTS). Pokreni instalaciju i označi
  *„Set JAVA_HOME"* ako je ponuđeno.
- **Linux (Debian/Ubuntu):**
  ```bash
  sudo apt install temurin-21-jdk     # uz Adoptium APT repo
  # ili
  sudo apt install openjdk-21-jdk
  ```
- Provjera:
  ```bash
  java -version      # mora prikazati 21.x
  mvn -version       # Maven mora koristiti JDK 21
  ```

## 2. Kloniranje repozitorija

```bash
git clone https://github.com/aradovanJava/secure-coding-akademija-java.git
cd secure-coding-akademija-java
```

## 3. IntelliJ IDEA

1. **File → Open…** i odaberi `pom.xml` (ili cijeli direktorij) → *Open as Project*.
   - Alternativno: **File → New → Project from Version Control** i zalijepi URL repozitorija.
2. Pričekaj **Maven sync** (desno dolje); IntelliJ će povući ovisnosti.
3. **File → Project Structure → Project** → *SDK* postavi na **21**, *Language level* na 21.
4. Pokretanje aplikacije: otvori `VulnShopApplication.java` i klikni zelenu strelicu, ili **Run → Run 'VulnShopApplication'**.
   - Aplikacija starta na `http://localhost:8080`.
5. Pokretanje testova po tagovima:
   - **Run → Edit Configurations → + → JUnit**; u polje *Tags* upiši `security`, `functional`, ili npr. `lab07`.
   - Ili iz terminala (vidi odjeljak 6).
6. HTTP Client za `.http` datoteke: otvori bilo koju datoteku iz `http/`, klikni zelenu strelicu uz zahtjev.
   Varijablu `@token` postavi ručno (kopiraj token iz odgovora na `login`).

## 4. VS Code

1. Instaliraj proširenja:
   - **Extension Pack for Java** (Microsoft)
   - **Spring Boot Extension Pack** (Pivotal/Broadcom)
   - **REST Client** (Huachao Mao) – za `.http` datoteke
2. **File → Open Folder…** → odaberi direktorij repozitorija (ili *Git: Clone* iz Command Palette).
3. Projekt se učita; u **Spring Boot Dashboard** (lijeva traka) pojavit će se `vulnshop-akademija` → *Run*.
4. Testovi: otvori **Testing** panel (ikona epruvete). Testove možeš filtrirati; za pokretanje po tagu
   koristi terminal (odjeljak 6).
5. `.http` datoteke: otvori datoteku iz `http/`, klikni *Send Request* iznad zahtjeva.
   Konfiguracija hosta je u `.vscode/settings.json` (`rest-client.environmentVariables`).

> Ako je potreban Maven Wrapper (`./mvnw`), a nije prisutan u repozitoriju, koristi ugrađeni Maven iz
> Extension Pack-a (*Java: Maven*), IntelliJ-jev *Bundled Maven* ili sustavski `mvn`.

## 5. JetBrains Rider

Rider je primarno za .NET; za Java verziju koristi IntelliJ IDEA (ista tvrtka, bolja Java podrška).

## 6. Naredbeni redak

```bash
# Pokretanje aplikacije (port 8080)
mvn spring-boot:run

# Build bez testova
mvn -ntp -DskipTests package

# Testovi po skupinama / vježbama (JUnit 5 tagovi)
mvn test -Dgroups=functional
mvn test -Dgroups=security
mvn test -Dgroups=lab07
```

> **Maven Wrapper:** repozitorij sadrži `mvnw` (Linux/macOS) i `mvnw.cmd` (Windows), pa ne moraš imati
> zasebno instaliran Maven — samo zamijeni `mvn` s `./mvnw` (ili `mvnw` na Windowsu), npr.
> `./mvnw spring-boot:run`, `./mvnw test -Dgroups=security`. IntelliJ i VS Code automatski ga prepoznaju.

### PowerShell: curl

U PowerShell-u je `curl` alias za `Invoke-WebRequest` i ima drukčiju sintaksu. Koristi **`curl.exe`**
(pravi curl) ili `Invoke-RestMethod`:

```powershell
# Pravi curl (preporuka za primjere iz uputa):
curl.exe -s -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"ana\",\"password\":\"Ana123!\"}'

# Ili Invoke-RestMethod:
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"username":"ana","password":"Ana123!"}'
```

## 7. Rješavanje čestih problema

| Problem | Rješenje |
|---|---|
| `Port 8080 already in use` | Zatvori proces na 8080 ili promijeni `server.port` u `application.properties`. Na Windowsu: `netstat -ano \| findstr :8080` pa `taskkill /PID <pid> /F`. |
| `java: error: release version 21 not supported` | IDE koristi stariji JDK. Postavi Project SDK na 21 (IntelliJ: Project Structure; VS Code: *Java: Configure Java Runtime*). |
| Maven ne može preuzeti ovisnosti | Provjeri internet/proxy. Iza proxyja postavi `~/.m2/settings.xml`. |
| `.http` zahtjev vraća 401 | Nisi postavio `@token`. Prvo pozovi `login`, kopiraj `token` iz odgovora u varijablu `@token`. |
| Testovi „ne vide" tagove | Pokreni ih kroz Maven (`mvn test -Dgroups=...`); surefire čita `-Dgroups`. |
| H2 konzola | Na grani `main` je uključena na `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:vulnshop`). Na `rjesenja` je isključena (lab04). |
