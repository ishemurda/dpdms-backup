package zw.ac.uz.dpdms.drought.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.Severity;
import zw.ac.uz.dpdms.drought.service.DroughtWorkflow;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DroughtControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        DroughtWorkflow workflow = new DroughtWorkflow();
        DroughtController controller = new DroughtController(workflow);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void submitReturns201WhenAuthorized() throws Exception {
        var metadata = new IncidentMetadata("Ward 2", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_drought_w2", Severity.HIGH, -17.1, 32.4);
        var request = new DroughtRequest(metadata, 105.0, 30, 50.0, 800, 15);

        mockMvc.perform(post("/api/droughts")
                .header("X-Actor-Id", "recorder_drought_w2")
                .header("X-Role", "WARD_RECORDER")
                .header("X-Hazard", "DROUGHT")
                .header("X-Ward", "Ward 2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.rainfallDeficitMm").value(105.0));
    }

    @Test
    void submitReturns403WhenHazardMismatched() throws Exception {
        var metadata = new IncidentMetadata("Ward 2", "Rushinga", "Mashonaland Central", Instant.now(), "recorder1", Severity.HIGH, -17.1, 32.4);
        var request = new DroughtRequest(metadata, 105.0, 30, 50.0, 800, 15);

        mockMvc.perform(post("/api/droughts")
                .header("X-Actor-Id", "recorder1")
                .header("X-Role", "WARD_RECORDER")
                .header("X-Hazard", "FLOOD")
                .header("X-Ward", "Ward 2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
