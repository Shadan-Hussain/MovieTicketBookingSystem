-- Sample data for movie ticket booking (PostgreSQL). Run once on empty schema.
-- Order: city -> theatre -> hall -> movie -> seat -> show -> show_seat -> users
-- payment_transaction and ticket are created by the app (auto-generated IDs).

INSERT INTO city (city_id, name, state_code, created_at) VALUES
  (1, 'Mumbai', 'MH', NOW()),
  (2, 'Delhi', 'DL', NOW());

INSERT INTO theatre (theatre_id, city_id, name, address, created_at) VALUES
  (1, 1, 'PVR Phoenix', '462, Senapati Bapat Marg, Lower Parel', NOW()),
  (2, 1, 'INOX R City', 'LBS Marg, Ghatkopar West', NOW());

INSERT INTO hall (hall_id, theatre_id, name, capacity, created_at) VALUES
  (1, 1, 'Screen 1', 100, NOW()),
  (2, 1, 'Screen 2', 80, NOW());

INSERT INTO movie (movie_id, name, duration_mins, description, poster_url, language, release_date, created_at) VALUES
  (1, 'Sample Movie One', 120, 'A sample film for testing.', 'https://example.com/poster1.jpg', 'Hindi', '2025-01-15', NOW()),
  (2, 'Sample Movie Two', 105, 'Another sample film.', NULL, 'English', '2025-02-01', NOW());

INSERT INTO seat (seat_id, hall_id, number, price, type, created_at) VALUES
  (1, 1, 'A1', 200, 'NORMAL', NOW()),
  (2, 1, 'A2', 200, 'NORMAL', NOW()),
  (3, 1, 'B1', 350, 'PREMIUM', NOW()),
  (4, 1, 'B2', 350, 'PREMIUM', NOW()),
  (5, 2, 'A1', 180, 'NORMAL', NOW()),
  (6, 2, 'A2', 180, 'NORMAL', NOW());

INSERT INTO show (show_id, movie_id, hall_id, start_time, end_time, created_at) VALUES
  (1, 1, 1, '2025-03-01 14:00:00+05:30', '2025-03-01 16:00:00+05:30', NOW()),
  (2, 1, 1, '2025-03-01 18:00:00+05:30', '2025-03-01 20:00:00+05:30', NOW()),
  (3, 2, 2, '2025-03-02 12:00:00+05:30', '2025-03-02 13:45:00+05:30', NOW());

INSERT INTO show_seat (show_seat_id, show_id, seat_id, status, created_at) VALUES
  (1, 1, 1, 'AVAILABLE', NOW()),
  (2, 1, 2, 'AVAILABLE', NOW()),
  (3, 1, 3, 'AVAILABLE', NOW()),
  (4, 1, 4, 'AVAILABLE', NOW()),
  (5, 2, 1, 'AVAILABLE', NOW()),
  (6, 2, 2, 'AVAILABLE', NOW()),
  (7, 2, 3, 'AVAILABLE', NOW()),
  (8, 2, 4, 'AVAILABLE', NOW()),
  (9, 3, 5, 'AVAILABLE', NOW()),
  (10, 3, 6, 'AVAILABLE', NOW());

INSERT INTO users (user_id, email, name, created_at) VALUES
  (1, 'user@example.com', 'Sample User', NOW());
