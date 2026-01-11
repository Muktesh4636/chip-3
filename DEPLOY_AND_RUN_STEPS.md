# Run & Deploy — Full Steps

This document contains full, copy/paste-ready steps for running the project locally on the server and deploying it to the internet (production) with Gunicorn + systemd + nginx. It also includes safe backup steps, migration troubleshooting, sample systemd and nginx snippets, and rollback instructions.

Paths and quick assumptions
- Repository root: `/root/Chips_dashboard`
- Django project root (contains `manage.py`): `/root/Chips_dashboard/chip_dashboard`
- Virtualenv (project): `/root/Chips_dashboard/chip_dashboard/myenv`
- Systemd service name (example in this repo): `broker_portal`
- Production Gunicorn bind used here: `0.0.0.0:8001`
- Domain used in examples: `pravoo.in` and server IP `72.61.148.117`

If your paths or service names differ, adjust the commands accordingly.

---

## Part A — Run updated code locally (development)
Purpose: validate code changes, run quick tests, and apply migrations to a local SQLite database (dev fallback).

1) Pull latest code and confirm branch
```bash
cd /root/Chips_dashboard
git pull origin main
git status --porcelain --branch
```
Why: Ensure you're running the latest code and see any local changes.

2) Activate virtualenv (interactive) or use venv python directly
```bash
source /root/Chips_dashboard/chip_dashboard/myenv/bin/activate
# or run manage.py with full venv python:
# /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py <command>
```
Why: Use the same Python and packages that the app uses.

3) Install/update dependencies (if requirements changed)
```bash
cd /root/Chips_dashboard
/root/Chips_dashboard/chip_dashboard/myenv/bin/pip install -r requirements.txt
```
Why: Adds any newly-introduced libraries.

4) Back up the local DB (always)
```bash
cd /root/Chips_dashboard/chip_dashboard
[ -f db.sqlite3 ] && cp db.sqlite3 db.sqlite3.bak.$(date +%Y%m%d%H%M)
```
Why: Protects your data before running migrations.

5) Run migrations (SQLite dev fallback)
```bash
cd /root/Chips_dashboard/chip_dashboard
USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py migrate --noinput
```
If migrations fail with a `ValueError` about a lazy reference (e.g., `core.customuser`), see Troubleshooting below.

6) Start the development server (quick test)
```bash
cd /root/Chips_dashboard/chip_dashboard
USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py runserver 0.0.0.0:8000
```
- For a background run:
```bash
USE_SQLITE=1 nohup /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py runserver 0.0.0.0:8000 &> /tmp/django-run.log &
```
Why: Quick manual testing. Do not use `runserver` for production.

7) Test locally
```bash
curl -I http://127.0.0.1:8000/
```

---

## Part B — Deploy updated code to internet (production)
Purpose: update the live site to the new code while keeping it always-up using systemd + Gunicorn + nginx.

Preflight checklist
- You have sudo privileges to reload systemd, restart services, and modify nginx.
- The systemd unit `broker_portal` exists (or you'll create it) and points to your venv python & gunicorn binary.
- Nginx site config proxies the domain to `127.0.0.1:8001` (or your chosen port).

1) Pull latest code & install dependencies
```bash
cd /root/Chips_dashboard
git pull origin main
/root/Chips_dashboard/chip_dashboard/myenv/bin/pip install -r requirements.txt
```

2) Back up the production database (CRITICAL)
- If using sqlite (not recommended in prod):
```bash
cd /root/Chips_dashboard/chip_dashboard
[ -f db.sqlite3 ] && cp db.sqlite3 db.sqlite3.bak.$(date +%Y%m%d%H%M)
```
- If using PostgreSQL (recommended):
```bash
mkdir -p /root/Chips_dashboard/db_backups
PGUSER=dbuser PGPASSWORD=yourpass pg_dump -h localhost -U dbuser -Fc dbname > /root/Chips_dashboard/db_backups/prod_$(date +%Y%m%d%H%M).dump
```
Why: You can restore data if migrations break.

3) Confirm `ALLOWED_HOSTS` includes your domain & IP
- Edit your settings or systemd unit environment to include `pravoo.in` and `72.61.148.117`.
- Example: if your systemd unit uses Environment= or an EnvironmentFile, set:
```
ALLOWED_HOSTS="localhost,127.0.0.1,72.61.148.117,pravoo.in"
```

4) Run database migrations (production)
- IMPORTANT: If migrations previously failed (lazy reference errors), resolve those first (see Troubleshooting). When ready:
```bash
cd /root/Chips_dashboard/chip_dashboard
# If Postgres is configured correctly in settings/env:
sudo -u www-data /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py migrate --noinput
# Or if using sqlite fallback (dev only):
USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py migrate --noinput
```

5) Collect static files
```bash
cd /root/Chips_dashboard/chip_dashboard
/root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py collectstatic --noinput
```
- Make sure nginx serves `STATIC_ROOT`.

6) Restart systemd & Gunicorn service
```bash
sudo systemctl daemon-reload
sudo systemctl restart broker_portal
sudo systemctl enable broker_portal
sudo systemctl status broker_portal --no-pager -l
```
- To watch logs:
```bash
sudo journalctl -u broker_portal -f
```
Why: Restart picks up new code and restarts workers.

7) Reload nginx (if nginx config changed)
```bash
sudo nginx -t && sudo systemctl reload nginx
```

