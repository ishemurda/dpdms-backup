package zw.ac.uz.dpdms.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AlertEvaluatorTest {

    private AlertEvaluator evaluator;

    @BeforeEach
    void setUp() { evaluator = new AlertEvaluator(); }

    @Test
    void criticalSeverityTriggersAlert() {
        IncidentPayload p = new IncidentPayload("id1", "Flood", "Ward 1", "CRITICAL", Map.of());
        List<AlertLogEntry> alerts = evaluator.evaluate(p);
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.stream().anyMatch(a -> a.reason().contains("CRITICAL")));
    }

    @Test
    void floodAboveThresholdTriggersAlert() {
        IncidentPayload p = new IncidentPayload("id2", "Flood", "Ward 2", "HIGH",
                Map.of("peakWaterLevelMetres", "4.5"));
        List<AlertLogEntry> alerts = evaluator.evaluate(p);
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.stream().anyMatch(a -> a.reason().contains("3 m")));
    }

    @Test
    void floodBelowThresholdNoAlert() {
        IncidentPayload p = new IncidentPayload("id3", "Flood", "Ward 3", "LOW",
                Map.of("peakWaterLevelMetres", "1.2"));
        List<AlertLogEntry> alerts = evaluator.evaluate(p);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void activeFireTriggersAlert() {
        IncidentPayload p = new IncidentPayload("id4", "Fire", "Ward 4", "HIGH",
                Map.of("fireState", "Still active"));
        List<AlertLogEntry> alerts = evaluator.evaluate(p);
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.stream().anyMatch(a -> a.reason().contains("ACTIVE")));
    }

    @Test
    void miningFatalitiesTriggersAlert() {
        IncidentPayload p = new IncidentPayload("id5", "Mining Accident", "Ward 5", "HIGH",
                Map.of("fatalities", "3"));
        List<AlertLogEntry> alerts = evaluator.evaluate(p);
        assertFalse(alerts.isEmpty());
        assertTrue(alerts.stream().anyMatch(a -> a.reason().contains("fatality")));
    }

    @Test
    void logAccumulatesAcrossEvaluations() {
        evaluator.evaluate(new IncidentPayload("a", "Fire", "W", "CRITICAL", Map.of()));
        evaluator.evaluate(new IncidentPayload("b", "Flood", "W", "HIGH", Map.of("peakWaterLevelMetres", "5")));
        assertTrue(evaluator.getLog().size() >= 2);
    }
}
