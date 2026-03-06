-- Drop all application tables. Run when you want to remove the table structures entirely.
-- CASCADE handles dependent objects.

DROP TABLE IF EXISTS ticket               CASCADE;
DROP TABLE IF EXISTS payment_transaction  CASCADE;
DROP TABLE IF EXISTS show_seat            CASCADE;
DROP TABLE IF EXISTS show                 CASCADE;
DROP TABLE IF EXISTS seat                 CASCADE;
DROP TABLE IF EXISTS hall                 CASCADE;
DROP TABLE IF EXISTS theatre              CASCADE;
DROP TABLE IF EXISTS movie                CASCADE;
DROP TABLE IF EXISTS city                 CASCADE;
DROP TABLE IF EXISTS users                CASCADE;
