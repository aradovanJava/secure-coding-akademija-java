package hr.span.akademija.vulnshop.common;

import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Zajednicka baza za sve laboratorijske testove.
 *
 * Koristi stvarni posluzitelj na nasumicnom portu ({@code RANDOM_PORT}) i
 * {@link TestRestTemplate}, jer neke vjezbe (lab03 SSRF na samog sebe, lab04
 * stranice gresaka) zahtijevaju stvarno ponasanje servera, a ne MockMvc.
 *
 * Nudi pomocne metode za prijavu i slanje zahtjeva s Bearer tokenom.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractLabTest {

    @Autowired
    protected TestRestTemplate rest;

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    protected String url(String path) {
        return baseUrl() + path;
    }

    /**
     * Gradi apsolutni URI s TOCNO JEDNOM enkodiranom query vrijednoscu.
     *
     * Vazno: {@link TestRestTemplate} enkodira String URL-ove, pa bi rucno
     * prethodno enkodiranje (URLEncoder) dovelo do DVOSTRUKOG enkodiranja
     * (npr. {@code %3A} -> {@code %253A}) i payload nikada ne bi stigao do
     * kontrolera u izvornom obliku. Zato ovdje vracamo vec izgradeni {@link URI}
     * koji RestTemplate salje bez dodatne obrade.
     */
    protected URI uri(String path, String queryParam, String rawValue) {
        return UriComponentsBuilder.fromUriString(baseUrl() + path)
                .queryParam(queryParam, rawValue)
                .build()
                .encode()
                .toUri();
    }

    /** Prijavljuje korisnika preko zajednickog /api/auth/login i vraca token. */
    protected String login(String username, String password) {
        ResponseEntity<Map> response = rest.postForEntity(
                url("/api/auth/login"),
                Map.of("username", username, "password", password),
                Map.class);
        if (response.getStatusCode().value() != 200 || response.getBody() == null) {
            throw new IllegalStateException("Prijava nije uspjela za korisnika " + username);
        }
        return (String) response.getBody().get("token");
    }

    protected HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    /** GET s Bearer tokenom. */
    protected <T> ResponseEntity<T> getWithToken(String path, String token, Class<T> type) {
        return rest.exchange(url(path), HttpMethod.GET,
                new HttpEntity<>(bearer(token)), type);
    }

    /** GET na vec izgradeni URI (bez dodatnog enkodiranja), s Bearer tokenom. */
    protected <T> ResponseEntity<T> getWithToken(URI uri, String token, Class<T> type) {
        return rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(bearer(token)), type);
    }

    /** POST s Bearer tokenom i JSON tijelom. */
    protected <T> ResponseEntity<T> postWithToken(String path, String token, Object body, Class<T> type) {
        return rest.exchange(url(path), HttpMethod.POST,
                new HttpEntity<>(body, bearer(token)), type);
    }

    /** PUT s Bearer tokenom i JSON tijelom. */
    protected <T> ResponseEntity<T> putWithToken(String path, String token, Object body, Class<T> type) {
        return rest.exchange(url(path), HttpMethod.PUT,
                new HttpEntity<>(body, bearer(token)), type);
    }
}
