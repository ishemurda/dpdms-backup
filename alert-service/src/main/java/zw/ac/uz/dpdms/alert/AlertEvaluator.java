package zw.ac.uz.dpdms.alert;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Evaluates an approved incident against configured thresholds and appends matching alerts
 * to the in-memory log.
 */
@Service
public class AlertEvaluator {

    private final CopyOnWriteArrayList<AlertLogEntry> log = new CopyOnWriteArrayList<>();

    public List<AlertLogEntry> evaluate(IncidentPayload incident) {
        List<AlertLogEntry> triggered = new ArrayList<>();
        Map<String, Object> fields = incident.fields() != null ? incident.fields() : Map.of();

        // Rule 1: CRITICAL severity always triggers
        if ("CRITICAL".equalsIgnoreCase(incident.severity())) {
            triggered.add(new AlertLogEntry(
                    incident.id(), incident.hazard(), incident.ward(),
                    incident.severity(), "CRITICAL severity incident reported", Instant.now()));
        }

        switch (incident.hazard() != null ? incident.hazard().toUpperCase() : "") {
            case "FLOOD" -> {
                Object level = fields.get("peakWaterLevelMetres");
                if (level != null) {
                    try {
                        double metres = Double.parseDouble(level.toString());
                        if (metres >= 3.0) triggered.add(new AlertLogEntry(
                                incident.id(), incident.hazard(), incident.ward(),
                                incident.severity(),
                                "Flood peak water level %.1f m exceeds 3 m threshold".formatted(metres),
                                Instant.now()));
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "FIRE" -> {
                Object state = fields.get("fireState");
                if (state != null && state.toString().toLowerCase().contains("active")) {
                    triggered.add(new AlertLogEntry(
                            incident.id(), incident.hazard(), incident.ward(),
                            incident.severity(), "Fire is still ACTIVE", Instant.now()));
                }
            }
            case "ZOONOTIC DISEASE", "ZOONOTIC_DISEASE" -> {
                Object cls = fields.get("classification");
                if (cls != null && cls.toString().toLowerCase().contains("outbreak")) {
                    triggered.add(new AlertLogEntry(
                            incident.id(), incident.hazard(), incident.ward(),
                            incident.severity(), "Zoonotic disease classified as OUTBREAK", Instant.now()));
                }
            }
            case "MINING ACCIDENT", "MINING_ACCIDENT" -> {
                Object fatalities = fields.get("fatalities");
                if (fatalities != null) {
                    try {
                        int f = Integer.parseInt(fatalities.toString());
                        if (f > 0) triggered.add(new AlertLogEntry(
                                incident.id(), incident.hazard(), incident.ward(),
                                incident.severity(),
                                "%d fatality(ies) reported in mining accident".formatted(f),
                                Instant.now()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        log.addAll(triggered);
        return triggered;
    }

    public List<AlertLogEntry> getLog() {
        return Collections.unmodifiableList(log);
    }

    public void clear() {
        log.clear();
    }
}
