-- Run this once after loading sample data (or any manual inserts with explicit IDs)
-- so that the next app insert gets max(id)+1 and does not hit "duplicate key" errors.
-- Safe to run on empty tables (sequence will be set to 1).

SELECT setval(pg_get_serial_sequence('city', 'city_id'), COALESCE((SELECT MAX(city_id) FROM city), 1));
SELECT setval(pg_get_serial_sequence('theatre', 'theatre_id'), COALESCE((SELECT MAX(theatre_id) FROM theatre), 1));
SELECT setval(pg_get_serial_sequence('hall', 'hall_id'), COALESCE((SELECT MAX(hall_id) FROM hall), 1));
SELECT setval(pg_get_serial_sequence('movie', 'movie_id'), COALESCE((SELECT MAX(movie_id) FROM movie), 1));
SELECT setval(pg_get_serial_sequence('seat', 'seat_id'), COALESCE((SELECT MAX(seat_id) FROM seat), 1));
SELECT setval(pg_get_serial_sequence('show', 'show_id'), COALESCE((SELECT MAX(show_id) FROM show), 1));
SELECT setval(pg_get_serial_sequence('show_seat', 'show_seat_id'), COALESCE((SELECT MAX(show_seat_id) FROM show_seat), 1));
SELECT setval(pg_get_serial_sequence('payment_transaction', 'transaction_id'), COALESCE((SELECT MAX(transaction_id) FROM payment_transaction), 1));
SELECT setval(pg_get_serial_sequence('ticket', 'ticket_id'), COALESCE((SELECT MAX(ticket_id) FROM ticket), 1));
SELECT setval(pg_get_serial_sequence('users', 'user_id'), COALESCE((SELECT MAX(user_id) FROM users), 1));
