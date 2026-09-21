# DPDMS starter project

The Rushinga Provincial Disaster Monitoring and Management System is structured as independently deployable Spring Boot services with React at the edge. React was chosen for reusable dashboard/map components and a responsive role-aware interface.

## Services

| Service | Responsibility |
| --- | --- |
| `discovery-service` | Eureka registry |
| `gateway-service` | Single API entry point and token routing |
| `auth-service` | Identity, JWT issuance and role/ward/hazard claims |
| `flood-service`, `drought-service`, `fire-service`, `zoonotic-disease-service`, `mining-accident-service` | Isolated incident ownership and approval workflows |
| `report-service` | Approved-data exports (PDF, DOCX, XLSX, CSV) |
| `alert-service` | Asynchronous email/WhatsApp alert dispatch and log |
| `dashboard-service` | Approved incident aggregation and map feed |

Each service owns a PostgreSQL schema and exposes REST endpoints. Synchronous calls go through HTTP; alert events are designed for RabbitMQ. Credentials belong in environment variables, never source control.

## Approval and access rules

New incidents begin `PENDING`. The relevant provincial supervisor alone can approve, reject, or request correction; every transition produces an audit record. Public/dashboard/report queries must filter to `APPROVED`. Backend authorization is based on JWT claims: ward recorders have one `(ward, hazard)` pair, supervisors one hazard, and national viewers read approved incidents only.

## Start locally

1. Copy `.env.example` to `.env` and set secrets.
2. Run `docker compose up -d` for PostgreSQL and RabbitMQ.
3. Start discovery, gateway, then individual services with `mvn -pl <service> spring-boot:run`.
4. In `frontend`, run `npm install && npm run dev`, then open `http://127.0.0.1:5173/`. Do not open `frontend/index.html` directly: it is TypeScript source and must be served by Vite. For a portable static preview, run `npm run build` and open `frontend/dist/index.html`.

The flood service is the reference vertical slice; clone its package pattern when implementing the remaining hazard indicators. See `docs/ARCHITECTURE.md` for the implementation checklist and security hardening work still required.
