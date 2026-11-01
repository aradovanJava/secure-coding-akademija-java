package hr.span.akademija.vulnshop.common;

/** Baca se kada prijavljeni korisnik nema dovoljno ovlasti (HTTP 403). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