8) Health checks & validation
- Confirm listeners:
```bash
ss -ltnp | egrep ':8001|:80|:443' || true
```
- Check nginx responses:
```bash
curl -I -k -H "Host: pravoo.in" https://127.0.0.1
curl -I https://pravoo.in
```
- Review logs:
```bash
sudo journalctl -u broker_portal -n 200 --no-pager
sudo tail -n 200 /var/log/nginx/error.log
```

---

## Sample systemd unit for Gunicorn (`/etc/systemd/system/broker_portal.service`)
Use or adapt this unit. Change paths, user, and environment variables as appropriate.

```
[Unit]
Description=Gunicorn daemon for broker_portal Django app
After=network.target

[Service]
User=root
Group=root
# Path to your virtualenv bin
Environment="PATH=/root/Chips_dashboard/chip_dashboard/myenv/bin"
Environment="DJANGO_SETTINGS_MODULE=broker_portal.settings"
# Add any other envs here (DATABASE_URL, ALLOWED_HOSTS, etc)
WorkingDirectory=/root/Chips_dashboard/chip_dashboard
ExecStart=/root/Chips_dashboard/chip_dashboard/myenv/bin/gunicorn broker_portal.wsgi:application \
    --name broker_portal \
    --bind 0.0.0.0:8001 \
    --workers 3 \
    --timeout 60
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Notes: tune `--workers` and `--timeout` based on CPU/RAM and request patterns.

---

## Nginx proxy snippet (inside server block for `pravoo.in`)
Make sure to forward headers for CSRF and client IPs.

```
server {
    listen 80;
    server_name pravoo.in 72.61.148.117;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name pravoo.in 72.61.148.117;

    ssl_certificate /etc/letsencrypt/live/pravoo.in/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/pravoo.in/privkey.pem;

    location /static/ {
        alias /path/to/static_root/;  # STATIC_ROOT
    }

    location / {
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_pass http://127.0.0.1:8001;
        proxy_redirect off;
    }
}
```

---

## Migration troubleshooting: "lazy reference to 'core.customuser'"
Symptoms: running `manage.py migrate` fails with values like:
```
ValueError: The field admin.LogEntry.user was declared with a lazy reference to 'core.customuser', but app 'core' doesn't provide model 'customuser'.
```
Possible causes:
- `AUTH_USER_MODEL` in settings differs from the actual model path.
- A migration references a model name that has been renamed or changed case.

Investigation steps:
```bash
# Check AUTH_USER_MODEL
grep -n "AUTH_USER_MODEL" broker_portal/settings.py || true
# Inspect your custom user model
sed -n '1,240p' core/models.py
# Find references in migrations
grep -R "customuser" -n core/migrations || true
```
Fix options:
1) If `AUTH_USER_MODEL` is incorrect, set it in `settings.py` to the correct value, e.g. `AUTH_USER_MODEL = 'core.CustomUser'` and re-run migrate.
2) If a migration references the wrong model name (typo/case), create a corrective migration or, for dev only, reset the DB and re-run migrations from scratch:
```bash
# DEV only - destroys data
rm db.sqlite3
python manage.py migrate
```
3) If option 1/2 not feasible, add a small temporary model stub that matches the referenced name (advanced and risky).

If you want, I can inspect `core/migrations` and `core/models.py` and propose a targeted edit.

---

## Common fixes for runtime issues
- Worker TIMEOUTs: increase gunicorn `--timeout` or increase `--workers`. Consider async workers (`gevent`) for I/O heavy workloads.
- CSRF failures: ensure nginx forwards host and proto headers and that `SESSION_COOKIE_SECURE` and `CSRF_COOKIE_SECURE` are set appropriately for HTTPS.
- Static 404s: run `collectstatic` and ensure nginx serves `STATIC_ROOT`.

---

## Rollback & safety (fast recovery)
- To restore sqlite backup:
```bash
cd /root/Chips_dashboard/chip_dashboard
cp db.sqlite3.bak.YOURTIMESTAMP db.sqlite3
sudo systemctl restart broker_portal
```
- To roll back code to previous commit:
```bash
cd /root/Chips_dashboard
git checkout HEAD~1
sudo systemctl restart broker_portal
```
- To undo the last commit locally (if unwanted):
```bash
cd /root/Chips_dashboard
git reset --soft HEAD~1  # keeps changes staged
# or to drop changes entirely
# git reset --hard HEAD~1
```

---

## Quick checklist (copy/paste)
- Local/dev test:
```bash
cd /root/Chips_dashboard
git pull origin main
/root/Chips_dashboard/chip_dashboard/myenv/bin/pip install -r requirements.txt
cd chip_dashboard
[ -f db.sqlite3 ] && cp db.sqlite3 db.sqlite3.bak.$(date +%Y%m%d%H%M)
USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py migrate --noinput
USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py runserver 0.0.0.0:8000
```
- Deploy/prod:
```bash
cd /root/Chips_dashboard
git pull origin main
/root/Chips_dashboard/chip_dashboard/myenv/bin/pip install -r requirements.txt
# backup DB
# run migrations
sudo -u www-data /root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py migrate --noinput
# collect static
/root/Chips_dashboard/chip_dashboard/myenv/bin/python manage.py collectstatic --noinput
# restart service
sudo systemctl daemon-reload
sudo systemctl restart broker_portal
sudo nginx -t && sudo systemctl reload nginx
```

---

If you'd like, I can:
- Commit this file into the repository for you (I have just created it under the repo root).
- Run the migration steps and inspect `core` migrations/models to fix the lazy reference error.
- Add this document to the repo with a small README or link from existing docs.

Tell me which follow-up you'd like. 
