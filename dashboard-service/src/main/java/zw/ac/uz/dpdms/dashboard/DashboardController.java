package zw.ac.uz.dpdms.dashboard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardAggregator aggregator;

    public DashboardController(DashboardAggregator aggregator) {
        this.aggregator = aggregator;
    }

    /** GET /api/dashboard/summary — total approved counts by hazard. */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary() {
        return ResponseEntity.ok(aggregator.summarize());
    }

    /** GET /api/dashboard/map — lat/lon + hazard for all approved incidents. */
    @GetMapping("/map")
    public ResponseEntity<List<IncidentSummary>> map() {
        return ResponseEntity.ok(aggregator.fetchAllApproved());
    }

    /** GET /api/dashboard/recent — last 10 approved incidents. */
    @GetMapping("/recent")
    public ResponseEntity<List<IncidentSummary>> recent() {
        List<IncidentSummary> all = aggregator.fetchAllApproved();
        int from = Math.max(0, all.size() - 10);
        return ResponseEntity.ok(all.subList(from, all.size()));
    }
}
