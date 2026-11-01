package hr.span.akademija.vulnshop.common;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Puni bazu pocetnim podacima pri startu aplikacije (i pri svakom pokretanju
 * testa, jer je baza in-memory i svjeza po instanci).
 *
 * Hashevi (BCrypt za {@code users} i {@code lab11_users}, MD5 za {@code lab06_customers})
 * racunaju se u Javi jer ih nije moguce zapisati u {@code data.sql}.
 * DDL je u {@code schema.sql}.
 */
@Component
@Order(1)
public class SeedData implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public SeedData(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedLab01();
        seedLab02();
        seedLab06();
        seedLab07();
        seedLab10();
        seedLab11();
        seedLab15();
    }

    private void seedUsers() {
        jdbc.update("DELETE FROM users");
        insertUser(1, "ana", "Ana123!", "USER");
        insertUser(2, "marko", "Marko123!", "USER");
        insertUser(3, "admin", "Admin123!", "ADMIN");
    }

    private void insertUser(long id, String username, String rawPassword, String role) {
        jdbc.update("INSERT INTO users (id, username, password, role) VALUES (?, ?, ?, ?)",
                id, username, encoder.encode(rawPassword), role);
    }

    private void seedLab01() {
        jdbc.update("DELETE FROM lab01_orders");
        jdbc.update("INSERT INTO lab01_orders (id, owner_username, items, shipping_address, total) VALUES (?,?,?,?,?)",
                1001L, "ana", "Laptop, Mis", "Ilica 1, Zagreb", new BigDecimal("999.00"));
        jdbc.update("INSERT INTO lab01_orders (id, owner_username, items, shipping_address, total) VALUES (?,?,?,?,?)",
                1002L, "ana", "Tipkovnica", "Ilica 1, Zagreb", new BigDecimal("49.00"));
        jdbc.update("INSERT INTO lab01_orders (id, owner_username, items, shipping_address, total) VALUES (?,?,?,?,?)",
                1003L, "marko", "Monitor", "Savska 10, Zagreb", new BigDecimal("299.00"));
        jdbc.update("INSERT INTO lab01_orders (id, owner_username, items, shipping_address, total) VALUES (?,?,?,?,?)",
                1004L, "admin", "Server", "Koturaska 47, Zagreb", new BigDecimal("4999.00"));
    }

    private void seedLab02() {
        jdbc.update("DELETE FROM lab02_profiles");
        jdbc.update("INSERT INTO lab02_profiles (user_id, display_name, email, role) VALUES (?,?,?,?)",
                1L, "Ana Anic", "ana@akademija.hr", "USER");
        jdbc.update("INSERT INTO lab02_profiles (user_id, display_name, email, role) VALUES (?,?,?,?)",
                2L, "Marko Maric", "marko@akademija.hr", "USER");
        jdbc.update("INSERT INTO lab02_profiles (user_id, display_name, email, role) VALUES (?,?,?,?)",
                3L, "Administrator", "admin@akademija.hr", "ADMIN");
    }

    private void seedLab06() {
        jdbc.update("DELETE FROM lab06_customers");
        jdbc.update("DELETE FROM lab06_cards");
        // Namjerno nesigurno: MD5(hex, bez soli). maja i ivan imaju istu lozinku -> isti hash.
        insertLab06Customer("ivan", md5Hex("123456"));
        insertLab06Customer("petra", md5Hex("password"));
        insertLab06Customer("luka", md5Hex("qwerty"));
        insertLab06Customer("maja", md5Hex("123456"));
    }

    private void insertLab06Customer(String username, String passwordHash) {
        jdbc.update("INSERT INTO lab06_customers (username, password_hash) VALUES (?, ?)",
                username, passwordHash);
    }

    private void seedLab07() {
        jdbc.update("DELETE FROM lab07_products");
        jdbc.update("DELETE FROM lab07_accounts");
        insertLab07Product(1, "Laptop", "Prijenosno racunalo", new BigDecimal("899.00"), false);
        insertLab07Product(2, "Mis", "Opticki mis", new BigDecimal("19.00"), false);
        insertLab07Product(3, "Tipkovnica", "Mehanicka tipkovnica", new BigDecimal("79.00"), false);
        insertLab07Product(4, "Monitor", "27-inchni monitor", new BigDecimal("299.00"), false);
        insertLab07Product(5, "Interni prototip X", "Tajni interni proizvod", new BigDecimal("0.00"), true);
        // Racuni s lozinkama u cistom tekstu (namjerno, radi UNION napada).
        jdbc.update("INSERT INTO lab07_accounts (id, username, password) VALUES (?,?,?)", 1L, "admin", "SuperTajna2026");
        jdbc.update("INSERT INTO lab07_accounts (id, username, password) VALUES (?,?,?)", 2L, "webshop", "Shop!2026");
    }

    private void insertLab07Product(long id, String name, String desc, BigDecimal price, boolean hidden) {
        jdbc.update("INSERT INTO lab07_products (id, name, description, price, hidden) VALUES (?,?,?,?,?)",
                id, name, desc, price, hidden);
    }

    private void seedLab10() {
        jdbc.update("DELETE FROM lab10_wallets");
        jdbc.update("DELETE FROM lab10_products");
        jdbc.update("DELETE FROM lab10_coupon_usage");
        for (String u : new String[]{"ana", "marko", "admin"}) {
            jdbc.update("INSERT INTO lab10_wallets (username, balance) VALUES (?, ?)", u, new BigDecimal("1000.00"));
        }
        jdbc.update("INSERT INTO lab10_products (id, name, price) VALUES (?,?,?)", 1L, "Laptop", new BigDecimal("899.00"));
        jdbc.update("INSERT INTO lab10_products (id, name, price) VALUES (?,?,?)", 2L, "Slusalice", new BigDecimal("99.00"));
        jdbc.update("INSERT INTO lab10_products (id, name, price) VALUES (?,?,?)", 3L, "Kabel", new BigDecimal("9.99"));
    }

    private void seedLab11() {
        jdbc.update("DELETE FROM lab11_users");
        // Lozinka sunshine1 nalazi se u wordlists/top-100.txt (brute force demo).
        insertLab11User(1, "ivana", "sunshine1");
        insertLab11User(2, "tomislav", "T0m!slav#2026");
        // Zaseban korisnik za test zakljucavanja (lockout).
        insertLab11User(3, "lockme", "Lock!Me2026");
    }

    private void insertLab11User(long id, String username, String rawPassword) {
        jdbc.update("INSERT INTO lab11_users (id, username, password_hash) VALUES (?,?,?)",
                id, username, encoder.encode(rawPassword));
    }

    private void seedLab15() {
        jdbc.update("DELETE FROM lab15_accounts");
        jdbc.update("INSERT INTO lab15_accounts (account_no, owner_username, balance) VALUES (?,?,?)",
                "HR-ANA-001", "ana", new BigDecimal("5000.00"));
        jdbc.update("INSERT INTO lab15_accounts (account_no, owner_username, balance) VALUES (?,?,?)",
                "HR-MARKO-001", "marko", new BigDecimal("1000.00"));
        jdbc.update("INSERT INTO lab15_accounts (account_no, owner_username, balance) VALUES (?,?,?)",
                "HR-ADMIN-001", "admin", new BigDecimal("10000.00"));
    }

    /** MD5 heks zapis (namjerno nesiguran, koristi se u lab06 dio A). */
    public static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 nije dostupan", e);
        }
    }
}
