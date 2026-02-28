# Backend APIs vs Frontend Usage

All backend REST APIs and whether the frontend uses them.

| Backend API | Method | Frontend usage |
|-------------|--------|----------------|
| `/auth/signup` | POST | Yes – `signup()` |
| `/auth/login` | POST | Yes – `login()` |
| `/cities` | GET | Yes – `getCities()` |
| `/movies?city_id=` | GET | Yes – `getMoviesByCity()` |
| `/shows?city_id=&movie_id=` | GET | Yes – `getShows()` |
| `/shows/{showId}/seats` | GET | Yes – `getSeatsForShow()` |
| `/shows/{showId}/seats/{seatId}/lock` | POST | Yes – `lockSeat()` (on Proceed to payment) |
| `/shows/{showId}/seats/{seatId}/lock` | DELETE | Yes – `unlockSeat()` in api.js (e.g. for cleanup; not used on seat click) |
| `/shows/{showId}/seats/{seatId}/payment-session` | POST | Yes – `getPaymentSession()` |
| `/tickets?show_id=&seat_id=` | GET | Yes – `getTicket()` (single ticket after payment) |
| `/tickets` (no params) | GET | Yes – `getMyTickets()` (My Tickets page) |
| `/transactions` | GET | Yes – `getMyTransactions()` (Transaction history) |
| `/admin/options/theatres` | GET | Yes – `getAdminTheatres()` |
| `/admin/options/halls` | GET | Yes – `getAdminHalls()` |
| `/admin/options/movies` | GET | Yes – `getAdminMovies()` |
| `/admin/cities` | POST | Yes – `adminAddCity()` |
| `/admin/theatres` | POST | Yes – `adminAddTheatre()` |
| `/admin/halls` | POST | Yes – `adminAddHall()` |
| `/admin/movies` | POST | Yes – `adminAddMovie()` |
| `/admin/halls/{hallId}/seats` | POST | Yes – `adminAddSeats()` (after add hall) |
| `/admin/shows` | POST | Yes – `adminAddShow()` |
| `/webhook/stripe` | POST | No – called by Stripe only |
| `/redirect/success` | GET | No – Stripe redirects to frontend `/payment/success?...` |
| `/redirect/cancel` | GET | No – Stripe redirects to frontend `/payment/cancel` |

**Summary:** Every user- or admin-facing API is used by the frontend. `/webhook/stripe` is backend-only. `/redirect/success` and `/redirect/cancel` are not used because Stripe is configured to redirect to the frontend URLs.
