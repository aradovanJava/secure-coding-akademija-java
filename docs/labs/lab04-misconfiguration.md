# Lab04 – Security Misconfiguration

## 1. Cilj vježbe

Prepoznati niz pogrešnih konfiguracija: izložene actuator endpointe koji otkrivaju tajne, stack trace u
odgovorima, prepopustljiv CORS i nedostatak sigurnosnih zaglavlja. Polaznik ih zatvara kroz konfiguraciju
i filtar.

- **OWASP Top 10:2025:** A02 – Security Misconfiguration
- **CWE:** CWE-16, CWE-209 (Information Exposure Through an Error Message), CWE-942 (Permissive CORS), CWE-693 (Protection Mechanism Failure)

## 2. Gdje je kod

- Konfiguracija: `src/main/resources/application.properties` (actuator, error, H2 konzola).
- CORS: `lab04/Lab04CorsConfig.java`; sigurnosna zaglavlja (samo na rjesenjima): `lab04/SecurityHeadersFilter.java`.
- Endpointi: `lab04/Lab04Controller.java` → `GET /api/lab04/ping`, `GET /api/lab04/crash`.
- Test: `lab04/Lab04SecurityTest.java`

## 3. Opis ranjivosti

Na grani `main`:
- `management.endpoints.web.exposure.include=*` i `management.endpoint.env.show-values=ALWAYS` →
  `/actuator/env` otkriva `app.payment.api-key=sk_live_akademija_123`.
- `server.error.include-stacktrace=always` + `include-message=always` → `/crash` vraća stack trace.
- CORS `allowedOriginPatterns("*")` + `allowCredentials(true)` → bilo koja stranica može slati
  autentificirane zahtjeve.
- Nema sigurnosnih zaglavlja; H2 konzola uključena.

## 4. Eksploatacija – korak po korak

1. **Tajne iz actuatora:**
   ```bash
   curl.exe -s http://localhost:8080/actuator/env | findstr sk_live
   ```
   **Očekivano (ranjivo):** vidi se `sk_live_akademija_123`.
2. **Stack trace:** `GET /api/lab04/crash` → tijelo sadrži `IllegalStateException`, `jdbc:h2...`, `at ...`.
3. **CORS:** preflight s `Origin: https://evil.example` (vidi `http/lab04.http`) → u odgovoru
   `Access-Control-Allow-Origin: https://evil.example`.
4. **ZAP/Burp:** passive scan odmah prijavljuje nedostatak `X-Content-Type-Options`, `X-Frame-Options`, CSP.

## 5. Analiza uzroka

Konfiguracija izlaže sve actuator endpointe i detalje grešaka; CORS reflektira svaki origin; ne postoji
filtar koji dodaje sigurnosna zaglavlja.

## 6. Popravak

- Izloži samo `health,info`; `env.show-values=NEVER`.
- `server.error.include-stacktrace=never`, `include-message=never`, `include-exception=false`.
- CORS allowlista iz konfiguracije (`app.cors.allowed-origins`), bez `*`.
- Dodaj filtar sa `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`,
  `Content-Security-Policy: default-src 'self'`, `Referrer-Policy: no-referrer`.
- Isključi H2 konzolu.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

`application.properties`:
```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.env.show-values=NEVER
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-exception=false
spring.h2.console.enabled=false
app.cors.allowed-origins=https://akademija.span.hr
```

`Lab04CorsConfig` (allowlista):
```java
registry.addMapping("/api/lab04/**")
        .allowedOrigins(allowedOrigins)   // iz app.cors.allowed-origins
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders("Authorization", "Content-Type")
        .allowCredentials(true);
```

`SecurityHeadersFilter` (OncePerRequestFilter):
```java
response.setHeader("X-Content-Type-Options", "nosniff");
response.setHeader("X-Frame-Options", "DENY");
response.setHeader("Referrer-Policy", "no-referrer");
if (!request.getRequestURI().startsWith("/lab08")) {       // lab08 ima vlastiti, stroziji CSP
    response.setHeader("Content-Security-Policy", "default-src 'self'");
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab04`
   - Prije: 4 sigurnosna testa (`/actuator/env`, `/crash`, CORS, zaglavlja) **padaju**.
   - Poslije: **prolaze**; funkcionalni (`/actuator/health` 200, `ping` 200) i dalje prolaze.
2. Ponovi napade: `/actuator/env` → 404, `/crash` → 500 bez stack tracea, CORS ne reflektira evil origin,
   `ping` ima `nosniff` i `X-Frame-Options: DENY`.

## 8. Zadatak za samostalni rad

- Dodaj `Strict-Transport-Security` i `Permissions-Policy` te test koji provjerava njihovu prisutnost.
- Pitanja:
  1. Zašto je opasno izložiti `/actuator/env` i `/actuator/heapdump`?
  2. Zašto `Access-Control-Allow-Origin: *` i `allowCredentials(true)` zajedno nisu dopušteni po specifikaciji?
  3. Koja je razlika između razvojnog i produkcijskog profila u Spring Bootu (`application-prod.properties`)?
