# Architecture and implementation checklist

```text
React client -> API gateway -> hazard services (one database schema each)
                           -> auth service
                           -> dashboard / report services (approved records only)
Hazard services -> RabbitMQ -> alert service -> Email / WhatsApp providers
All services -> Eureka discovery; Actuator -> central logs and metrics
```

## Hazard-specific fields to implement

| Service | Required indicators |
| --- | --- |
| Flood | Peak water level, river basin, displaced households, flooded hectares, inundation days |
| Drought | Rainfall deficit, dry days, crop failure %, water-shortage population, livestock deaths |
| Fire | Burned hectares, cause, injuries/fatalities, destroyed structures, active/contained state |
| Zoonotic disease | Pathogen, affected species, human cases, animal cases, cluster/outbreak classification |
| Mining accident | Mine and type, accident type, trapped/injured miners, fatalities, rescue-operation state |

## Before production

1. Replace temporary request headers in `flood-service` with JWT authentication from `auth-service`; propagate only verified `role`, `hazard`, and `ward` claims through the gateway.
2. Replace the in-memory Flood store with PostgreSQL migrations, repositories, and a per-service schema. Apply the same workflow and audit trail to all hazard services.
3. Add authorization tests for cross-hazard access, ward mismatch, supervisor-only decisions, national read-only access, and approved-only dashboard/report queries.
4. Configure real email/WhatsApp provider credentials through environment variables and RabbitMQ retry/dead-letter queues.
