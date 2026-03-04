-- Add poster image storage (BYTEA) and content type for Option 1: store image in PostgreSQL.
-- Run this if you are not using spring.jpa.hibernate.ddl-auto=update.

ALTER TABLE movie ADD COLUMN IF NOT EXISTS poster_image BYTEA;
ALTER TABLE movie ADD COLUMN IF NOT EXISTS poster_content_type VARCHAR(128);
