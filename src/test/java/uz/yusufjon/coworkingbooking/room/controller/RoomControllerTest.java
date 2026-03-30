package uz.yusufjon.coworkingbooking.room.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uz.yusufjon.coworkingbooking.common.response.PageResponse;
import uz.yusufjon.coworkingbooking.config.SecurityConfig;
import uz.yusufjon.coworkingbooking.room.dto.AvailableRoomResponse;
import uz.yusufjon.coworkingbooking.room.dto.CreateRoomRequest;
import uz.yusufjon.coworkingbooking.room.dto.RoomResponse;
import uz.yusufjon.coworkingbooking.room.service.RoomService;
import uz.yusufjon.coworkingbooking.security.JwtAccessDeniedHandler;
import uz.yusufjon.coworkingbooking.security.JwtAuthenticationEntryPoint;
import uz.yusufjon.coworkingbooking.security.jwt.JwtAuthenticationFilter;
import uz.yusufjon.coworkingbooking.security.jwt.JwtService;
import uz.yusufjon.coworkingbooking.security.service.CustomUserDetailsService;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uz.yusufjon.coworkingbooking.support.SecurityTestUtils.authenticatedUser;

@WebMvcTest(controllers = RoomController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void adminCanCreateRoom() throws Exception {
        CreateRoomRequest request = createRoomRequest();

        when(roomService.createRoom(any(CreateRoomRequest.class))).thenReturn(availableRoomResponse());

        mockMvc.perform(post("/api/rooms")
                        .with(authenticatedUser(1L, Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Room created successfully"))
                .andExpect(jsonPath("$.data.name").value("Focus Room"));
    }

    @Test
    void customerCannotCreateRoom() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .with(authenticatedUser(2L, Role.CUSTOMER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRoomRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    void availableRoomsEndpointReturnsExpectedStructure() throws Exception {
        when(roomService.getAvailableRooms(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(availableRoomResponse()));

        mockMvc.perform(get("/api/rooms/available")
                        .with(authenticatedUser(2L, Role.CUSTOMER))
                        .param("startTime", "2026-04-01T10:00:00")
                        .param("endTime", "2026-04-01T11:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Available rooms fetched successfully"))
                .andExpect(jsonPath("$.data[0].capacity").value(6));
    }

    @Test
    void authenticatedUserCanListRooms() throws Exception {
        when(roomService.getRooms(any(), any(), any(), any(), any()))
                .thenReturn(new PageResponse<>(List.of(roomResponse(true)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/rooms")
                        .with(authenticatedUser(2L, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Focus Room"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void authenticatedUserCanFetchRoomDetails() throws Exception {
        when(roomService.getRoomDetails(anyLong())).thenReturn(roomResponse(true));

        mockMvc.perform(get("/api/rooms/7")
                        .with(authenticatedUser(2L, Role.CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Room details fetched successfully"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    private CreateRoomRequest createRoomRequest() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Focus Room");
        request.setLocation("2nd floor");
        request.setCapacity(6);
        request.setHourlyPrice(BigDecimal.valueOf(25));
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(18, 0));
        return request;
    }

    private AvailableRoomResponse availableRoomResponse() {
        return AvailableRoomResponse.builder()
                .id(7L)
                .name("Focus Room")
                .location("2nd floor")
                .capacity(6)
                .hourlyPrice(BigDecimal.valueOf(25))
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(18, 0))
                .build();
    }

    private RoomResponse roomResponse(boolean active) {
        return RoomResponse.builder()
                .id(7L)
                .name("Focus Room")
                .location("2nd floor")
                .capacity(6)
                .hourlyPrice(BigDecimal.valueOf(25))
                .openTime(LocalTime.of(8, 0))
                .closeTime(LocalTime.of(18, 0))
                .active(active)
                .build();
    }
}
