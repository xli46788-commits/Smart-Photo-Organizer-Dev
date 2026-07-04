USE imprint;

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
