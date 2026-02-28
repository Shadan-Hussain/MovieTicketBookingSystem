# Movie Ticket Booking – Frontend

React (Vite) frontend for the Movie Ticket Booking backend.

## Setup

```bash
npm install
```

Create `.env` in the project root (optional):

```
VITE_API_URL=http://localhost:8081
```

Default API URL is `http://localhost:8081` if not set.

## Run

```bash
npm run dev
```

Open http://localhost:5173.

## Payment redirects (Stripe)

After payment, Stripe redirects the user to your **backend** URLs. To show the frontend success/cancel pages instead, configure the backend so that Stripe redirects to the frontend:

- Success: `http://localhost:5173/payment/success?session_id={CHECKOUT_SESSION_ID}`
- Cancel: `http://localhost:5173/payment/cancel`

Set in the backend (e.g. `application.properties` or env):

- `stripe.successUrl=http://localhost:5173/payment/success?session_id={CHECKOUT_SESSION_ID}`
- `stripe.cancelUrl=http://localhost:5173/payment/cancel`

(Use your real frontend URL in production.)

## Flows

- **User:** Sign up → Login → Choose city → Movies → Shows → Select one seat → Proceed to payment (Stripe) → Success page (polls for ticket) or Cancel page.
- **Admin:** Login with an ADMIN user → “Manage database” in nav → Add city, theatre, hall, movie, seats, show via forms. Invalid entries show an alert at the top; otherwise “Insert successful”.
