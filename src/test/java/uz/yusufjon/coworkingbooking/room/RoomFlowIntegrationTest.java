package uz.yusufjon.coworkingbooking.room;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import uz.yusufjon.coworkingbooking.support.AbstractMockMvcIntegrationTest;
import uz.yusufjon.coworkingbooking.user.entity.Role;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RoomFlowIntegrationTest extends AbstractMockMvcIntegrationTest {

    @Test
    void customerCannotCreateRoomAndAdminCanCreateRoom() throws Exception {
        AuthTokens customer = registerUser("Room Customer", uniqueEmail("room-customer"), "Secret123");
        AuthTokens admin = createAndLoginUser("Room Admin", uniqueEmail("room-admin"), "Admin123", Role.ADMIN, true);
        String roomPayload = roomPayload("Innovation Hub");

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(customer.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Forbidden"));

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", bearer(admin.accessToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Innovation Hub"));
    }

    @Test
    void authenticatedUserCanListRoomsAndFetchRoomDetails() throws Exception {
        AuthTokens customer = registerUser("Room Reader", uniqueEmail("room-reader"), "Secret123");
        Long roomId = roomByName("Ocean Room").getId();

        mockMvc.perform(get("/api/rooms")
                        .header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].name").exists());

        mockMvc.perform(get("/api/rooms/%d".formatted(roomId))
                        .header("Authorization", bearer(customer.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(roomId));
    }

    @Test
    void deactivateRemovesRoomFromAvailabilityAndActivateRestoresIt() throws Exception {
        AuthTokens admin = createAndLoginUser("Availability Admin", uniqueEmail("availability-admin"), "Admin123", Role.ADMIN, true);
        AuthTokens customer = registerUser("Availability Customer", uniqueEmail("availability-customer"), "Secret123");
        Long roomId = roomByName("Ocean Room").getId();
        LocalDateTime startTime = LocalDateTime.now().plusDays(30).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endTime = startTime.plusHours(1);

        mockMvc.perform(patch("/api/rooms/%d/deactivate".formatted(roomId))
                        .header("Authorization", bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        MvcResult unavailableResult = mockMvc.perform(get("/api/rooms/available")
                        .header("Authorization", bearer(customer.accessToken()))
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode unavailableRooms = json(unavailableResult).get("data");
        assertThat(StreamSupport.stream(unavailableRooms.spliterator(), false)
                .map(node -> node.get("id").asLong()))
                .doesNotContain(roomId);

        mockMvc.perform(patch("/api/rooms/%d/activate".formatted(roomId))
                        .header("Authorization", bearer(admin.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        MvcResult availableResult = mockMvc.perform(get("/api/rooms/available")
                        .header("Authorization", bearer(customer.accessToken()))
                        .param("startTime", startTime.toString())
                        .param("endTime", endTime.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode availableRooms = json(availableResult).get("data");
        assertThat(StreamSupport.stream(availableRooms.spliterator(), false)
                .map(node -> node.get("id").asLong()))
                .contains(roomId);
    }

    private String roomPayload(String name) {
        return """
                {
                  "name": "%s",
                  "location": "3rd floor",
                  "capacity": 8,
                  "hourlyPrice": 35.00,
                  "openTime": "%s",
                  "closeTime": "%s"
                }
                """.formatted(name, LocalTime.of(8, 0), LocalTime.of(20, 0));
    }
}
