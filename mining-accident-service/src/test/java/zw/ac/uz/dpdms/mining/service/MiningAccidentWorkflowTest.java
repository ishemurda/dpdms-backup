package zw.ac.uz.dpdms.mining.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.mining.api.MiningAccidentRequest;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MiningAccidentWorkflowTest {
    private MiningAccidentWorkflow workflow;
    private MiningAccidentRequest request;

    @BeforeEach
    void setUp() {
        workflow = new MiningAccidentWorkflow();
        var metadata = new IncidentMetadata("Ward 5", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_mining_w5", Severity.CRITICAL, -16.95, 32.7);
        request = new MiningAccidentRequest(metadata, "Mazowe Artisanal Pit 4", "collapse", 4, 1, "ongoing");
    }

    @Test
    void recorderSubmitsMiningAccidentInWard() {
        var user = new UserClaim("recorder_mining_w5", Role.WARD_RECORDER, HazardType.MINING_ACCIDENT, "Ward 5");
        MiningAccidentIncident created = workflow.submit(request, user);

        assertNotNull(created.id());
        assertEquals(IncidentStatus.PENDING, created.status());
        assertEquals("collapse", created.accidentType());
        assertEquals(4, created.trappedOrInjured());
        assertEquals(1, created.fatalities());
    }

    @Test
    void recorderCannotSubmitWrongWard() {
        var wrongWard = new UserClaim("recorder_mining_w5", Role.WARD_RECORDER, HazardType.MINING_ACCIDENT, "Ward 2");
        assertThrows(ForbiddenException.class, () -> workflow.submit(request, wrongWard));
    }

    @Test
    void supervisorApprovesMiningAccident() {
        var recorder = new UserClaim("recorder_mining_w5", Role.WARD_RECORDER, HazardType.MINING_ACCIDENT, "Ward 5");
        var incident = workflow.submit(request, recorder);

        var supervisor = new UserClaim("supervisor_mining", Role.PROVINCIAL_SUPERVISOR, HazardType.MINING_ACCIDENT, null);
        var approved = workflow.transition(incident.id(), IncidentStatus.APPROVED, supervisor, "Mines inspector report verified");

        assertEquals(IncidentStatus.APPROVED, approved.status());
        assertEquals(1, workflow.approved().size());
    }
}
