package zw.ac.uz.dpdms.fire.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.fire.api.FireRequest;
import zw.ac.uz.dpdms.fire.domain.FireIncident;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FireWorkflowTest {
    private FireWorkflow workflow;
    private FireRequest request;

    @BeforeEach
    void setUp() {
        workflow = new FireWorkflow();
        var metadata = new IncidentMetadata("Ward 3", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_fire_w3", Severity.CRITICAL, -17.05, 32.55);
        request = new FireRequest(metadata, 45.0, "Veld fire", 2, 3, "active");
    }

    @Test
    void recorderSubmitsFireWithinAssignedWard() {
        var user = new UserClaim("recorder_fire_w3", Role.WARD_RECORDER, HazardType.FIRE, "Ward 3");
        FireIncident created = workflow.submit(request, user);

        assertNotNull(created.id());
        assertEquals(IncidentStatus.PENDING, created.status());
        assertEquals(45.0, created.areaBurnedHectares());
        assertEquals("active", created.fireState());
    }

    @Test
    void recorderCannotSubmitOutsideWard() {
        var wrongWard = new UserClaim("recorder_fire_w3", Role.WARD_RECORDER, HazardType.FIRE, "Ward 1");
        assertThrows(ForbiddenException.class, () -> workflow.submit(request, wrongWard));
    }

    @Test
    void fireSupervisorApprovesIncident() {
        var recorder = new UserClaim("recorder_fire_w3", Role.WARD_RECORDER, HazardType.FIRE, "Ward 3");
        var incident = workflow.submit(request, recorder);

        var supervisor = new UserClaim("supervisor_fire", Role.PROVINCIAL_SUPERVISOR, HazardType.FIRE, null);
        var approved = workflow.transition(incident.id(), IncidentStatus.APPROVED, supervisor, "Forestry Commission ground team on site");

        assertEquals(IncidentStatus.APPROVED, approved.status());
        assertEquals(1, workflow.approved().size());
    }

    @Test
    void miningSupervisorCannotApproveFire() {
        var recorder = new UserClaim("recorder_fire_w3", Role.WARD_RECORDER, HazardType.FIRE, "Ward 3");
        var incident = workflow.submit(request, recorder);

        var miningSupervisor = new UserClaim("supervisor_mining", Role.PROVINCIAL_SUPERVISOR, HazardType.MINING_ACCIDENT, null);
        assertThrows(ForbiddenException.class, () -> workflow.transition(incident.id(), IncidentStatus.APPROVED, miningSupervisor, "Approve"));
    }
}
