# 01 – Profesionalni alati (Tjedan 1)

Ovdje su alati koje koristimo u Tjednu 1, s **konkretnim naredbama nad VulnShop Akademijom**. Gdje alat
nije dio CI-a, to je jasno naznačeno (pokreće se lokalno).

> Aplikacija radi na `http://localhost:8080`. Pokreni je (`mvn spring-boot:run`) prije dinamičkih alata.

## 1. OWASP ZAP (Zed Attack Proxy)

Besplatan DAST alat.

- **Automated Scan:** *Quick Start → Automated Scan*, URL `http://localhost:8080`, *Attack*.
  Očekuj nalaze za lab04 (nedostatak sigurnosnih zaglavlja), lab08 (XSS).
- **Manual Explore:** otvori preglednik kroz ZAP proxy i ručno prođi lab08 stranice.
- **Fuzzer (lab11):** u *History* odaberi `POST /api/lab11/login`, desni klik → *Fuzz*; označi vrijednost
  `password`, dodaj *File* payload = `wordlists/top-100.txt`. Za `ivana` ćeš dobiti 200 na `sunshine1`.

## 2. Burp Suite Community

- **Proxy:** uhvati promet preglednika.
- **Repeater:** ručno mijenjaj zahtjeve (idealno za lab01 IDOR – mijenjaj `id`, lab07 SQLi – mijenjaj `q`).
- **Intruder (lab11):** Sniper napad na `password`, payload set iz `wordlists/top-100.txt`.

## 3. SonarQube for IDE (SonarLint)

Instaliraj u IntelliJ/VS Code. Prikazuje „Security Hotspots" dok tipkaš — primijeti upozorenja za
konkatenaciju SQL-a (lab07), `ObjectInputStream` (lab13), slabe hasheve (lab06).

## 4. Semgrep

```bash
semgrep scan --config p/owasp-top-ten .
```
Prijavljuje npr. SQL injection (lab07), command injection (lab09), nesigurnu deserijalizaciju (lab13).
Pokreće se **lokalno** (nije u ovom CI-u).

## 5. SpotBugs + Find Security Bugs (SAST, Java)

Konfigurirano kao Maven profil `sast` (ne pokreće se po defaultu):

```bash
mvn -Psast spotbugs:check
```
Find Security Bugs detektira obrasce poput `SQL_INJECTION_JDBC`, `COMMAND_INJECTION`,
`OBJECT_DESERIALIZATION`, `WEAK_MESSAGE_DIGEST_MD5`. Pokreni lokalno (treba pristup Maven Centralu za
dohvat plugina).

## 6. OWASP Dependency-Check (SCA, lab05)

```bash
mvn org.owasp:dependency-check-maven:check
```
Pronalazi CVE-2022-42889 za `commons-text` 1.9 na grani `main`. Napomena: za bržu i potpuniju bazu
registriraj **NVD API ključ** (`-Dnvd.api.key=...`).

## 7. OSV-Scanner

```bash
osv-scanner scan source -r .
```
Čita `pom.xml` i javlja ranjive verzije (commons-text 1.9).

## 8. Trivy

```bash
trivy fs .
```
Skenira ovisnosti (i, ako gradiš image, kontejner). Brz pregled poznatih ranjivosti.

## 9. CycloneDX (SBOM)

Maven profil `sca` (ne pokreće se po defaultu):

```bash
mvn -Psca cyclonedx:makeAggregateBom
```
Generira `target/bom.xml` / `target/bom.json` (SBOM) za praćenje ovisnosti i brzu reakciju na nove CVE-ove.

## 10. GitHub Dependabot

U repozitoriju na GitHubu uključi *Dependency graph* i *Dependabot alerts*. Automatski će prijaviti ranjivi
`commons-text` 1.9 i predložiti nadogradnju (grana `rjesenja` je već na 1.13.0).

---

## Što je u CI-u, a što lokalno

| Alat | U CI-u (GitHub Actions)? | Napomena |
|---|---|---|
| Build + JUnit testovi (security/functional) | **Da** | `.github/workflows/ci.yml` |
| SpotBugs + Find Security Bugs | Ne (lokalno) | `mvn -Psast spotbugs:check` |
| CycloneDX SBOM | Ne (lokalno) | `mvn -Psca cyclonedx:makeAggregateBom` |
| OWASP Dependency-Check / OSV / Trivy / Semgrep / ZAP / Burp | Ne (lokalno) | treba mrežni pristup bazama/pluginovima |
| Dependabot | Da (GitHub platforma) | izvan workflowa |

Preporuka: u stvarnom projektu SAST i SCA integriraj u CI (zaseban job), uz prag ozbiljnosti koji ruši build.
