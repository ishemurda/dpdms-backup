package zw.ac.uz.dpdms.zoonotic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.zoonotic.api.ZoonoticDiseaseRequest;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ZoonoticDiseaseWorkflowTest {
    private ZoonoticDiseaseWorkflow workflow;
    private ZoonoticDiseaseRequest request;

    @BeforeEach
    void setUp() {
        workflow = new ZoonoticDiseaseWorkflow();
        var metadata = new IncidentMetadata("Ward 4", "Rushinga", "Mashonaland Central", Instant.now(), "recorder_zoonotic_w4", Severity.HIGH, -17.0, 32.6);
        request = new ZoonoticDiseaseRequest(metadata, "Anthrax", "Cattle", 3, 18, "outbreak");
    }

    @Test
    void recorderSubmitsZoonoticIncidentWithinWard() {
        var user = new UserClaim("recorder_zoonotic_w4", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 4");
        ZoonoticDiseaseIncident created = workflow.submit(request, user);

        assertNotNull(created.id());
        assertEquals(IncidentStatus.PENDING, created.status());
        assertEquals("Anthrax", created.pathogen());
        assertEquals(3, created.humanCases());
        assertEquals(18, created.animalCases());
    }

    @Test
    void recorderCannotSubmitWrongWard() {
        var wrongWard = new UserClaim("recorder_zoonotic_w4", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 2");
        assertThrows(ForbiddenException.class, () -> workflow.submit(request, wrongWard));
    }

    @Test
    void supervisorApprovesZoonoticIncident() {
        var recorder = new UserClaim("recorder_zoonotic_w4", Role.WARD_RECORDER, HazardType.ZOONOTIC_DISEASE, "Ward 4");
        var incident = workflow.submit(request, recorder);

        var supervisor = new UserClaim("supervisor_zoonotic", Role.PROVINCIAL_SUPERVISOR, HazardType.ZOONOTIC_DISEASE, null);
        var approved = workflow.transition(incident.id(), IncidentStatus.APPROVED, supervisor, "Veterinary Department lab results verified");

        assertEquals(IncidentStatus.APPROVED, approved.status());
        assertEquals(1, workflow.approved().size());
    }
}
