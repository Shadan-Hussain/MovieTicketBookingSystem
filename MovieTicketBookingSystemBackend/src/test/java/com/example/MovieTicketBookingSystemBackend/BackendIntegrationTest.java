package com.example.MovieTicketBookingSystemBackend;

import com.example.MovieTicketBookingSystemBackend.dto.LoginRequest;
import com.example.MovieTicketBookingSystemBackend.dto.SignupRequest;
import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddCityRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddHallRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddMovieRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddSeatsRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddShowRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddTheatreRequest;
import com.example.MovieTicketBookingSystemBackend.config.AdminRoleFilter;
import com.example.MovieTicketBookingSystemBackend.config.JwtAuthFilter;
import com.example.MovieTicketBookingSystemBackend.model.Ticket;
import com.example.MovieTicketBookingSystemBackend.model.Transaction;
import com.example.MovieTicketBookingSystemBackend.model.User;
import com.example.MovieTicketBookingSystemBackend.repository.ShowRepository;
import com.example.MovieTicketBookingSystemBackend.repository.SeatRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TicketRepository;
import com.example.MovieTicketBookingSystemBackend.repository.TransactionRepository;
import com.example.MovieTicketBookingSystemBackend.repository.UserRepository;
import com.example.MovieTicketBookingSystemBackend.service.JwtService;
import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.example.MovieTicketBookingSystemBackend.service.StripeWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Movie Ticket Booking backend API.
 * Uses H2 in-memory DB and a mocked Redis (seat lock) and Stripe (payment session).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private AdminRoleFilter adminRoleFilter;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ShowRepository showRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TicketRepository ticketRepository;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    private Long cityId;
    private Long theatreId;
    private Long hallId;
    private Long movieId;
    private Long showId;
    private Long seatId;
    private String authToken;
    private String adminToken;
    private String seedCityName;
    private String seedMovieName;

    @BeforeAll
    void initAll() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(jwtAuthFilter, "/*")
                .addFilter(adminRoleFilter, "/*")
                .build();
        authToken = signupAndGetToken("testuser", "password123", "testuser@test.local");
        adminToken = signupAndGetToken("adminuser", "adminpass", "adminuser@test.local");
        userRepository.findByUsername("adminuser").ifPresent(u -> {
            u.setRole(User.ROLE_ADMIN);
            userRepository.save(u);
        });
    }

    @BeforeEach
    void setUp() throws Exception {
        when(stripeService.createCheckoutSession(anyLong(), anyLong(), anyLong()))
                .thenReturn(new StripeSessionResponse("cs_test_123", "https://checkout.stripe.com/test"));
        seedData();
    }

    private String signupAndGetToken(String username, String password, String email) throws Exception {
        SignupRequest req = new SignupRequest();
        req.setUsername(username);
        req.setPassword(password);
        req.setEmail(email);
        req.setName(username != null ? username : "Test User");
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
        return loginAndGetToken(username, password);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withAuth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + authToken);
    }

    private MockHttpServletRequestBuilder withAdminAuth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + adminToken);
    }

    private void seedData() throws Exception {
        String suffix = "_" + System.nanoTime();
        seedCityName = "Mumbai" + suffix;
        seedMovieName = "Inception" + suffix;
        cityId = createCity(seedCityName);
        theatreId = createTheatre(cityId, "PVR Andheri" + suffix);
        hallId = createHall(theatreId, "Screen 1" + suffix);
        createSeats(hallId, 2, 3);
        movieId = createMovie(seedMovieName, 148);
        showId = createShow(movieId, hallId);
        seatId = getFirstSeatIdForShow(showId);
    }

    private Long createCity(String name) throws Exception {
        AddCityRequest req = new AddCityRequest();
        req.setName(name);
        req.setStateCode("MH");
        String body = mockMvc.perform(withAdminAuth(post("/admin/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createTheatre(Long cityId, String name) throws Exception {
        AddTheatreRequest req = new AddTheatreRequest();
        req.setCityId(cityId);
        req.setName(name);
        req.setAddress("Andheri West");
        String body = mockMvc.perform(withAdminAuth(post("/admin/theatres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createHall(Long theatreId, String name) throws Exception {
        AddHallRequest req = new AddHallRequest();
        req.setTheatreId(theatreId);
        req.setName(name);
        String body = mockMvc.perform(withAdminAuth(post("/admin/halls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void createSeats(Long hallId, int rows, int cols) throws Exception {
        AddSeatsRequest req = new AddSeatsRequest();
        req.setRows(rows);
        req.setCols(cols);
        req.setPremiumRowStart(0);
        req.setPremiumRowEnd(0);
        req.setPricePremium(200L);
        req.setPriceNormal(100L);
        mockMvc.perform(withAdminAuth(post("/admin/halls/" + hallId + "/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Seats added"));
    }

    private Long createMovie(String name, int durationMins) throws Exception {
        AddMovieRequest req = new AddMovieRequest();
        req.setName(name);
        req.setDurationMins(durationMins);
        req.setDescription("A mind-bending thriller");
        req.setLanguage("EN");
        String body = mockMvc.perform(withAdminAuth(post("/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createShow(Long movieId, Long hallId) throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusMinutes(178);
        AddShowRequest req = new AddShowRequest();
        req.setMovieId(movieId);
        req.setHallId(hallId);
        req.setStartTime(start);
        req.setEndTime(end);
        String body = mockMvc.perform(withAdminAuth(post("/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long getFirstSeatIdForShow(Long showId) throws Exception {
        String body = mockMvc.perform(withAuth(get("/shows/{showId}/seats", showId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("seatId").asLong();
    }

    @Nested
    @DisplayName("POST /auth/signup and POST /auth/login")
    class AuthApi {

        @Test
        @DisplayName("signup returns 201 with id only; token from login")
        void signupSuccess() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("newuser");
            req.setPassword("pass123");
            req.setEmail("newuser@test.local");
            req.setName("New User");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.token").doesNotExist());
        }

        @Test
        @DisplayName("signup with duplicate username returns 400")
        void signupDuplicateUsername() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("testuser");
            req.setPassword("other");
            req.setEmail("different@test.local");
            req.setName("Test User");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup with blank username returns 400")
        void signupBlankUsername() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("   ");
            req.setPassword("pass");
            req.setEmail("a@test.local");
            req.setName("Someone");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup with blank email returns 400")
        void signupBlankEmail() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("someuser");
            req.setPassword("pass123");
            req.setEmail("   ");
            req.setName("Someone");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup with duplicate email returns 400")
        void signupDuplicateEmail() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("anotheruser");
            req.setPassword("pass123");
            req.setEmail("testuser@test.local");
            req.setName("Another User");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("signup with missing name returns 400")
        void signupMissingName() throws Exception {
            SignupRequest req = new SignupRequest();
            req.setUsername("naminguser");
            req.setPassword("pass123");
            req.setEmail("naming@test.local");
            mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("login returns 200 with token for valid credentials")
        void loginSuccess() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("testuser");
            req.setPassword("password123");
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").exists())
                    .andExpect(jsonPath("$.userId").exists());
        }

        @Test
        @DisplayName("login with wrong password returns 401")
        void loginWrongPassword() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("testuser");
            req.setPassword("wrong");
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("login with non-existent username returns 401")
        void loginWrongUsername() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("nobody");
            req.setPassword("pass");
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /cities")
    class CitiesApi {

        @Test
        @DisplayName("returns list of cities including seeded city when authenticated")
        void listCities() throws Exception {
            mockMvc.perform(withAuth(get("/cities")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == '" + seedCityName + "')]").exists())
                    .andExpect(jsonPath("$[?(@.cityId == " + cityId + ")]").exists());
        }

        @Test
        @DisplayName("returns 401 without JWT")
        void listCitiesUnauthorized() throws Exception {
            mockMvc.perform(get("/cities"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 User not found when token userId not in DB")
        void listCitiesUserNotFound() throws Exception {
            String tokenForNonExistentUser = jwtService.generate(99999L, "ghost");
            mockMvc.perform(get("/cities").header("Authorization", "Bearer " + tokenForNonExistentUser))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(containsString("User not found")));
        }
    }

    @Nested
    @DisplayName("GET /movies")
    class MoviesApi {

        @Test
        @DisplayName("returns movies that have shows in the given city when authenticated")
        void getMoviesByCity() throws Exception {
            mockMvc.perform(withAuth(get("/movies").param("city_id", String.valueOf(cityId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == '" + seedMovieName + "')]").exists())
                    .andExpect(jsonPath("$[?(@.movieId == " + movieId + ")]").exists());
        }

        @Test
        @DisplayName("returns empty list for city with no shows")
        void getMoviesByCityEmpty() throws Exception {
            Long otherCityId = createCity("Pune_" + System.nanoTime());
            mockMvc.perform(withAuth(get("/movies").param("city_id", String.valueOf(otherCityId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /shows")
    class ShowsApi {

        @Test
        @DisplayName("returns shows for city and movie when authenticated")
        void getShowsByCityAndMovie() throws Exception {
            mockMvc.perform(withAuth(get("/shows")
                            .param("city_id", String.valueOf(cityId))
                            .param("movie_id", String.valueOf(movieId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.showId == " + showId + ")]").exists());
        }

        @Test
        @DisplayName("returns empty list for non-existent city or movie")
        void getShowsEmptyForNonExistent() throws Exception {
            mockMvc.perform(withAuth(get("/shows").param("city_id", "99999").param("movie_id", String.valueOf(movieId))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(withAuth(get("/shows").param("city_id", String.valueOf(cityId)).param("movie_id", "99999")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /shows/{showId}/seats")
    class ShowSeatsApi {

        @Test
        @DisplayName("returns seats for show with status when authenticated")
        void getSeatsForShow() throws Exception {
            mockMvc.perform(withAuth(get("/shows/{showId}/seats", showId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                    .andExpect(jsonPath("$[0].seatId").exists());
        }

        @Test
        @DisplayName("returns LOCKED status for seat when locked in Redis")
        void getSeatsForShowReflectsLockedStatus() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk());
            // Seats are ordered by seatId; seatId is the first seat from seedData, so first element is the one we locked
            mockMvc.perform(withAuth(get("/shows/{showId}/seats", showId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].seatId").value(seatId))
                    .andExpect(jsonPath("$[0].status").value("LOCKED"));
        }
    }

    @Nested
    @DisplayName("POST /shows/{showId}/seats/{seatId}/lock")
    class LockSeatApi {

        @Test
        @DisplayName("locks seat and returns 200 with success message when authenticated")
        void lockSeat() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Seat successfully locked"));
        }

        @Test
        @DisplayName("returns 401 without JWT")
        void lockSeatUnauthorized() throws Exception {
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 404 for non-existent show-seat")
        void lockSeatNotFound() throws Exception {
            mockMvc.perform(withAuth(post("/shows/99999/seats/99999/lock")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when seat is already locked")
        void lockSeatAlreadyLocked() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk());
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /shows/{showId}/seats/{seatId}/payment-session")
    class PaymentSessionApi {

        @Test
        @DisplayName("returns 403 when seat is not locked")
        void paymentSessionWithoutLock() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/payment-session", showId, seatId)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("returns session URL when seat is locked by same user")
        void paymentSessionWithLock() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk());
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/payment-session", showId, seatId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("cs_test_123"))
                    .andExpect(jsonPath("$.sessionUrl").exists());
        }

        @Test
        @DisplayName("returns 403 when lock is held by another user")
        void paymentSessionLockHeldByOtherUser() throws Exception {
            // testuser (authToken) locks the seat
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk());
            String otherToken = signupAndGetToken("otheruser", "pass", "otheruser@test.local");
            // otheruser tries payment-session -> 403 (lock held by testuser)
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/payment-session", showId, seatId)
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /tickets")
    class TicketsApi {

        @Test
        @DisplayName("returns 404 when no successful payment for show-seat")
        void getTicketNotFound() throws Exception {
            mockMvc.perform(withAuth(get("/tickets")
                            .param("show_id", String.valueOf(showId))
                            .param("seat_id", String.valueOf(seatId))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 401 without JWT")
        void getTicketUnauthorized() throws Exception {
            mockMvc.perform(get("/tickets")
                            .param("show_id", String.valueOf(showId))
                            .param("seat_id", String.valueOf(seatId)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /tickets without params returns list of current user tickets")
        void getMyTicketsList() throws Exception {
            mockMvc.perform(withAuth(get("/tickets")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
            Transaction txn = new Transaction();
            txn.setShow(showRepository.getReferenceById(showId));
            txn.setSeat(seatRepository.getReferenceById(seatId));
            txn.setUser(userRepository.findByUsername("testuser").orElseThrow());
            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setAmount(100L);
            txn.setCurrency("usd");
            txn.setStripeSessionId("test-session-" + System.nanoTime());
            txn.setCreatedAt(Instant.now());
            txn = transactionRepository.save(txn);
            Ticket ticket = new Ticket();
            ticket.setTransaction(txn);
            ticket.setCreatedAt(Instant.now());
            ticketRepository.save(ticket);
            mockMvc.perform(withAuth(get("/tickets")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].ticketId").exists())
                    .andExpect(jsonPath("$[0].showId").value(showId))
                    .andExpect(jsonPath("$[0].seatId").value(seatId));
        }

        @Test
        @DisplayName("returns 404 when ticket belongs to another user")
        void getTicketReturns404WhenTicketBelongsToAnotherUser() throws Exception {
            SignupRequest signupReq = new SignupRequest();
            signupReq.setUsername("ticketowner");
            signupReq.setPassword("pass123");
            signupReq.setEmail("ticketowner@test.local");
            signupReq.setName("Ticket Owner");
            String signupBody = mockMvc.perform(post("/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupReq)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long ownerUserId = objectMapper.readTree(signupBody).get("id").asLong();

            Transaction txn = new Transaction();
            txn.setShow(showRepository.getReferenceById(showId));
            txn.setSeat(seatRepository.getReferenceById(seatId));
            txn.setUser(userRepository.getReferenceById(ownerUserId));
            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setAmount(100L);
            txn.setCurrency("usd");
            txn.setStripeSessionId("test-session-" + System.nanoTime());
            txn.setCreatedAt(Instant.now());
            txn = transactionRepository.save(txn);

            Ticket ticket = new Ticket();
            ticket.setTransaction(txn);
            ticket.setCreatedAt(Instant.now());
            ticketRepository.save(ticket);

            mockMvc.perform(withAuth(get("/tickets")
                            .param("show_id", String.valueOf(showId))
                            .param("seat_id", String.valueOf(seatId))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /transactions")
    class TransactionsApi {

        @Test
        @DisplayName("returns list of current user transactions (non-PENDING) when authenticated")
        void getMyTransactionsList() throws Exception {
            mockMvc.perform(withAuth(get("/transactions")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("excludes PENDING transactions from list")
        void getMyTransactionsExcludesPending() throws Exception {
            Transaction txn = new Transaction();
            txn.setShow(showRepository.getReferenceById(showId));
            txn.setSeat(seatRepository.getReferenceById(seatId));
            txn.setUser(userRepository.findByUsername("testuser").orElseThrow());
            txn.setStatus(Transaction.STATUS_PENDING);
            txn.setAmount(100L);
            txn.setCurrency("usd");
            txn.setStripeSessionId("pending-" + System.nanoTime());
            txn.setCreatedAt(Instant.now());
            transactionRepository.save(txn);
            mockMvc.perform(withAuth(get("/transactions")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.status == 'PENDING')]").isEmpty());
        }

        @Test
        @DisplayName("returns 401 without JWT")
        void getTransactionsUnauthorized() throws Exception {
            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /redirect/success and /redirect/cancel")
    class RedirectApi {

        @Test
        @DisplayName("success returns 200 with message and session_id")
        void redirectSuccess() throws Exception {
            mockMvc.perform(get("/redirect/success").param("session_id", "cs_abc123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Payment succeeded"))
                    .andExpect(jsonPath("$.sessionId").value("cs_abc123"));
        }

        @Test
        @DisplayName("success without session_id returns 200 with message")
        void redirectSuccessWithoutSessionId() throws Exception {
            mockMvc.perform(get("/redirect/success"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Payment succeeded"));
        }

        @Test
        @DisplayName("cancel returns 200 with message")
        void redirectCancel() throws Exception {
            mockMvc.perform(get("/redirect/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Payment cancelled"));
        }
    }

    @Nested
    @DisplayName("POST /webhook/stripe")
    class StripeWebhookApi {

        @Test
        @DisplayName("returns 200 when webhook service accepts payload")
        void webhookSuccess() throws Exception {
            when(stripeWebhookService.processWebhook(any(byte[].class), any()))
                    .thenReturn(null);
            mockMvc.perform(post("/webhook/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "t=123,v1=abc")
                            .content("{\"type\":\"checkout.session.completed\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 400 when webhook service rejects payload")
        void webhookRejected() throws Exception {
            when(stripeWebhookService.processWebhook(any(byte[].class), any()))
                    .thenReturn("Invalid signature");
            mockMvc.perform(post("/webhook/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "t=123,v1=bad")
                            .content("{\"type\":\"checkout.session.completed\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Invalid signature"));
        }
    }

    @Nested
    @DisplayName("Admin validation")
    class AdminValidation {

        @Test
        @DisplayName("POST /admin/* without JWT returns 401")
        void adminWithoutTokenReturns401() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("City");
            req.setStateCode("MH");
            mockMvc.perform(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("POST /admin/* with USER role returns 403")
        void adminWithUserRoleReturns403() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("City");
            req.setStateCode("MH");
            mockMvc.perform(withAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /admin/cities with blank name returns 400")
        void addCityBlankName() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("   ");
            req.setStateCode("MH");
            mockMvc.perform(withAdminAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/cities with missing stateCode returns 400")
        void addCityMissingStateCode() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("Pune");
            mockMvc.perform(withAdminAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/cities with null name returns 400")
        void addCityNullName() throws Exception {
            mockMvc.perform(withAdminAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/theatres without cityId returns 400")
        void addTheatreMissingCityId() throws Exception {
            AddTheatreRequest req = new AddTheatreRequest();
            req.setName("Theatre");
            req.setAddress("Some Address");
            mockMvc.perform(withAdminAuth(post("/admin/theatres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/theatres without address returns 400")
        void addTheatreMissingAddress() throws Exception {
            AddTheatreRequest req = new AddTheatreRequest();
            req.setCityId(cityId);
            req.setName("Theatre");
            mockMvc.perform(withAdminAuth(post("/admin/theatres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/halls/{hallId}/seats with invalid rows returns 400")
        void addSeatsInvalidRows() throws Exception {
            AddSeatsRequest req = new AddSeatsRequest();
            req.setRows(0);
            req.setCols(5);
            mockMvc.perform(withAdminAuth(post("/admin/halls/{hallId}/seats", hallId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows without required fields returns 400")
        void addShowMissingFields() throws Exception {
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows with non-existent hallId returns 400")
        void addShowInvalidHall() throws Exception {
            OffsetDateTime start = OffsetDateTime.now().plusHours(1);
            AddShowRequest req = new AddShowRequest();
            req.setMovieId(movieId);
            req.setHallId(99999L);
            req.setStartTime(start);
            req.setEndTime(start.plusHours(2));
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows with non-existent movieId returns 400")
        void addShowInvalidMovie() throws Exception {
            OffsetDateTime start = OffsetDateTime.now().plusHours(1);
            AddShowRequest req = new AddShowRequest();
            req.setMovieId(99999L);
            req.setHallId(hallId);
            req.setStartTime(start);
            req.setEndTime(start.plusHours(2));
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/movies without description returns 400")
        void addMovieMissingDescription() throws Exception {
            AddMovieRequest req = new AddMovieRequest();
            req.setName("A Movie");
            req.setDurationMins(120);
            req.setLanguage("EN");
            mockMvc.perform(withAdminAuth(post("/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/movies without language returns 400")
        void addMovieMissingLanguage() throws Exception {
            AddMovieRequest req = new AddMovieRequest();
            req.setName("A Movie");
            req.setDurationMins(120);
            req.setDescription("Some description");
            mockMvc.perform(withAdminAuth(post("/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/cities with duplicate name returns 400")
        void addCityDuplicateName() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("DupCity");
            req.setStateCode("MH");
            mockMvc.perform(withAdminAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated());
            mockMvc.perform(withAdminAuth(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/theatres with duplicate name returns 400")
        void addTheatreDuplicateName() throws Exception {
            AddTheatreRequest req = new AddTheatreRequest();
            req.setCityId(cityId);
            req.setName("DupTheatre");
            req.setAddress("Some Address");
            mockMvc.perform(withAdminAuth(post("/admin/theatres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated());
            mockMvc.perform(withAdminAuth(post("/admin/theatres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/halls with duplicate name in same theatre returns 400")
        void addHallDuplicateNameInTheatre() throws Exception {
            AddHallRequest req = new AddHallRequest();
            req.setTheatreId(theatreId);
            req.setName("DupHall");
            mockMvc.perform(withAdminAuth(post("/admin/halls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated());
            mockMvc.perform(withAdminAuth(post("/admin/halls")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/movies with duplicate name returns 400")
        void addMovieDuplicateName() throws Exception {
            AddMovieRequest req = new AddMovieRequest();
            req.setName("DupMovie");
            req.setDurationMins(100);
            req.setDescription("Some description");
            req.setLanguage("EN");
            mockMvc.perform(withAdminAuth(post("/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated());
            mockMvc.perform(withAdminAuth(post("/admin/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows with overlapping time in same hall returns 400")
        void addShowOverlappingInHall() throws Exception {
            OffsetDateTime overlapStart = OffsetDateTime.now().plusHours(1).plusMinutes(30);
            OffsetDateTime overlapEnd = overlapStart.plusMinutes(178);
            AddShowRequest req = new AddShowRequest();
            req.setMovieId(movieId);
            req.setHallId(hallId);
            req.setStartTime(overlapStart);
            req.setEndTime(overlapEnd);
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows with show duration less than movie duration + 30 returns 400")
        void addShowDurationTooShort() throws Exception {
            OffsetDateTime start = OffsetDateTime.now().plusHours(5);
            OffsetDateTime end = start.plusMinutes(148);
            AddShowRequest req = new AddShowRequest();
            req.setMovieId(movieId);
            req.setHallId(hallId);
            req.setStartTime(start);
            req.setEndTime(end);
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows with show duration more than movie duration + 45 returns 400")
        void addShowDurationTooLong() throws Exception {
            OffsetDateTime start = OffsetDateTime.now().plusHours(5);
            OffsetDateTime end = start.plusMinutes(148 + 50);
            AddShowRequest req = new AddShowRequest();
            req.setMovieId(movieId);
            req.setHallId(hallId);
            req.setStartTime(start);
            req.setEndTime(end);
            mockMvc.perform(withAdminAuth(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Admin options (dropdowns)")
    class AdminOptionsApi {

        @Test
        @DisplayName("GET /admin/options/movies returns id, label and durationMins")
        void adminOptionsMoviesIncludesDuration() throws Exception {
            mockMvc.perform(withAdminAuth(get("/admin/options/movies")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                    .andExpect(jsonPath("$[*].durationMins").value(org.hamcrest.Matchers.hasItem(148)))
                    .andExpect(jsonPath("$[0].id").exists())
                    .andExpect(jsonPath("$[0].label").exists())
                    .andExpect(jsonPath("$[0].durationMins").exists());
        }
    }

    @Nested
    @DisplayName("DELETE /shows/{showId}/seats/{seatId}/lock")
    class UnlockSeatApi {

        @Test
        @DisplayName("unlocks seat when lock is held by current user")
        void unlockSeatSuccess() throws Exception {
            mockMvc.perform(withAuth(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk());
            mockMvc.perform(withAuth(delete("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Seat unlocked"));
        }

        @Test
        @DisplayName("returns 200 with message when lock not held by user")
        void unlockSeatNotHeldByUser() throws Exception {
            mockMvc.perform(withAuth(delete("/shows/{showId}/seats/{seatId}/lock", showId, seatId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Lock not held by user"));
        }
    }
}
