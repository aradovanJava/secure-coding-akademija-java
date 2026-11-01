# Lab03 – SSRF (Server-Side Request Forgery)

## 1. Cilj vježbe

Pokazati kako funkcija „pregled poveznice" koja dohvaća korisnički zadani URL može natjerati server da
pošalje zahtjev na interne resurse (loopback, cloud metadata) ili lokalne datoteke. Polaznik uvodi
validaciju sheme i odredišne adrese.

- **OWASP Top 10:2025:** A01 – Broken Access Control (SSRF je od 2025. dio A01)
- **CWE:** CWE-918 (Server-Side Request Forgery)

## 2. Gdje je kod

- `src/main/java/hr/span/akademija/vulnshop/lab03/Lab03PreviewController.java` → `GET /api/lab03/preview?url=`
- Interna meta: `lab03/InternalController.java` → `GET /internal/admin/secrets` (vraća tajne samo s loopbacka)
- Test: `lab03/Lab03SecurityTest.java`

## 3. Opis ranjivosti

`preview` prima `url` i bez ikakve provjere otvara vezu (`openConnection`) te vraća prvih 2000 znakova.
Napadač može:
- dosegnuti **interni** endpoint na samom serveru (`http://127.0.0.1:8080/internal/admin/secrets`),
- dohvatiti **cloud metadata** (`http://169.254.169.254/...`, čest izvor curenja ključeva u oblaku),
- (na Javi) čitati **lokalne datoteke** preko `file://`.

## 4. Eksploatacija – korak po korak

Vrijednost `url` mora biti URL-enkodirana (vidi `http/lab03.http`).

1. Prijavi se kao **ana**, zapamti token.
2. **Napad na interni endpoint:**
   ```bash
   curl.exe -s "http://localhost:8080/api/lab03/preview?url=http%3A%2F%2F127.0.0.1%3A8080%2Finternal%2Fadmin%2Fsecrets" ^
     -H "Authorization: Bearer <token>"
   ```
   **Očekivano (ranjivo):** HTTP 200, a `content` sadrži `Sup3rTajn0!` i `sk_live_akademija_123`.
3. **Cloud metadata:** `url=http://169.254.169.254/latest/meta-data/`.
4. **file:// (Java):** `url=file:///etc/passwd` (Linux) ili `url=file:///C:/Windows/win.ini` (Windows).

## 5. Analiza uzroka

```java
URL target = URI.create(url).toURL();
URLConnection connection = target.openConnection();   // RANJIVO: bez provjere sheme/hosta
```

Nema allowliste shema, nema provjere da odredišna IP adresa nije interna, a slijede se i preusmjeravanja.

## 6. Popravak

- Dopusti samo `http`/`https`.
- Razriješi **sve** adrese hosta i blokiraj loopback, privatne (10/8, 172.16/12, 192.168/16), link-local
  (169.254/16), `::1`, `fc00::/7`, `0.0.0.0`.
- Isključi preusmjeravanja, postavi timeout, vrati **400** za nedopušteno odredište.

<details>
<summary>Referentno rješenje (grana <code>rjesenja</code>)</summary>

```java
URI uri = URI.create(url);
String scheme = uri.getScheme();
if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
    return ResponseEntity.status(400).body(Map.of("error", "Dopustene su samo http i https sheme."));
}
String host = uri.getHost();
if (host == null) {
    return ResponseEntity.status(400).body(Map.of("error", "Neispravan host."));
}
InetAddress[] addresses = InetAddress.getAllByName(host);
for (InetAddress address : addresses) {
    if (isBlocked(address)) {
        return ResponseEntity.status(400)
                .body(Map.of("error", "Odrediste nije dopusteno (interna/loopback adresa)."));
    }
}
HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
connection.setConnectTimeout(5000);
connection.setReadTimeout(5000);
connection.setInstanceFollowRedirects(false);   // bez preusmjeravanja
// ... (status 3xx -> odbij)

private boolean isBlocked(InetAddress address) {
    if (address.isLoopbackAddress() || address.isAnyLocalAddress()
            || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
        return true;
    }
    byte[] bytes = address.getAddress();
    if (bytes.length == 16) {                    // IPv6 ULA fc00::/7
        return ((bytes[0] & 0xFF) & 0xFE) == 0xFC;
    }
    return false;
}
```

</details>

## 7. Provjera popravka

1. `mvn test -Dgroups=lab03`
   - Prije: 5 sigurnosnih testova (127.0.0.1, localhost, [::1], 169.254.169.254, file://) **pada**.
   - Poslije: **prolaze**; funkcionalni (`ftp://` nije 200; bez tokena 401) i dalje prolaze.
2. Ponovi napade: svi vraćaju **400**, a tijelo više ne sadrži `Sup3rTajn0!`.

## 8. Zadatak za samostalni rad

- Dodaj **allowlistu domena** (npr. samo `https://akademija.span.hr`) i test koji potvrđuje da se vanjski
  dopušteni URL dohvaća, a sve ostalo odbija.
- Pitanja:
  1. Zašto provjera samo URL stringa (bez DNS razrješavanja) nije dovoljna (DNS rebinding)?
  2. Zašto treba blokirati preusmjeravanja?
  3. Kako 169.254.169.254 može dovesti do potpune kompromitacije u oblaku?
