# Nginx & Gunicorn Snippets

This file contains the exact snippets used in the running system for quick copy-paste.

## systemd unit

File: `/etc/systemd/system/broker_portal.service`

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

## nginx site

File: `/etc/nginx/sites-available/django`

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

---

End of file.
