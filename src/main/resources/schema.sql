-- Shema baze za VulnShop Akademija (H2 in-memory).
-- Svaka vjezba ima vlastite tablice s prefiksom labXX_ radi izolacije.

-- Zajednicka tablica korisnika (autentikacija). Lozinke: BCrypt (vidi SeedData).
CREATE TABLE IF NOT EXISTS users (
    id       BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role     VARCHAR(20)  NOT NULL
);

-- Lab01 – IDOR: narudzbe.
CREATE TABLE IF NOT EXISTS lab01_orders (
    id               BIGINT PRIMARY KEY,
    owner_username   VARCHAR(100)  NOT NULL,
    items            VARCHAR(500)  NOT NULL,
    shipping_address VARCHAR(500)  NOT NULL,
    total            DECIMAL(10,2) NOT NULL
);

-- Lab02 – Mass assignment: profili.
CREATE TABLE IF NOT EXISTS lab02_profiles (
    user_id      BIGINT PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    email        VARCHAR(200) NOT NULL,
    role         VARCHAR(20)  NOT NULL
);

-- Lab06 – Kriptografski propusti, dio A (kupci) i dio B (kartice).
CREATE TABLE IF NOT EXISTS lab06_customers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL
);
CREATE TABLE IF NOT EXISTS lab06_cards (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner      VARCHAR(100)  NOT NULL,
    ciphertext VARCHAR(1000) NOT NULL,
    last4      VARCHAR(4)    NOT NULL
);

-- Lab07 – SQL injection: proizvodi i racuni (lozinke namjerno u cistom tekstu).
CREATE TABLE IF NOT EXISTS lab07_products (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(500)  NOT NULL,
    price       DECIMAL(10,2) NOT NULL,
    hidden      BOOLEAN       NOT NULL
);
CREATE TABLE IF NOT EXISTS lab07_accounts (
    id       BIGINT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL
);

-- Lab08 – XSS: pohranjene recenzije.
CREATE TABLE IF NOT EXISTS lab08_reviews (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT       NOT NULL,
    author     VARCHAR(200) NOT NULL,
    text       VARCHAR(2000) NOT NULL
);

-- Lab10 – Poslovna logika: novcanici, proizvodi, iskoristeni kuponi.
CREATE TABLE IF NOT EXISTS lab10_wallets (
    username VARCHAR(100) PRIMARY KEY,
    balance  DECIMAL(12,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS lab10_products (
    id    BIGINT PRIMARY KEY,
    name  VARCHAR(200)  NOT NULL,
    price DECIMAL(10,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS lab10_coupon_usage (
    username VARCHAR(100) NOT NULL,
    coupon   VARCHAR(100) NOT NULL
);

-- Lab11 – Autentikacijski propusti: brute force / enumeracija.
CREATE TABLE IF NOT EXISTS lab11_users (
    id            BIGINT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL
);

-- Lab15 – Rukovanje iznimkama: bankovni racuni.
CREATE TABLE IF NOT EXISTS lab15_accounts (
    account_no     VARCHAR(50) PRIMARY KEY,
    owner_username VARCHAR(100)  NOT NULL,
    balance        DECIMAL(14,2) NOT NULL
);
