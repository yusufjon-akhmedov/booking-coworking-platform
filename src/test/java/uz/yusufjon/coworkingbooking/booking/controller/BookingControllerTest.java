package uz.yusufjon.coworkingbooking.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uz.yusufjon.coworkingbooking.booking.dto.BookingResponse;
import uz.yusufjon.coworkingbooking.booking.dto.CancelBookingRequest;
import uz.yusufjon.coworkingbooking.booking.dto.CreateBookingRequest;
import uz.yusufjon.coworkingbooking.booking.service.BookingService;
import uz.yusufjon.coworkingbooking.config.SecurityConfig;
import uz.yusufjon.coworkingbooking.security.JwtAccessDeniedHandler;
import uz.yusufjon.coworkingbooking.security.JwtAuthenticationEntryPoint;
import uz.yusufjon.coworkingbooking.security.jwt.JwtAuthenticationFilter;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetailsService;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.yusufjon.coworkingbooking.support.SecurityTestUtils.authenticatedUser;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void customerCanCreateBooking() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        request.setNotes("Focus session");

        when(bookingService.createBooking(anyLong(), any(CreateBookingRequest.class))).thenReturn(bookingResponse("CONFIRMED"));

        mockMvc.perform(post("/api/bookings")
                        .with(authenticatedUser(1L, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Booking created successfully"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void unauthenticatedCreateBookingReturnsUnauthorized() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(7L);
        request.setStartTime(LocalDateTime.now().plusDays(1));
        request.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void customerCanFetchOwnBookings() throws Exception {
        when(bookingService.getMyBookings(1L)).thenReturn(List.of(bookingResponse("CONFIRMED")));

        mockMvc.perform(get("/api/bookings/me")
                        .with(authenticatedUser(1L, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("My bookings fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void customerCanCancelOwnBooking() throws Exception {
        CancelBookingRequest request = new CancelBookingRequest();
        request.setReason("Change of plans");

        when(bookingService.cancelBooking(anyLong(), anyLong(), any(CancelBookingRequest.class)))
                .thenReturn(bookingResponse("CANCELLED"));

        mockMvc.perform(patch("/api/bookings/10/cancel")
                        .with(authenticatedUser(1L, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Booking cancelled successfully"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void customerCannotAccessAdminBookingEndpoint() throws Exception {
        mockMvc.perform(get("/api/bookings/admin")
                        .with(authenticatedUser(1L, Role.CUSTOMER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    private BookingResponse bookingResponse(String status) {
        return BookingResponse.builder()
                .id(10L)
                .userId(1L)
                .roomId(7L)
                .roomName("Focus Room")
                .status(status)
                .startTime(LocalDateTime.of(2026, 4, 1, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 1, 11, 0))
                .notes("Focus session")
                .createdAt(LocalDateTime.of(2026, 3, 30, 9, 0))
                .build();
    }
}
