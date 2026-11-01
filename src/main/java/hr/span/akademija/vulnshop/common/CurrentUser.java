package hr.span.akademija.vulnshop.common;

/**
 * Trenutno prijavljeni korisnik, izveden iz valjanog Bearer tokena.
 * Postavlja ga {@link AuthFilter} kao atribut zahtjeva.
 */
public record CurrentUser(long id, String username, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
