# Postman – Movie Ticket Booking API

## Import

1. Open Postman.
2. **Import collection**: File → Import → choose `Movie-Ticket-Booking-API.postman_collection.json`.
3. **Import environment (optional)**: File → Import → choose `Local.postman_environment.json`, then select **Local** in the environment dropdown (top right).

## Variables

- **baseUrl**: API base URL (default: `http://localhost:8081`). Set in the collection or in the **Local** environment.
- **token**: JWT from login. Set automatically when you run **Auth → Login** (saved to both collection and environment variables so it works with or without an environment selected).

**If token is not updating after Login:** Re-import the collection (the Login request has a script that saves the token). Ensure the Login response is 200 and the body contains `"token": "..."`. If you use the **Local** environment, leave the **token** variable there (initial value empty); the script will overwrite it after Login.

## Usage

1. Start the backend (e.g. `./mvnw spring-boot:run`).
2. **Auth → Login** with an account that has the role you need:
   - For **Admin** requests (add city, theatre, hall, movie, seats, show): log in with a user that has role **ADMIN** (e.g. username `shadan` after running the SQL to add the admin user). The token is stored automatically.
   - For **User** requests (cities, movies, shows, seats, lock, payment session, ticket): any authenticated user (USER or ADMIN) can call these.
3. Run **Admin (JWT with ADMIN role required)** requests to create cities, theatres, halls, movies, seats, shows. They require the same `{{token}}` from Login; the user must have role ADMIN or you get 403.
4. Run **User (JWT required)** requests for listing cities/movies/shows, seats, lock, payment session, ticket.

Replace path/query placeholders (e.g. `1` for city id, show id, seat id) with real ids from previous responses.
