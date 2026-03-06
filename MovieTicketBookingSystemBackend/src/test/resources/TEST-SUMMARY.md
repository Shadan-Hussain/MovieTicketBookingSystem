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

### GET /movies/{movieId} and poster

| Test | What it checks |
|------|-----------------|
| **getMovieReturns200** | GET `/movies/{movieId}` (auth) returns 200 with movie details and `hasPoster` false. |
| **getMovieReturns404WhenNotFound** | GET `/movies/99999` returns 404. |
| **getPosterReturns404WhenNoPoster** | GET `/movies/{movieId}/poster` (no auth) returns 404 when movie has no stored poster. |
| **uploadPosterThenGetPosterReturns200** | POST `/admin/movies/{movieId}/poster` with valid JPEG then GET poster returns 200 and same bytes. |
| **uploadPosterEmptyFileReturns400** | POST poster with empty file returns 400. |
| **uploadPosterWrongContentTypeReturns400** | POST poster with `text/plain` returns 400. |
| **uploadPosterTooLargeReturns400** | POST poster with file &gt; 2 MB returns 400. |

---

### GET /shows

| Test | What it checks |
|------|-----------------|
| **getShowsByCityAndMovie** | GET `/shows?city_id=...&movie_id=...` returns 200 and a list that includes our seeded show (correct `showId`). |
| **getShowsEmptyForNonExistent** | Returns empty list for non-existent city or movie. |

---

### GET /shows/{showId}

| Test | What it checks |
|------|-----------------|
| **getShowByIdReturns200WithDetails** | GET `/shows/{showId}` returns 200 with showId, movieName, theatreName, hallName, startTime, endTime. |
| **getShowByIdReturns404WhenNotFound** | GET `/shows/99999` returns 404. |
| **getShowByIdReturns400WhenShowAlreadyStarted** | For a show with start time in the past, returns 400 with message "Show ended or already started". |

---

### GET /shows/{showId}/seats

| Test | What it checks |
|------|-----------------|
| **getSeatsForShow** | GET `/shows/{showId}/seats` returns 200, an array of seats, and the first seat has `status` "AVAILABLE" and a `seatId`. |
| **getSeatsForShowReflectsLockedStatus** | After locking a seat, GET seats returns that seat with status "LOCKED". |
| **getSeatsForShowReturns400WhenShowAlreadyStarted** | For a show with start time in the past, returns 400 with message "Show ended or already started". |

---

### POST .../lock (seat lock)

| Test | What it checks |
|------|-----------------|
| **lockSeat** | POST `/shows/{showId}/seats/{seatId}/lock` for our seeded show/seat returns **200**. The fake Redis stores the lock. |
| **lockSeatUnauthorized** | POST lock without JWT returns 401. |
| **lockSeatNotFound** | POST lock for non-existent show/seat (e.g. 99999/99999) returns **404**. |
| **lockSeatAlreadyLocked** | POST lock again for same seat returns **409** (Seat not available at the moment). |

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
| **getTicketNotFound** | GET `/tickets?show_id=...&seat_id=...` for a show/seat that has **no** successful payment returns **404**. |
| **getTicketReturns400WhenTransactionFailed** | When the user has a transaction for this show+seat with status FAILED, returns 400 with message "payment failed". |
| **getTicketReturns400WhenTransactionRefundInitiated** | When the user has a transaction for this show+seat with status REFUND_INITIATED, returns 400 with message "Ticket creation failed, refund initiated". |
| **getTicketUnauthorized** | GET ticket without JWT returns 401. |
| **getMyTicketsList** | GET `/tickets` (no params) returns list of current user's tickets. |
| **getTicketReturns404WhenTicketBelongsToAnotherUser** | GET ticket for show+seat where another user has the ticket returns 404. |

---

### Admin validation (400 Bad Request)

These check that invalid admin payloads are rejected.

| Test | What it checks |
|------|-----------------|
| **addCityBlankName** | POST `/admin/cities` with `name: "   "` → **400**. |
| **addCityNullName** | POST `/admin/cities` with `{}` (no name) → **400**. |
| **addTheatreMissingCityId** | POST `/admin/theatres` with only `name`, no `cityId` → **400**. |
| **addHallInvalidRows** | POST `/admin/halls` with `rows: 0` (and other required fields) → **400**. |
| **addShowMissingFields** | POST `/admin/shows` with `{}` (no movieId, hallId, startTime, endTime) → **400**. |
| **addShowWithStartTimeInPastReturns400** | POST `/admin/shows` with startTime in the past → **400** "Show start time must be in the future". |

---

## 5. Summary table

| Category | # tests | What’s tested |
|----------|--------|----------------|
| Cities API | 1 | List cities returns seeded city. |
| Movies API | 2 | Movies by city; empty list when no shows. |
| Movie by ID and poster | 7 | GET movie by id; GET poster (404/200); upload poster (success, empty file, wrong type, too large). |
| Shows API | 2 | Shows by city and movie; empty for non-existent. |
| Show by ID | 3 | GET show by id (movieName, etc.); 404; 400 when show already started. |
| Show seats | 3 | Seats for show; locked status; 400 when show already started. |
| Lock seat | 4 | Lock succeeds; 401; 404; 409 when already locked. |
| Payment session | 3 | 403 without lock; 200 when locked; 403 when other user's lock. |
| Tickets | 6 | 404 when no ticket; 400 FAILED/REFUND_INITIATED; 401; list; 404 other user. |
| Admin validation | 24+ | Bad body, duplicates, show overlap, past start time, etc. |
| **Total** | **~70** | Integration tests over HTTP with test DB and mocks. |

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
