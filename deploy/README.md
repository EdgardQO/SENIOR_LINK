# SENIOR_LINK en Hostinger (sin dominio todavía)

Este Compose utiliza la instancia PostgreSQL existente, en la red externa `seniorlink_internal`. **No instala un segundo PostgreSQL**. La base `seniorlink_db` y el usuario `seniorlink_user` ya deben existir.

## Antes de arrancar

1. Cambia/rota toda contraseña y secreto previamente subido a un repositorio público, incluso si se elimina el archivo posteriormente. El historial seguirá conteniéndolos.
2. Revisa la estructura de tablas de producción; `HIBERNATE_DDL_AUTO=update` es temporal para las primeras pruebas, no una estrategia segura de migraciones.
3. Crea `deploy/.env` fuera del control de versiones con permisos `chmod 600`.
4. Asigna a `SENIORLINK_DB_PASSWORD` la contraseña **existente** de `seniorlink_user` del VPS (no la contraseña de postgres ni de n8n).
5. Genera una **nueva** clave JWT para el VPS: `openssl rand -base64 48 | tr -d '\n'`. Nunca reutilices un secreto filtrado.
6. Verifica que las redes `proxy` y `seniorlink_internal` existan (`sudo docker network ls`).

## Despliegue

```bash
cd /opt/proyectos/senior-link/codigo/deploy
cp .env.example .env
chmod 600 .env
nano .env
sudo docker compose --env-file .env config --quiet
sudo docker compose --env-file .env up -d --build
sudo docker compose --env-file .env ps
sudo docker compose --env-file .env logs --tail=80 backend
```

Para seguridad, los puertos 4321 (Astro) y 8080 (API) se publican solo en `127.0.0.1` del **VPS**, no en la IP pública. Accede desde **PowerShell de Windows** con un túnel:

```powershell
ssh -i "$env:USERPROFILE\.ssh\id_ed25519" -L 4321:127.0.0.1:4321 -L 8080:127.0.0.1:8080 adminvps@179.66.150.160
```

Mientras mantengas ese túnel abierto, visita http://localhost:4321 en Windows. No hace falta abrir puertos UFW adicionales. Cuando tengas un dominio, configuraremos Caddy, HTTPS, CORS y `PUBLIC_API_URL` del build de Astro.

Si el puerto 4321 o 8080 está ocupado en Windows, libera el puerto o modifica conjuntamente los puertos locales del túnel, la URL del frontend y CORS. El valor `PUBLIC_API_URL` queda integrado en el build de Astro.

**No subas `deploy/.env` ni la contraseña de PostgreSQL.** La base de datos y n8n siguen separados por usuario, base y red. Haz backups externos antes de operar con datos reales.
