# Test Summary – Movie Ticket Booking Backend

This document explains what tests we have, what they test, and how everything is set up.

---

## 1. Unit tests vs integration tests

| Type | What it means | What we have |
|------|----------------|--------------|
| **Unit test** | Tests one class or method in isolation. Dependencies (DB, Redis, other services) are usually **mocked**. Fast, no real server. | We do **not** have separate unit tests in this project. |
| **Integration test** | Tests the app as a whole: real HTTP requests, real (or in-memory) database, multiple layers (controller → service → repository) working together. | **Yes.** All our tests are **integration tests**. |

Our tests are **integration tests**: they start the full Spring Boot application (with test config), send HTTP requests to the API, and assert on the responses. They verify that the **entire flow** works (controllers, services, repositories, validation).

---

## 2. How the tests are configured

### 2.1 Test class setup

- **`@SpringBootTest`** – Starts the full Spring application context (all beans, like in production, but with test config).
- **`@ActiveProfiles("test")`** – Activates the `test` profile so `application-test.properties` is used.
- **`@Import(TestRedisConfig.class)`** – Adds our test “Redis” and mock Stripe into the context.
- **MockMvc** – We build it in `@BeforeEach` with `MockMvcBuilders.webAppContextSetup(webApplicationContext).build()`. This lets us call the API **without** starting a real HTTP server (faster, no port binding).
- **`@MockitoBean`** – Replaces the real `StripeService` with a mock so we never call the real Stripe API. We stub `createCheckoutSession` to return a fake session ID and URL.

### 2.2 Test profile: `application-test.properties`

Used only when running tests (`test` profile).

| Setting | Purpose |
|--------|---------|
| **H2 in-memory database** | Replaces PostgreSQL. Same SQL, but runs in memory and is recreated per test run. No need for a real DB. |
| **Exclude Redis auto-config** | We don’t connect to real Redis. Instead, `TestRedisConfig` provides a fake “Redis” (in-memory map) so seat locking still works. |
| **Dummy Stripe keys** | App starts without real Stripe config; payment session creation is handled by the mocked `StripeService`. |

### 2.3 TestRedisConfig (fake Redis)

Because we excluded real Redis:

- We define a **test-only** `RedisTemplate` bean that behaves like Redis for our use case.
- Under the hood it uses a **ConcurrentHashMap**: “set key” = put in map, “key exists?” = map contains key.
- So **seat lock** and **payment-session** flows still work: lock stores the key, and “is locked?” checks that key.

---

## 3. What runs before each test: seed data

In `@BeforeEach` we:

1. Build MockMvc.
2. Stub the Stripe mock: `createCheckoutSession(any, any)` returns a fake session.
3. Call **seedData()**, which creates one full “world” via the API:
   - 1 city (e.g. Mumbai)
   - 1 theatre (e.g. PVR Andheri)
   - 1 hall (e.g. Screen 1)
   - 6 seats (2 rows × 3 cols) in that hall
   - 1 movie (e.g. Inception, 148 min)
   - 1 show (that movie in that hall at a future time)

So every test starts with the same data: one city, one theatre, one hall, one movie, one show, and known `cityId`, `theatreId`, `hallId`, `movieId`, `showId`, and one `seatId`.

---

## 4. What each test does (by endpoint / area)

### GET /cities

| Test | What it checks |
|------|-----------------|
| **listCities** | GET `/cities` returns 200, a JSON array, and that array contains the seeded city (e.g. Mumbai) with the expected `cityId`. |

---

### GET /movies

| Test | What it checks |
|------|-----------------|
| **getMoviesByCity** | GET `/movies?city_id=...` (our seeded city) returns 200 and a list that includes the seeded movie (e.g. Inception) with the expected `movieId`. |
| **getMoviesByCityEmpty** | For a city that has no theatres/shows (e.g. newly created Pune), GET `/movies?city_id=...` returns 200 and an **empty** array. |

---

### GET /shows

| Test | What it checks |
|------|-----------------|
| **getShowsByCityAndMovie** | GET `/shows?city_id=...&movie_id=...` returns 200 and a list that includes our seeded show (correct `showId`). |

---

### GET /shows/{showId}/seats

| Test | What it checks |
|------|-----------------|
| **getSeatsForShow** | GET `/shows/{showId}/seats` returns 200, an array of seats, and the first seat has `status` "AVAILABLE" and a `seatId`. |

---

### POST .../lock (seat lock)

| Test | What it checks |
|------|-----------------|
| **lockSeat** | POST `/shows/{showId}/seats/{seatId}/lock` for our seeded show/seat returns **200**. The fake Redis stores the lock. |
| **lockSeatNotFound** | POST lock for non-existent show/seat (e.g. 99999/99999) returns **404**. |

---

### POST .../payment-session (Stripe checkout)

| Test | What it checks |
|------|-----------------|
| **paymentSessionWithoutLock** | POST payment-session **without** locking first returns **410 Gone** (“lock expired; please lock again”). |
| **paymentSessionWithLock** | After locking the seat, POST payment-session returns **200**, and the JSON has `sessionId` "cs_test_123" and a `url` (from our Stripe mock). |

---

### GET /tickets

| Test | What it checks |
|------|-----------------|
| **getTicketNotFound** | GET `/tickets?show_id=...&seat_id=...` for a show/seat that has **no** successful payment returns **404**. (We don’t simulate webhook/payment success in these tests.) |

---

### Admin validation (400 Bad Request)

These check that invalid admin payloads are rejected.

| Test | What it checks |
|------|-----------------|
| **addCityBlankName** | POST `/admin/cities` with `name: "   "` → **400**. |
| **addCityNullName** | POST `/admin/cities` with `{}` (no name) → **400**. |
| **addTheatreMissingCityId** | POST `/admin/theatres` with only `name`, no `cityId` → **400**. |
| **addSeatsInvalidRows** | POST `/admin/halls/{id}/seats` with `rows: 0` → **400**. |
| **addShowMissingFields** | POST `/admin/shows` with `{}` (no movieId, hallId, startTime, endTime) → **400**. |

---

## 5. Summary table

| Category | # tests | What’s tested |
|----------|--------|----------------|
| Cities API | 1 | List cities returns seeded city. |
| Movies API | 2 | Movies by city; empty list when no shows. |
| Shows API | 1 | Shows by city and movie. |
| Show seats | 1 | Seats for a show, with status. |
| Lock seat | 2 | Lock succeeds; 404 for bad show/seat. |
| Payment session | 2 | 410 without lock; 200 + session when locked. |
| Tickets | 1 | 404 when no payment. |
| Admin validation | 5 | Bad or missing body → 400. |
| **Total** | **15** | Integration tests over HTTP with test DB and mocks. |

---

## 6. How to run the tests

From the project root (e.g. `MovieTicketBookingSystemBackend`):

```bash
./mvnw test
```

To run only this integration test class:

```bash
./mvnw test -Dtest=BackendIntegrationTest
```

---

## 7. Dependencies used for testing (from pom.xml)

- **spring-boot-starter-test** – JUnit 5, Mockito, AssertJ, Spring Test, MockMvc.
- **spring-boot-starter-webmvc-test** – Extra web/test support.
- **H2** – In-memory database (test scope).
- **jackson-databind** + **jackson-datatype-jsr310** – JSON and Java 8 date/time (e.g. `OffsetDateTime`) in tests.

This summary reflects the tests we implemented: they are **integration tests** that hit the real API with a test DB and mocked Redis and Stripe.
