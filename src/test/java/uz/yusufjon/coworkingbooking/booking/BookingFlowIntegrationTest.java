package uz.yusufjon.coworkingbooking.booking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uz.yusufjon.coworkingbooking.support.AbstractMockMvcIntegrationTest;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingFlowIntegrationTest extends AbstractMockMvcIntegrationTest {

    @Test
    void customerCanCreateBookingFetchDetailsAndSeeHistory() throws Exception {
        AuthTokens customer = registerUser("Booking Customer", uniqueEmail("booking-customer"), "Secret123");
        Long roomId = roomByName("Ocean Room").getId();

        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        MvcResult createResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(customer.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "notes": "Quiet focus session"
                                }
                                """.formatted(roomId, startTime, endTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andReturn();

        Long bookingId = json(createResult).get("data").get("id").asLong();

        mockMvc.perform(get("/api/bookings/%d".formatted(bookingId))
                        .header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(bookingId))
                .andExpect(jsonPath("$.data.roomId").value(roomId));

        mockMvc.perform(get("/api/bookings/history")
                        .header("Authorization", bearer(customer.accessToken()))
                        .param("status", "CONFIRMED")
                        .param("roomId", roomId.toString())
                        .param("from", startTime.minusMinutes(5).toString())
                        .param("to", endTime.plusMinutes(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(bookingId));
    }

    @Test
    void conflictingBookingReturnsConflict() throws Exception {
        AuthTokens firstCustomer = registerUser("First Customer", uniqueEmail("conflict-a"), "Secret123");
        AuthTokens secondCustomer = registerUser("Second Customer", uniqueEmail("conflict-b"), "Secret123");
        Long roomId = roomByName("Ocean Room").getId();

        LocalDateTime startTime = LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        createBooking(firstCustomer.accessToken(), roomId, startTime, endTime);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(secondCustomer.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "notes": "Overlapping booking"
                                }
                                """.formatted(roomId, startTime.plusMinutes(30), endTime.plusMinutes(30))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Selected time slot is already booked"));
    }

    @Test
    void ownerCanCancelBookingAndNonOwnerCannotCancelIt() throws Exception {
        AuthTokens owner = registerUser("Owner Customer", uniqueEmail("owner"), "Secret123");
        AuthTokens otherCustomer = registerUser("Other Customer", uniqueEmail("other"), "Secret123");
        Long roomId = roomByName("Focus Room").getId();

        LocalDateTime startTime = LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0);
        Long bookingId = createBooking(owner.accessToken(), roomId, startTime, startTime.plusHours(1));

        mockMvc.perform(patch("/api/bookings/%d/cancel".formatted(bookingId))
                        .header("Authorization", bearer(otherCustomer.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Not mine"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));

        mockMvc.perform(patch("/api/bookings/%d/cancel".formatted(bookingId))
                        .header("Authorization", bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"Change of plans"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/bookings/history")
                        .header("Authorization", bearer(owner.accessToken()))
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(bookingId));
    }

    @Test
    void ownerCanRescheduleBookingAndConflictingRescheduleReturnsConflict() throws Exception {
        AuthTokens owner = registerUser("Reschedule Owner", uniqueEmail("reschedule-owner"), "Secret123");
        AuthTokens otherCustomer = registerUser("Reschedule Other", uniqueEmail("reschedule-other"), "Secret123");
        Long roomId = roomByName("Ocean Room").getId();

        LocalDateTime ownerStart = LocalDateTime.now().plusDays(4).withHour(16).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime otherStart = LocalDateTime.now().plusDays(4).withHour(19).withMinute(0).withSecond(0).withNano(0);
        Long bookingId = createBooking(owner.accessToken(), roomId, ownerStart, ownerStart.plusHours(1));
        createBooking(otherCustomer.accessToken(), roomId, otherStart, otherStart.plusHours(1));

        LocalDateTime validRescheduleStart = LocalDateTime.now().plusDays(4).withHour(17).withMinute(0).withSecond(0).withNano(0);

                mockMvc.perform(patch("/api/bookings/%d/reschedule".formatted(bookingId))
                        .header("Authorization", bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }
                                """.formatted(validRescheduleStart, validRescheduleStart.plusHours(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startTime").value(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(validRescheduleStart)));

        mockMvc.perform(patch("/api/bookings/%d/reschedule".formatted(bookingId))
                        .header("Authorization", bearer(owner.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startTime": "%s",
                                  "endTime": "%s"
                                }
                                """.formatted(otherStart.plusMinutes(30), otherStart.plusHours(1).plusMinutes(30))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Selected reschedule slot is already booked"));
    }

    @Test
    void adminCanViewPaginatedBookings() throws Exception {
        AuthTokens customer = registerUser("Admin List Customer", uniqueEmail("admin-booking"), "Secret123");
        AuthTokens admin = createAndLoginUser("Admin Booker", uniqueEmail("booking-admin"), "Admin123", Role.ADMIN, true);
        Long roomId = roomByName("Focus Room").getId();

        LocalDateTime startTime = LocalDateTime.now().plusDays(5).withHour(11).withMinute(0).withSecond(0).withNano(0);
        Long bookingId = createBooking(customer.accessToken(), roomId, startTime, startTime.plusHours(1));

        MvcResult result = mockMvc.perform(get("/api/bookings/admin")
                        .header("Authorization", bearer(admin.accessToken()))
                        .param("status", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = json(result).get("data").get("content");
        assertThat(StreamSupport.stream(content.spliterator(), false)
                .map(node -> node.get("id").asLong()))
                .contains(bookingId);
    }

    private Long createBooking(String accessToken, Long roomId, LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %d,
                                  "startTime": "%s",
                                  "endTime": "%s",
                                  "notes": "Integration booking"
                                }
                                """.formatted(roomId, startTime, endTime)))
                .andExpect(status().isCreated())
                .andReturn();

        return json(result).get("data").get("id").asLong();
    }
}
