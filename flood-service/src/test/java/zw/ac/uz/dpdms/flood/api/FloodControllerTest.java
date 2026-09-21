package zw.ac.uz.dpdms.flood.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.Severity;
import zw.ac.uz.dpdms.flood.service.FloodWorkflow;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FloodControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FloodWorkflow workflow = new FloodWorkflow();
        FloodController controller = new FloodController(workflow);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void submitReturns201WhenAuthorized() throws Exception {
        var metadata = new IncidentMetadata("Ward 1", "Rushinga", "Mashonaland Central", Instant.now(), "recorder1", Severity.HIGH, -17.2, 32.3);
        var request = new FloodRequest(metadata, 2.5, "Mazowe", 5, 12.0, 2);

        mockMvc.perform(post("/api/floods")
                .header("X-Actor-Id", "recorder1")
                .header("X-Role", "WARD_RECORDER")
                .header("X-Hazard", "FLOOD")
                .header("X-Ward", "Ward 1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.peakWaterLevelMetres").value(2.5));
    }

    @Test
    void submitReturns403WhenWardMismatched() throws Exception {
        var metadata = new IncidentMetadata("Ward 1", "Rushinga", "Mashonaland Central", Instant.now(), "recorder1", Severity.HIGH, -17.2, 32.3);
        var request = new FloodRequest(metadata, 2.5, "Mazowe", 5, 12.0, 2);

        mockMvc.perform(post("/api/floods")
                .header("X-Actor-Id", "recorder1")
                .header("X-Role", "WARD_RECORDER")
                .header("X-Hazard", "FLOOD")
                .header("X-Ward", "Ward 2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitReturns403ForNationalViewer() throws Exception {
        var metadata = new IncidentMetadata("Ward 1", "Rushinga", "Mashonaland Central", Instant.now(), "viewer1", Severity.HIGH, -17.2, 32.3);
        var request = new FloodRequest(metadata, 2.5, "Mazowe", 5, 12.0, 2);

        mockMvc.perform(post("/api/floods")
                .header("X-Actor-Id", "viewer1")
                .header("X-Role", "NATIONAL_VIEWER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
