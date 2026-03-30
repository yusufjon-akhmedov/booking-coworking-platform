INSERT INTO users (full_name, email, password, role, enabled)
VALUES
    ('Admin User', 'admin@coworking.uz', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi4Q8fK2rQ0J7sKnz03WxZmf0RduCMW', 'ADMIN', TRUE),
    ('Customer User', 'customer@coworking.uz', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi4Q8fK2rQ0J7sKnz03WxZmf0RduCMW', 'CUSTOMER', TRUE);

INSERT INTO rooms (name, location, capacity, hourly_price, open_time, close_time, active)
VALUES
    ('Ocean Room', '1st Floor', 6, 25.00, '08:00:00', '22:00:00', TRUE),
    ('Focus Room', '2nd Floor', 4, 18.00, '09:00:00', '20:00:00', TRUE);