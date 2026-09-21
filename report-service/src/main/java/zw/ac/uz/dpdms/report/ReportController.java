package zw.ac.uz.dpdms.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportAggregator aggregator;

    public ReportController(ReportAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * GET /api/reports/json?hazard=Flood
     * Returns approved incident data as JSON (all hazards or filtered).
     */
    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ReportRow>> jsonReport(
            @RequestParam(required = false) String hazard) {
        return ResponseEntity.ok(aggregator.fetchAll(hazard));
    }

    /**
     * GET /api/reports/csv?hazard=Flood
     * Downloads a CSV file of approved incidents.
     */
    @GetMapping("/csv")
    public ResponseEntity<byte[]> csvReport(
            @RequestParam(required = false) String hazard) {
        List<ReportRow> rows = aggregator.fetchAll(hazard);
        byte[] csv = aggregator.toCsv(rows);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dpdms-approved-incidents.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
