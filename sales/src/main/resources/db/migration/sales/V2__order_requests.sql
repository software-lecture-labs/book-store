-- Idempotency-Key of POST /api/v1/orders (ADR-0022): which order each key of a customer led to.
-- The row is written in the order's transaction before the payment; the primary key makes a
-- concurrent repeat wait for the first request and then fail, after which it finds the order.
-- No foreign key to orders: the order row is written later in the same transaction.
CREATE TABLE order_requests (
    user_id      BIGINT        NOT NULL,
    request_key  VARCHAR(100)  NOT NULL,
    fingerprint  VARCHAR(100)  NOT NULL,
    order_number CHAR(14)      NOT NULL,
    received_at  TIMESTAMP(6)  NOT NULL,
    CONSTRAINT pk_order_requests PRIMARY KEY (user_id, request_key)
);
