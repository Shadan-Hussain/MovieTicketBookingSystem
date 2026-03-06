-- Clear all application data and reset identity sequences.
-- Run when you want a clean slate; inserts will then use DB-generated IDs.
-- Order: child tables first so foreign keys are satisfied.

TRUNCATE TABLE ticket,
             payment_transaction,
             show_seat,
             show,
             seat,
             hall,
             theatre,
             movie,
             city,
             users
    RESTART IDENTITY
    CASCADE;
