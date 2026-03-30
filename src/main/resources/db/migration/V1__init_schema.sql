CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rooms (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100)   NOT NULL UNIQUE,
    location     VARCHAR(255),
    capacity     INTEGER        NOT NULL CHECK (capacity > 0),
    hourly_price NUMERIC(10, 2) NOT NULL CHECK (hourly_price >= 0),
    open_time    TIME           NOT NULL,
    close_time   TIME           NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    version             BIGINT      NOT NULL DEFAULT 0,
    user_id             BIGINT      NOT NULL,
    room_id             BIGINT      NOT NULL,
    start_time          TIMESTAMP   NOT NULL,
    end_time            TIMESTAMP   NOT NULL,
    status              VARCHAR(30) NOT NULL,
    notes               VARCHAR(500),
    cancelled_at        TIMESTAMP,
    cancellation_reason VARCHAR(255),
    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (id),

    CONSTRAINT fk_bookings_room
        FOREIGN KEY (room_id) REFERENCES rooms (id),

    CONSTRAINT chk_booking_time
        CHECK (end_time > start_time)
);

CREATE INDEX idx_bookings_room_id ON bookings (room_id);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_room_start_end ON bookings (room_id, start_time, end_time);