package zw.ac.uz.dpdms.fire.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import zw.ac.uz.dpdms.common.IncidentMetadata;
import zw.ac.uz.dpdms.common.Severity;
import zw.ac.uz.dpdms.fire.service.FireWorkflow;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FireControllerTest {
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        FireWorkflow workflow = new FireWorkflow();
        FireController controller = new FireController(workflow);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void submitReturns201WhenAuthorized() throws Exception {
        var metadata = new IncidentMetadata("Ward 3", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_fire_w3", Severity.CRITICAL, -17.05, 32.55);
        var request = new FireRequest(metadata, 30.0, "Accidental", 0, 1, "active");

        mockMvc.perform(post("/api/fires")
                .header("X-Actor-Id", "recorder_fire_w3")
                .header("X-Role", "WARD_RECORDER")
                .header("X-Hazard", "FIRE")
                .header("X-Ward", "Ward 3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.areaBurnedHectares").value(30.0));
    }
}
