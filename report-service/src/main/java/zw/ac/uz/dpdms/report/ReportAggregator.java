package zw.ac.uz.dpdms.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ReportAggregator {

    private static final Logger log = LoggerFactory.getLogger(ReportAggregator.class);
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${hazard.urls.flood}")    private String floodUrl;
    @Value("${hazard.urls.drought}")  private String droughtUrl;
    @Value("${hazard.urls.fire}")     private String fireUrl;
    @Value("${hazard.urls.zoonotic}") private String zoonoticUrl;
    @Value("${hazard.urls.mining}")   private String miningUrl;

    public List<ReportRow> fetchAll(String hazardFilter) {
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("Flood",            floodUrl    + "/api/floods/approved");
        endpoints.put("Drought",          droughtUrl  + "/api/droughts/approved");
        endpoints.put("Fire",             fireUrl     + "/api/fires/approved");
        endpoints.put("Zoonotic Disease", zoonoticUrl + "/api/zoonotic/approved");
        endpoints.put("Mining Accident",  miningUrl   + "/api/mining/approved");

        List<ReportRow> rows = new ArrayList<>();
        for (Map.Entry<String, String> entry : endpoints.entrySet()) {
            String hazard = entry.getKey();
            if (hazardFilter != null && !hazardFilter.isBlank() && !hazardFilter.equalsIgnoreCase(hazard)) continue;
            try {
                String json = rest.getForObject(entry.getValue(), String.class);
                JsonNode arr = mapper.readTree(json);
                if (arr.isArray()) {
                    for (JsonNode node : arr) rows.add(toRow(node, hazard));
                }
            } catch (Exception e) {
                log.warn("Could not fetch {} reports: {}", hazard, e.getMessage());
            }
        }
        return rows;
    }

    private ReportRow toRow(JsonNode node, String hazard) {
        JsonNode meta = node.path("metadata");
        Map<String, Object> fields = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> {
            if (!Set.of("id", "metadata", "status", "auditTrail").contains(e.getKey())) {
                fields.put(e.getKey(), e.getValue().asText());
            }
        });
        return new ReportRow(
                node.path("id").asText(),
                hazard,
                meta.path("ward").asText(),
                meta.path("district").asText(),
                meta.path("severity").asText(),
                meta.path("occurredAt").asText(),
                meta.path("latitude").asDouble(0),
                meta.path("longitude").asDouble(0),
                fields
        );
    }

    /** Renders a CSV byte array for the given rows. */
    public byte[] toCsv(List<ReportRow> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,hazard,ward,district,severity,occurredAt,latitude,longitude,hazardFields\n");
        for (ReportRow r : rows) {
            sb.append(escape(r.id())).append(',');
            sb.append(escape(r.hazard())).append(',');
            sb.append(escape(r.ward())).append(',');
            sb.append(escape(r.district())).append(',');
            sb.append(escape(r.severity())).append(',');
            sb.append(escape(r.occurredAt())).append(',');
            sb.append(r.latitude()).append(',');
            sb.append(r.longitude()).append(',');
            sb.append(escape(r.hazardFields().toString())).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String escape(String v) {
        if (v == null) return "\"\"";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
