package hr.span.akademija.vulnshop.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Oznaka za parametar kontrolera tipa {@link CurrentUser}.
 * Po defaultu se zahtijeva prijava; {@code optional = true} dopusta anonimni pristup
 * (tada parametar moze biti {@code null}).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {
    boolean optional() default false;
}
