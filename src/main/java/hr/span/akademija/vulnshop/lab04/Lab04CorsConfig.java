package hr.span.akademija.vulnshop.lab04;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lab04 – POPRAVLJENA CORS konfiguracija.
 *
 * POPRAVAK: dopusteni su samo izvori iz konfiguracijske allowliste
 * ({@code app.cors.allowed-origins}), a ne bilo koji origin. Time zlonamjerne
 * stranice vise ne mogu slati autentificirane zahtjeve i citati odgovore.
 */
@Configuration
public class Lab04CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public Lab04CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split("\\s*,\\s*");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/lab04/**")
                .allowedOrigins(allowedOrigins) // POPRAVAK: fiksna allowlista
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Authorization", "Content-Type")
                .allowCredentials(true);
    }
}
