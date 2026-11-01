package hr.span.akademija.vulnshop.common;

/** Baca se kada zasticeni endpoint ne dobije valjan Bearer token (HTTP 401). */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
