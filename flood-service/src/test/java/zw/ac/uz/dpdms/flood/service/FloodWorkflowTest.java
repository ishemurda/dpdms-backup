package zw.ac.uz.dpdms.flood.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.flood.api.FloodRequest;
import zw.ac.uz.dpdms.flood.domain.FloodIncident;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FloodWorkflowTest {

    private FloodWorkflow workflow;
    private IncidentMetadata metadata;
    private FloodRequest request;

    @BeforeEach
    void setUp() {
        workflow = new FloodWorkflow();
        metadata = new IncidentMetadata(
            "Ward 1",
            "Rushinga",
            "Mashonaland Central",
            Instant.now(),
            "recorder_flood_w1",
            Severity.HIGH,
            -17.2,
            32.3
        );
        request = new FloodRequest(metadata, 2.5, "Mazowe", 10, 25.0, 3);
    }

    @Test
    void recorderCannotSubmitOutsideAssignedWard() {
        assertThrows(
            zw.ac.uz.dpdms.common.ForbiddenException.class,
            () -> workflow.submit(request, "recorder_flood_w1", "Ward 2")
        );
    }

    @Test
    void recorderCannotSubmitDifferentHazard() {
        UserClaim wrongHazardClaim = new UserClaim("recorder", Role.WARD_RECORDER, HazardType.FIRE, "Ward 1");
        assertThrows(
            zw.ac.uz.dpdms.common.ForbiddenException.class,
            () -> workflow.submit(request, wrongHazardClaim)
        );
    }

    @Test
    void nationalViewerCannotSubmitIncidents() {
        UserClaim nationalClaim = new UserClaim("national_user", Role.NATIONAL_VIEWER, null, null);
        assertThrows(
            zw.ac.uz.dpdms.common.ForbiddenException.class,
            () -> workflow.submit(request, nationalClaim)
        );
    }

    @Test
    void successfulSubmissionEntersPendingWithAuditTrail() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        assertNotNull(created.id());
        assertEquals(IncidentStatus.PENDING, created.status());
        assertEquals(1, created.auditTrail().size());
        assertEquals("Submitted for approval", created.auditTrail().get(0).reason());
        assertEquals("recorder_flood_w1", created.auditTrail().get(0).actorId());
    }

    @Test
    void supervisorApprovesPendingIncident() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        UserClaim supervisorClaim = new UserClaim("supervisor_flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        FloodIncident approved = workflow.transition(created.id(), IncidentStatus.APPROVED, supervisorClaim, "Verified with local river gauge");

        assertEquals(IncidentStatus.APPROVED, approved.status());
        assertEquals(2, approved.auditTrail().size());
        assertEquals(IncidentStatus.APPROVED, approved.auditTrail().get(1).to());
        assertEquals("Verified with local river gauge", approved.auditTrail().get(1).reason());

        // Approved incident appears in approved() feed
        assertEquals(1, workflow.approved().size());
    }

    @Test
    void supervisorFromDifferentHazardCannotDecide() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        UserClaim fireSupervisor = new UserClaim("supervisor_fire", Role.PROVINCIAL_SUPERVISOR, HazardType.FIRE, null);
        assertThrows(
            zw.ac.uz.dpdms.common.ForbiddenException.class,
            () -> workflow.transition(created.id(), IncidentStatus.APPROVED, fireSupervisor, "Approve")
        );
    }

    @Test
    void terminalIncidentCannotBeTransitionedAgain() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        UserClaim supervisorClaim = new UserClaim("supervisor_flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        workflow.transition(created.id(), IncidentStatus.APPROVED, supervisorClaim, "Approved");

        assertThrows(
            IllegalStateException.class,
            () -> workflow.transition(created.id(), IncidentStatus.REJECTED, supervisorClaim, "Reject")
        );
    }

    @Test
    void recorderCanUpdateWhenCorrectionRequested() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        UserClaim supervisorClaim = new UserClaim("supervisor_flood", Role.PROVINCIAL_SUPERVISOR, HazardType.FLOOD, null);
        workflow.transition(created.id(), IncidentStatus.CORRECTION_REQUESTED, supervisorClaim, "Check water level");

        FloodRequest updatedReq = new FloodRequest(metadata, 3.1, "Mazowe", 12, 28.0, 4);
        FloodIncident updated = workflow.update(created.id(), updatedReq, recorderClaim);

        assertEquals(3.1, updated.peakWaterLevelMetres());
        assertEquals(IncidentStatus.PENDING, updated.status());
        assertEquals(3, updated.auditTrail().size());
    }

    @Test
    void recorderCanDeletePendingIncident() {
        UserClaim recorderClaim = new UserClaim("recorder_flood_w1", Role.WARD_RECORDER, HazardType.FLOOD, "Ward 1");
        FloodIncident created = workflow.submit(request, recorderClaim);

        workflow.delete(created.id(), recorderClaim);
        assertThrows(java.util.NoSuchElementException.class, () -> workflow.get(created.id()));
    }
}
