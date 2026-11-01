package hr.span.akademija.vulnshop.lab04;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lab04 – RANJIVA CORS konfiguracija.
 *
 * RANJIVO: dopusta BILO KOJI origin ({@code allowedOriginPatterns("*")}) UZ
 * {@code allowCredentials(true)}. Time bilo koja zlonamjerna stranica moze slati
 * autentificirane zahtjeve i citati odgovore (CWE-942). Takoder, nigdje se ne
 * postavljaju sigurnosna zaglavlja (X-Content-Type-Options, X-Frame-Options, CSP).
 */
@Configuration
public class Lab04CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // RANJIVO: refleksija bilo kojeg origina uz dopustene kolacice/credentiale.
        registry.addMapping("/api/lab04/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
