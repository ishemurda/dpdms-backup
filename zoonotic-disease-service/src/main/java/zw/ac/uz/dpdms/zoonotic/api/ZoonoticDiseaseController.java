package zw.ac.uz.dpdms.zoonotic.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.ac.uz.dpdms.common.*;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;
import zw.ac.uz.dpdms.zoonotic.service.ZoonoticDiseaseWorkflow;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/zoonotic-diseases")
public class ZoonoticDiseaseController {
    private final ZoonoticDiseaseWorkflow workflow;

    public ZoonoticDiseaseController(ZoonoticDiseaseWorkflow workflow) {
        this.workflow = workflow;
    }

    @PostMapping
    public ResponseEntity<ZoonoticDiseaseIncident> submit(
            @Valid @RequestBody ZoonoticDiseaseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        return ResponseEntity.status(HttpStatus.CREATED).body(workflow.submit(request, user));
    }

    @GetMapping
    public List<ZoonoticDiseaseIncident> list(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        return workflow.list(user);
    }

    @GetMapping("/approved")
    public List<ZoonoticDiseaseIncident> approved() {
        return workflow.approved();
    }

    @GetMapping("/{id}")
    public ZoonoticDiseaseIncident get(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        return workflow.get(id, user);
    }

    @PutMapping("/{id}")
    public ZoonoticDiseaseIncident update(
            @PathVariable UUID id,
            @Valid @RequestBody ZoonoticDiseaseRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        return workflow.update(id, request, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        workflow.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/decision")
    public ZoonoticDiseaseIncident decide(
            @PathVariable UUID id,
            @RequestParam IncidentStatus status,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Actor-Id", required = false) String actor,
            @RequestHeader(value = "X-Role", required = false) String role,
            @RequestHeader(value = "X-Hazard", required = false) String hazard,
            @RequestHeader(value = "X-Ward", required = false) String ward) {
        UserClaim user = SecurityUtils.resolveUser(authHeader, actor, role, hazard, ward);
        return workflow.transition(id, status, user, reason);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
