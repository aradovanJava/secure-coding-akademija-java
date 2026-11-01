package hr.span.akademija.vulnshop.common;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Omogucuje kontrolerima da deklariraju parametar tipa {@link CurrentUser}.
 *
 * Ako je parametar oznacen s {@link RequireAuth} (ili nije oznacen s
 * {@code @RequireAuth(optional = true)}), a korisnik nije prijavljen, baca se
 * {@link UnauthorizedException} (HTTP 401). Time se jednolicno rjesava zahtjev
 * "bez tokena = 401" na svim zasticenim endpointima.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        CurrentUser user = (CurrentUser) webRequest.getAttribute(
                AuthFilter.CURRENT_USER_ATTR, RequestAttributes.SCOPE_REQUEST);

        RequireAuth annotation = parameter.getParameterAnnotation(RequireAuth.class);
        boolean optional = annotation != null && annotation.optional();

        if (user == null && !optional) {
            throw new UnauthorizedException("Potrebna je prijava (nedostaje ili je neispravan Bearer token).");
        }
        return user;
    }
}
