USE imprint;

CREATE TABLE IF NOT EXISTS user_music (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    file_path       VARCHAR(500) NOT NULL,
    original_name   VARCHAR(255),
    file_size       BIGINT,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_music_user (user_id)
);
