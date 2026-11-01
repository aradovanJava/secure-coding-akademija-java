package hr.span.akademija.vulnshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ulazna tocka aplikacije VulnShop Akademija.
 *
 * UPOZORENJE: ovo je NAMJERNO RANJIVA aplikacija za vjezbe sigurnog programiranja.
 * Pokrecite je iskljucivo lokalno i nikada je ne izlazite na javnu mrezu.
 */
@SpringBootApplication
public class VulnShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(VulnShopApplication.class, args);
    }
}
