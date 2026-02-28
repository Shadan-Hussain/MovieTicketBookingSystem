package com.example.MovieTicketBookingSystemBackend;

import com.example.MovieTicketBookingSystemBackend.dto.StripeSessionResponse;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddCityRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddHallRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddMovieRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddSeatsRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddShowRequest;
import com.example.MovieTicketBookingSystemBackend.dto.admin.AddTheatreRequest;
import com.example.MovieTicketBookingSystemBackend.service.StripeService;
import com.example.MovieTicketBookingSystemBackend.service.StripeWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class BackendIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        when(stripeService.createCheckoutSession(anyLong(), anyLong()))
                .thenReturn(new StripeSessionResponse("cs_test_123", "https://checkout.stripe.com/test"));
        seedData();
    }

    private void seedData() throws Exception {
        cityId = createCity("Mumbai");
        theatreId = createTheatre(cityId, "PVR Andheri");
        hallId = createHall(theatreId, "Screen 1");
        createSeats(hallId, 2, 3);
        movieId = createMovie("Inception", 148);
        showId = createShow(movieId, hallId);
        seatId = getFirstSeatIdForShow(showId);
    }

    private Long createCity(String name) throws Exception {
        AddCityRequest req = new AddCityRequest();
        req.setName(name);
        req.setStateCode("MH");
        String body = mockMvc.perform(post("/admin/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
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
        String body = mockMvc.perform(post("/admin/theatres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createHall(Long theatreId, String name) throws Exception {
        AddHallRequest req = new AddHallRequest();
        req.setTheatreId(theatreId);
        req.setName(name);
        String body = mockMvc.perform(post("/admin/halls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
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
        mockMvc.perform(post("/admin/halls/" + hallId + "/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.count").value(rows * cols));
    }

    private Long createMovie(String name, int durationMins) throws Exception {
        AddMovieRequest req = new AddMovieRequest();
        req.setName(name);
        req.setDurationMins(durationMins);
        String body = mockMvc.perform(post("/admin/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long createShow(Long movieId, Long hallId) throws Exception {
        OffsetDateTime start = OffsetDateTime.now().plusHours(1);
        OffsetDateTime end = start.plusHours(2);
        AddShowRequest req = new AddShowRequest();
        req.setMovieId(movieId);
        req.setHallId(hallId);
        req.setStartTime(start);
        req.setEndTime(end);
        String body = mockMvc.perform(post("/admin/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private Long getFirstSeatIdForShow(Long showId) throws Exception {
        String body = mockMvc.perform(get("/shows/{showId}/seats", showId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("seatId").asLong();
    }

    @Nested
    @DisplayName("GET /cities")
    class CitiesApi {

        @Test
        @DisplayName("returns list of cities including seeded city")
        void listCities() throws Exception {
            mockMvc.perform(get("/cities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == 'Mumbai')]").exists())
                    .andExpect(jsonPath("$[?(@.cityId == " + cityId + ")]").exists());
        }
    }

    @Nested
    @DisplayName("GET /movies")
    class MoviesApi {

        @Test
        @DisplayName("returns movies that have shows in the given city")
        void getMoviesByCity() throws Exception {
            mockMvc.perform(get("/movies").param("city_id", String.valueOf(cityId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.name == 'Inception')]").exists())
                    .andExpect(jsonPath("$[?(@.movieId == " + movieId + ")]").exists());
        }

        @Test
        @DisplayName("returns empty list for city with no shows")
        void getMoviesByCityEmpty() throws Exception {
            Long otherCityId = createCity("Pune");
            mockMvc.perform(get("/movies").param("city_id", String.valueOf(otherCityId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /shows")
    class ShowsApi {

        @Test
        @DisplayName("returns shows for city and movie")
        void getShowsByCityAndMovie() throws Exception {
            mockMvc.perform(get("/shows")
                            .param("city_id", String.valueOf(cityId))
                            .param("movie_id", String.valueOf(movieId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[?(@.showId == " + showId + ")]").exists());
        }

        @Test
        @DisplayName("returns empty list for non-existent city or movie")
        void getShowsEmptyForNonExistent() throws Exception {
            mockMvc.perform(get("/shows").param("city_id", "99999").param("movie_id", String.valueOf(movieId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
            mockMvc.perform(get("/shows").param("city_id", String.valueOf(cityId)).param("movie_id", "99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /shows/{showId}/seats")
    class ShowSeatsApi {

        @Test
        @DisplayName("returns seats for show with status")
        void getSeatsForShow() throws Exception {
            mockMvc.perform(get("/shows/{showId}/seats", showId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                    .andExpect(jsonPath("$[0].seatId").exists());
        }
    }

    @Nested
    @DisplayName("POST /shows/{showId}/seats/{seatId}/lock")
    class LockSeatApi {

        @Test
        @DisplayName("locks seat and returns 200")
        void lockSeat() throws Exception {
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 404 for non-existent show-seat")
        void lockSeatNotFound() throws Exception {
            mockMvc.perform(post("/shows/99999/seats/99999/lock"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when seat is already locked")
        void lockSeatAlreadyLocked() throws Exception {
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /shows/{showId}/seats/{seatId}/payment-session")
    class PaymentSessionApi {

        @Test
        @DisplayName("returns 410 Gone when seat is not locked")
        void paymentSessionWithoutLock() throws Exception {
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/payment-session", showId, seatId))
                    .andExpect(status().isGone());
        }

        @Test
        @DisplayName("returns session URL when seat is locked")
        void paymentSessionWithLock() throws Exception {
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/lock", showId, seatId))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/shows/{showId}/seats/{seatId}/payment-session", showId, seatId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value("cs_test_123"))
                    .andExpect(jsonPath("$.sessionUrl").exists());
        }
    }

    @Nested
    @DisplayName("GET /tickets")
    class TicketsApi {

        @Test
        @DisplayName("returns 404 when no successful payment for show-seat")
        void getTicketNotFound() throws Exception {
            mockMvc.perform(get("/tickets")
                            .param("show_id", String.valueOf(showId))
                            .param("seat_id", String.valueOf(seatId)))
                    .andExpect(status().isNotFound());
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
        @DisplayName("POST /admin/cities with blank name returns 400")
        void addCityBlankName() throws Exception {
            AddCityRequest req = new AddCityRequest();
            req.setName("   ");
            mockMvc.perform(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/cities with null name returns 400")
        void addCityNullName() throws Exception {
            mockMvc.perform(post("/admin/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/theatres without cityId returns 400")
        void addTheatreMissingCityId() throws Exception {
            AddTheatreRequest req = new AddTheatreRequest();
            req.setName("Theatre");
            mockMvc.perform(post("/admin/theatres")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/halls/{hallId}/seats with invalid rows returns 400")
        void addSeatsInvalidRows() throws Exception {
            AddSeatsRequest req = new AddSeatsRequest();
            req.setRows(0);
            req.setCols(5);
            mockMvc.perform(post("/admin/halls/{hallId}/seats", hallId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /admin/shows without required fields returns 400")
        void addShowMissingFields() throws Exception {
            mockMvc.perform(post("/admin/shows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }
}
