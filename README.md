# HelpDesk – IT Support Management System

React (Vite) · Spring Boot 3 · MySQL · JPA/Hibernate · Spring Security + JWT · Docker

## Run everything
```bash
docker compose up --build
```
- App: http://localhost:3000
- API: http://localhost:8080

## Run for development
```bash
# backend (needs MySQL on :3306, user root/root, or set DB_URL / DB_USER / DB_PASS)
cd backend && mvn spring-boot:run

# frontend
cd frontend && cp .env.example .env && npm install && npm run dev   # http://localhost:5173
```

## Seeded accounts
| Role | Email | Password |
|---|---|---|
| Admin | admin@helpdesk.com | admin123 |
| Support | support@helpdesk.com | support123 |

New sign-ups are Employees. Admins change roles in Admin > Users. Change `JWT_SECRET` and the seeded passwords before deploying.

## What each role can do
- **Employee**: create tickets, see only their own, comment, close their own.
- **Support**: see unassigned + own tickets, pick up unassigned ones, change status, comment, view analytics.
- **Admin**: everything, plus assign tickets, manage users and roles, edit SLA rules.

## API
| Method | Path | Notes |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | returns `{ token, ... }` |
| POST/GET | /api/tickets | create / list (scoped by role) |
| GET | /api/tickets/{id} | |
| PUT | /api/tickets/{id}/assign | admin, `{assigneeId}` |
| PUT | /api/tickets/{id}/claim | support |
| PUT | /api/tickets/{id}/status | `{status}` |
| GET/POST | /api/tickets/{id}/comments | |
| GET | /api/notifications, /unread-count; PUT /{id}/read, /read-all | |
| GET | /api/analytics/summary | admin, support |
| GET/PUT | /api/users, /api/users/{id}; GET /api/users/support | admin |
| GET/PUT | /api/sla, /api/sla/{priority} | PUT admin |

## SLA and escalation
A ticket's due time = creation time + resolution hours for its priority. A scheduler runs every minute: open/in-progress tickets past due are flagged as breached and admins (plus the assignee, or all support agents if unassigned) get a notification.
