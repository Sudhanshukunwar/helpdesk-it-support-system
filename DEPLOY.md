# Deploying HelpDesk

This guide uses one small Linux server (a VPS) running Docker. Caddy gets a free HTTPS certificate automatically.

## 1. What you need
- A Linux VPS (Ubuntu 22.04 or 24.04) with at least 2 GB RAM, from any provider.
- A domain name, or no domain at all (see "No domain?" below).
- Your code on GitHub.

## 2. Point your domain at the server
Create an **A record** for your domain (for example `helpdesk.yourname.com`) with the server's public IP.

**No domain?** Use a free wildcard DNS name that contains the IP, e.g. for IP `203.0.113.5` use `203-0-113-5.sslip.io` as `DOMAIN`. Caddy can still get a certificate for it.

## 3. Install Docker on the server
```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # log out and back in after this
```

## 4. Open the firewall
Allow ports 22, 80 and 443 only (in your provider's firewall, and/or `sudo ufw allow 22,80,443/tcp && sudo ufw enable`).

## 5. Get the code and configure it
```bash
git clone https://github.com/Sudhanshukunwar/helpdesk-it-support-system.git
cd helpdesk-it-support-system
cp .env.example .env
nano .env
```
Fill in `.env`:
- `DOMAIN`: your domain
- `DB_PASSWORD`: a long random password
- `JWT_SECRET`: 48+ random characters (`openssl rand -base64 48`)
- `ADMIN_EMAIL` / `ADMIN_PASSWORD`: your real admin login (8+ characters, with a letter and a number)

## 6. Start it
```bash
docker compose -f docker-compose.prod.yml up -d --build
```
The first build takes a few minutes. Then open `https://YOUR_DOMAIN` and sign in with the admin email and password from `.env`.

Useful commands:
```bash
docker compose -f docker-compose.prod.yml logs -f backend   # watch the backend
docker compose -f docker-compose.prod.yml ps                # status
docker compose -f docker-compose.prod.yml down              # stop (data is kept)
```

## 7. Update after you push new code
```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

## 8. Back up the database
```bash
docker compose -f docker-compose.prod.yml exec db sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" helpdesk' > backup.sql
```

## What is different from local
| | Local | Production |
|---|---|---|
| Database port | published on 3307 | not exposed |
| Backend port | published on 8080 | not exposed, only reachable through Caddy |
| HTTPS | no | yes, automatic |
| Demo users | `admin123`, `support123` | only your admin from `.env` |
| Demo JWT secret | allowed (warning) | refused: the app will not start |
| API URL in the frontend | `localhost:8080` | same origin (`/api`) |

## Other hosts
Platforms such as Render, Railway or Fly.io can also run the two Dockerfiles, but you then need a managed MySQL database and must set the same environment variables on the backend (`DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `CORS_ORIGIN` = your frontend URL, `APP_PRODUCTION=true`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `SEED_DEMO_SUPPORT=false`) and build the frontend with `VITE_API_URL` set to the backend's public URL. Check each platform's current free-tier limits before choosing.
