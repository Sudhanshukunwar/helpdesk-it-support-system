# HelpDesk – IT Support Management System

![CI](https://github.com/Sudhanshukunwar/helpdesk-it-support-system/actions/workflows/ci.yml/badge.svg)

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

## Security
- **Access tokens** are JWTs that expire after 15 minutes. They carry an issuer, a unique id, a type (`access`) and a token version.
- **Refresh tokens** are random, valid 7 days, stored only as SHA-256 hashes, and single-use. Each refresh issues a new one. If a used token appears again, all sessions of that user are revoked.
- The frontend refreshes silently on a 401 and retries the request once.
- **Instant revocation:** changing a password, "Sign out on all devices" and deactivating a user bump the user's token version, which invalidates every access token already issued.
- **Roles are read from the database** on each request, so role changes apply immediately.
- **Lockout:** 5 wrong passwords lock the account for 15 minutes (`app.security.*`).
- **Password rule:** 8+ characters with a letter and a number. Passwords are hashed with BCrypt.
- The app refuses to start if `JWT_SECRET` is shorter than 32 characters.

Endpoints: `POST /api/auth/refresh`, `POST /api/auth/logout`, `PUT /api/account/password`, `POST /api/account/sign-out-everywhere`.

## Ticket history
Every creation, assignment, pick-up, status change and SLA breach is stored in `ticket_history` (who, what, when) and shown as an Activity timeline on the ticket page. `GET /api/tickets/{id}/history` (same access rules as the ticket).

## Tests
```bash
cd backend && mvn test        # JWT, login lockout, refresh-token rotation, ticket access rules, SLA job
cd frontend && npm test       # UI helpers
```
GitHub Actions runs both on every push (`.github/workflows/ci.yml`).

## Deploying
See [DEPLOY.md](DEPLOY.md) for a step-by-step guide (VPS + Docker + automatic HTTPS) and `docker-compose.prod.yml`.
