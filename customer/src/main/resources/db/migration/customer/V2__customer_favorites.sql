CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    added_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, book_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_favorites_user_added ON favorites (user_id, added_at DESC);
