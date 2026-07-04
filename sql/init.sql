CREATE DATABASE IF NOT EXISTS imprint DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE imprint;

CREATE TABLE IF NOT EXISTS user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(20)  NOT NULL,
    keywords    VARCHAR(500),
    built_in    TINYINT(1)   DEFAULT 0,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_category (user_id, name),
    INDEX idx_category_user (user_id)
);

CREATE TABLE IF NOT EXISTS album (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    category    VARCHAR(20),
    cover_url   VARCHAR(255),
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_album_user (user_id)
);

CREATE TABLE IF NOT EXISTS photo (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    album_id        BIGINT,
    file_path       VARCHAR(500) NOT NULL,
    thumb_path      VARCHAR(500),
    original_name   VARCHAR(255),
    ai_category     VARCHAR(20),
    ai_confidence   DECIMAL(5,4),
    taken_at        DATETIME,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_photo_user (user_id),
    INDEX idx_photo_category (ai_category)
);

CREATE TABLE IF NOT EXISTS user_music (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    original_name   VARCHAR(255),
    file_size       BIGINT,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_music_user (user_id)
);
