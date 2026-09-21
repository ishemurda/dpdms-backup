package zw.ac.uz.dpdms.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardAggregator {

    private static final Logger log = LoggerFactory.getLogger(DashboardAggregator.class);
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${hazard.urls.flood}")
    private String floodUrl;
    @Value("${hazard.urls.drought}")
    private String droughtUrl;
    @Value("${hazard.urls.fire}")
    private String fireUrl;
    @Value("${hazard.urls.zoonotic}")
    private String zoonoticUrl;
    @Value("${hazard.urls.mining}")
    private String miningUrl;

    /** Map of hazard label → approved endpoint URL */
    private Map<String, String> endpointMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Flood",            floodUrl    + "/api/floods/approved");
        m.put("Drought",          droughtUrl  + "/api/droughts/approved");
        m.put("Fire",             fireUrl     + "/api/fires/approved");
        m.put("Zoonotic Disease", zoonoticUrl + "/api/zoonotic/approved");
        m.put("Mining Accident",  miningUrl   + "/api/mining/approved");
        return m;
    }

    public List<IncidentSummary> fetchAllApproved() {
        List<IncidentSummary> all = new ArrayList<>();
        for (Map.Entry<String, String> entry : endpointMap().entrySet()) {
            String hazard = entry.getKey();
            String url    = entry.getValue();
            try {
                String json = rest.getForObject(url, String.class);
                JsonNode arr = mapper.readTree(json);
                if (arr.isArray()) {
                    for (JsonNode node : arr) {
                        IncidentSummary s = toSummary(node, hazard);
                        if (s != null) all.add(s);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch approved incidents from {} ({}): {}", hazard, url, e.getMessage());
            }
        }
        return all;
    }

    public DashboardSummary summarize() {
        List<IncidentSummary> all = fetchAllApproved();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (IncidentSummary s : all) counts.merge(s.hazard(), 1, Integer::sum);
        List<HazardSummary> byHazard = counts.entrySet().stream()
                .map(e -> new HazardSummary(e.getKey(), e.getValue()))
                .toList();
        return new DashboardSummary(all.size(), byHazard, Instant.now());
    }

    private IncidentSummary toSummary(JsonNode node, String hazard) {
        try {
            JsonNode meta = node.path("metadata");
            return new IncidentSummary(
                    node.path("id").asText(),
                    hazard,
                    meta.path("ward").asText(),
                    meta.path("district").asText(),
                    meta.path("severity").asText(),
                    meta.path("latitude").asDouble(0),
                    meta.path("longitude").asDouble(0),
                    meta.path("occurredAt").asText()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
