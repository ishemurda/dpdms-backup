package zw.ac.uz.dpdms.drought.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.drought.api.DroughtRequest;
import zw.ac.uz.dpdms.drought.domain.DroughtIncident;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DroughtWorkflowTest {
    private DroughtWorkflow workflow;
    private DroughtRequest request;

    @BeforeEach
    void setUp() {
        workflow = new DroughtWorkflow();
        var metadata = new IncidentMetadata("Ward 2", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_drought_w2", Severity.CRITICAL, -17.1, 32.4);
        request = new DroughtRequest(metadata, 120.5, 45, 65.0, 1500, 35);
    }

    @Test
    void recorderCanSubmitWithinAssignedWard() {
        var user = new UserClaim("recorder_drought_w2", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 2");
        DroughtIncident created = workflow.submit(request, user);

        assertNotNull(created.id());
        assertEquals(IncidentStatus.PENDING, created.status());
        assertEquals(120.5, created.rainfallDeficitMm());
        assertEquals(45, created.consecutiveDryDays());
        assertEquals(1, created.auditTrail().size());
    }

    @Test
    void recorderCannotSubmitWrongWardOrHazard() {
        var wrongWard = new UserClaim("recorder_drought_w2", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 1");
        assertThrows(ForbiddenException.class, () -> workflow.submit(request, wrongWard));

        var wrongHazard = new UserClaim("recorder_flood_w2", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 2");
        assertThrows(ForbiddenException.class, () -> workflow.submit(request, wrongHazard));
    }

    @Test
    void supervisorApprovesDroughtIncident() {
        var recorder = new UserClaim("recorder_drought_w2", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 2");
        var incident = workflow.submit(request, recorder);

        var supervisor = new UserClaim("supervisor_drought", Role.PROVINCIAL_SUPERVISOR, HazardType.DROUGHT, null);
        var approved = workflow.transition(incident.id(), IncidentStatus.APPROVED, supervisor, "Agrometeorological ground verification complete");

        assertEquals(IncidentStatus.APPROVED, approved.status());
        assertEquals(1, workflow.approved().size());
    }

    @Test
    void floodSupervisorCannotApproveDrought() {
        var recorder = new UserClaim("recorder_drought_w2", Role.WARD_RECORDER, HazardType.DROUGHT, "Ward 2");
        var incident = workflow.submit(request, recorder);

        var floodSupervisor = new UserClaim("supervisor_flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        assertThrows(ForbiddenException.class, () -> workflow.transition(incident.id(), IncidentStatus.APPROVED, floodSupervisor, "Approve"));
    }
}
