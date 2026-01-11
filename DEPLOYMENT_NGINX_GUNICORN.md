# Deployment: Nginx + Gunicorn + systemd (broker_portal)

This document shows how I configured nginx and gunicorn (systemd) for the `broker_portal` Django app and includes the service/unit and nginx site snippets plus commands to manage them.

---

## Goals
- Serve Django via Gunicorn (systemd managed)
- Use nginx as a reverse proxy and TLS terminator
- Keep the app always up (systemd Restart=always)

## Systemd unit (example)

Create file `/etc/systemd/system/broker_portal.service` with the following contents (the project here is in `/root/Chips_dashboard/chip_dashboard` and the virtualenv in `myenv`):

```
[Unit]
Description=Gunicorn daemon for broker_portal Django app
After=network.target

[Service]
User=root
Group=www-data
WorkingDirectory=/root/Chips_dashboard/chip_dashboard
Environment="PATH=/root/Chips_dashboard/chip_dashboard/myenv/bin"
Environment="DJANGO_SETTINGS_MODULE=broker_portal.settings"
Environment="ALLOWED_HOSTS=localhost,127.0.0.1,72.61.148.117,pravoo.in"
Environment="USE_SQLITE=1"
ExecStart=/root/Chips_dashboard/chip_dashboard/myenv/bin/gunicorn broker_portal.wsgi:application --bind 0.0.0.0:8001 --workers 3
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Notes:
- Change `User`/`Group` if you want non-root deployment.
- For production, set `USE_SQLITE=0` and configure PostgreSQL environment variables (DB_NAME, DB_USER, DB_PASSWORD, DB_HOST, DB_PORT).

Reload and enable:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now broker_portal
sudo journalctl -u broker_portal -f
```

## Nginx site (example)

Create `/etc/nginx/sites-available/django` and symlink to `sites-enabled`.

Key server block snippet (example with TLS already provisioned by Certbot):

```
server {
    server_name pravoo.in www.pravoo.in;

    location / {
        proxy_pass http://127.0.0.1:8001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    listen 443 ssl; # managed by Certbot
    ssl_certificate /etc/letsencrypt/live/pravoo.in/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/pravoo.in/privkey.pem; # managed by Certbot
    include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot

}
server {
    if ($host = pravoo.in) {
        return 301 https://$host$request_uri;
    }
    listen 80;
    server_name pravoo.in www.pravoo.in;
    return 404; # managed by Certbot
}
```

Test and reload nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## Useful commands

- View gunicorn logs:
  - `sudo journalctl -u broker_portal -f`
- Check service status:
  - `sudo systemctl status broker_portal`
- Check open sockets:
  - `ss -ltnp | egrep ':80|:443|:8001'`
- Apply Django migrations (SQLite fallback):
  - `USE_SQLITE=1 /root/Chips_dashboard/chip_dashboard/myenv/bin/python /root/Chips_dashboard/chip_dashboard/manage.py migrate`

## Security & production notes
- Use a non-root user to run the app.
- Use PostgreSQL for production and set the database environment variables.
- Use Certbot for TLS and auto-renewal (the site already had certs).
- Add process monitoring/alerts (Prometheus, simple health-check scripts) for production.

---

End of file.
