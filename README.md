# Time Keeper

A small web app for tracking working time, replacing the spreadsheet workflow.

- Log time entries (`from` → `to`) and get the duration computed automatically.
- Group entries per day with day-level and per-entry notes (ticket IDs, descriptions).
- See totals per day and per week (Monday–Sunday) in decimal hours (e.g. `2.92 h`).
- Manage work types/categories (`Isakr`, `Sales`, `Other`, `Teamlead` by default).

## Stack

| Part     | Technology                                      |
| -------- | ----------------------------------------------- |
| Backend  | Kotlin, Spring Boot 3, Spring Data JPA, Flyway  |
| Database | PostgreSQL 16                                   |
| Frontend | Vue 3, TypeScript, Vite                          |
| Runtime  | Docker / Docker Compose (Kubernetes-ready)      |

## Run with Docker

```bash
docker compose up --build
```

Then open:

- Frontend: <http://localhost:3000>
- Backend API: <http://localhost:8080/api>

The frontend (nginx) proxies `/api` to the backend, so no CORS setup is needed in production.

To stop:

```bash
docker compose down
```

To also wipe the database:

```bash
docker compose down -v
```

## Deploy to Kubernetes (Rancher Desktop)

Manifests live in `kubernetes/` (namespace `timekeeper`, Postgres with a
`local-path` PVC, backend, and a NodePort frontend). Build the images locally
first so the cluster can use them from the shared Docker image store:

```bash
docker build -t timekeeper-backend:latest ./backend
docker build -t timekeeper-frontend:latest ./frontend
kubectl apply -f kubernetes/
```

Open <http://localhost:30080>. The backend stays a `ClusterIP` service; for
direct API access use `kubectl port-forward -n timekeeper svc/backend 8080:8080`.

`docker compose` and these manifests use the same `timekeeper-*:latest` image
tags. After rebuilding an image, recreate its pod so the kubelet resolves the
new build (with a mutable tag it may keep the cached one):

```bash
kubectl -n timekeeper delete pod -l app=frontend
kubectl -n timekeeper delete pod -l app=backend
```

To carry over data from the docker-compose database:

```bash
docker exec timekeeper-db pg_dump -U timekeeper -d timekeeper \
  --data-only --no-owner -t work_day > /tmp/work_day.sql
docker exec timekeeper-db pg_dump -U timekeeper -d timekeeper \
  --data-only --no-owner -t time_entry > /tmp/time_entry.sql
{ echo "TRUNCATE work_day RESTART IDENTITY CASCADE;"; cat /tmp/work_day.sql /tmp/time_entry.sql; } \
  | kubectl -n timekeeper exec -i deploy/db -- psql -U timekeeper -d timekeeper
```

To remove the deployment (the PVC/data is deleted with it):

```bash
kubectl delete namespace timekeeper
```

## Local development

Start just the database:

```bash
docker compose up -d db
```

Backend (needs a JDK 21 toolchain):

```bash
cd backend
./gradlew bootRun
```

Frontend (needs Node 22+):

```bash
cd frontend
npm install
npm run dev
```

Vite runs on <http://localhost:5173> and proxies `/api` to `http://localhost:8080`.

## API

| Method | Path                                        | Description                          |
| ------ | ------------------------------------------- | ------------------------------------ |
| GET    | `/api/week?date=YYYY-MM-DD`                 | Week (Mon–Sun) containing the date   |
| GET    | `/api/days/{date}`                          | One day with entries and totals      |
| PUT    | `/api/days/{date}`                          | Update day notes                     |
| POST   | `/api/days/{date}/entries`                  | Add a time entry to a day            |
| PUT    | `/api/days/{date}/entries/{entryId}`        | Update a time entry                  |
| DELETE | `/api/days/{date}/entries/{entryId}`        | Delete a time entry                  |
| GET    | `/api/categories`                           | List categories                      |
| POST   | `/api/categories`                           | Create a category                    |
| PUT    | `/api/categories/{id}`                      | Rename / recolor a category          |
| DELETE | `/api/categories/{id}`                      | Delete a category                    |

Example entry payload:

```json
{
  "startTime": "09:40",
  "endTime": "12:35",
  "categoryId": 1,
  "notes": "ISAKRDEV-5215"
}
```

## Project layout

```
backend/   Spring Boot + Kotlin service, Flyway migrations, Dockerfile
frontend/  Vue 3 + TypeScript app, nginx config, Dockerfile
docker-compose.yml
```

## Configuration

The backend reads configuration from environment variables (with sane local defaults):

| Variable                    | Default                                              |
| --------------------------- | ---------------------------------------------------- |
| `SPRING_DATASOURCE_URL`     | `jdbc:postgresql://localhost:5432/timekeeper`         |
| `SPRING_DATASOURCE_USERNAME`| `timekeeper`                                         |
| `SPRING_DATASOURCE_PASSWORD`| `timekeeper`                                         |
| `SERVER_PORT`               | `8080`                                               |

## Importing from Apple Numbers

The importer reads the monthly sheets of a `.numbers` workbook and loads the
entries through the API (safe to re-run; existing entries are skipped).

```bash
python -m venv .venv && source .venv/bin/activate
pip install numbers-parser requests

python scripts/import_numbers.py \
  --file ~/Desktop/Timekeeping.numbers \
  --sheets "September 2026" "October 2026" "November 2026" \
  --dry-run          # remove --dry-run to write the data
```

It handles the spreadsheet's quirks: blank `Date` cells inherit the previous
date, `From`/`To` date parts are ignored (only the time-of-day is used), and
`Details` become day-level notes. Summary/pivot tables are skipped. The
`--dry-run` mode also reports any day whose imported total disagrees with the
sheet's own "Total per day" cell.

## Next steps (suggested)

- Kubernetes manifests (Deployment + Service + ConfigMap/Secret) for backend and frontend.
- Import the existing Numbers spreadsheet data.
- Reporting/export (CSV/PDF) for billing.
