package zw.ac.uz.dpdms.alert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEvaluator evaluator;

    public AlertController(AlertEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    /**
     * POST /api/alerts/evaluate
     * Submits an approved incident for threshold evaluation.
     * Returns the list of alerts that were triggered (may be empty).
     */
    @PostMapping("/evaluate")
    public ResponseEntity<List<AlertLogEntry>> evaluate(@RequestBody IncidentPayload incident) {
        return ResponseEntity.ok(evaluator.evaluate(incident));
    }

    /** GET /api/alerts — retrieve the full alert log. */
    @GetMapping
    public ResponseEntity<List<AlertLogEntry>> getLog() {
        return ResponseEntity.ok(evaluator.getLog());
    }

    /** DELETE /api/alerts — clear the alert log (admin/testing only). */
    @DeleteMapping
    public ResponseEntity<Void> clear() {
        evaluator.clear();
        return ResponseEntity.noContent().build();
    }
}
