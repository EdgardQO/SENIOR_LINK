# SeniorLink — pasarela simulada para localhost

Esta funcionalidad solo existe con `SPRING_PROFILES_ACTIVE=local`.
No utiliza bancos, tarjetas ni fondos reales. El proyecto en VPS y la rama `main` no se modifican.

## 1. Actualizar el código

Desde la carpeta del proyecto en PowerShell:

```powershell
git status
git fetch origin
git switch dev/pasarela-local
git pull origin dev/pasarela-local
```

Si todavía no existe esa rama local:

```powershell
git switch --track origin/dev/pasarela-local
```

Si tienes cambios sin guardar, haz commit o usa un mecanismo adecuado para conservarlos antes de cambiar de rama.

## 2. Backend

Requisitos: Java 21 y una base PostgreSQL **local** ya creada y vacía, por ejemplo `SENIOR_LINK`.
Hibernate creará las tablas y el esquema `pasarela` en el perfil local. No ejecutes manualmente los scripts en `api/sql/local` sobre una base generada por JPA.

Configura en IntelliJ (Run > Edit Configurations > Environment variables):

- `SPRING_PROFILES_ACTIVE=local`
- `DB_URL=jdbc:postgresql://localhost:5432/SENIOR_LINK`
- `DB_USERNAME` y `DB_PASSWORD` de tu PostgreSQL local
- `JWT_SECRET_BASE64`: clave Base64 privada y suficientemente larga. **Nunca** la publiques en GitHub.

Para crear un administrador local la primera vez, establece también los siguientes valores **solo en IntelliJ**:

- `LOCAL_ADMIN_DNI`: identificador local de ocho dígitos
- `LOCAL_ADMIN_EMAIL`: correo de pruebas que controles
- `LOCAL_ADMIN_PASSWORD`: contraseña de al menos 12 caracteres

Opcionalmente puedes crear la cuenta de soporte usando `LOCAL_SOPORTE_DNI`, `LOCAL_SOPORTE_EMAIL` y `LOCAL_SOPORTE_PASSWORD`.
Estas cuentas se crean una sola vez y no se actualizan en reinicios posteriores. No existe tabla de credenciales de prueba.

Ejecuta `ApiApplication` desde IntelliJ, o desde `api`:

```powershell
.\mvnw.cmd spring-boot:run
```

## 3. Astro

```powershell
cd frontend
npm ci
npm run dev
```

Abre el puerto que Astro indique en la consola (normalmente 4321, pero puede ser 4322 u otro).
El proxy `/api` apunta a `http://localhost:8080` a través de `astro.config.mjs`.

## 4. Flujo manual de pruebas

1. Registra desde `/registro` una cuenta de acompañante, una de adulto mayor y una de familiar.
2. Como acompañante, entra a `/acompanante/disponibilidad` y define al menos un día y hora. Ajusta una tarifa entre S/ 10 y S/ 50.
3. Como familiar, solicita vinculación en `/familiar/vinculaciones`. El adulto mayor la autoriza desde `/adulto-mayor/vinculaciones`.
4. Crea una reserva con al menos 120 minutos de anticipación en `/adulto-mayor/reservas` o `/familiar/reservas`. Debe durar entre 60 y 480 minutos.
5. El acompañante acepta desde `/acompanante/solicitudes` dentro de 60 minutos.
6. El cliente añade una **tarjeta sintética** en `/adulto-mayor/metodos-pago` o `/familiar/metodos-pago`. El sistema genera un identificador del tipo `DEMO-########`; solo permite CVV ficticios.
7. Paga dentro de 30 minutos después de la aceptación. La pasarela valida internamente fondos de prueba, fecha y CVV, y responde APROBADO o RECHAZADO. **SeniorLink no consulta ni muestra el saldo**.
8. Prueba la cancelación de una reserva pagada; quedará en disputa. Soporte revisa en `/soporte/incidencias` si se reportó un incidente; administración puede registrar un reembolso ficticio desde `/admin/pagos`.
9. Al finalizar un servicio, el acompañante lo marca finalizado y el adulto o familiar autorizado confirma. El cliente puede valorarlo una vez. Administración registra la liquidación manual en `/admin/liquidaciones`.
10. Desde `/admin/configuracion-reservas` puedes modificar límites para **nuevas** reservas.

## 5. Verificación técnica

La rama contiene el workflow `.github/workflows/pasarela-local-ci.yml` que arranca PostgreSQL temporal, ejecuta Maven y construye Astro.

En PowerShell, con una base de pruebas dedicada, puedes ejecutar:

```powershell
cd api
.\mvnw.cmd test
```

Los tests automatizados son transaccionales, pero no sustituyen revisar manualmente el flujo de interfaces con varios roles.

## 6. Alcance y seguridad

- Los balances ficticios solo existen dentro de `pasarela.tarjetas`, nunca en las respuestas de métodos de pago.
- Los identificadores de tarjeta siguen el patrón `DEMO-########`; no introduzcas ni almacenes datos bancarios reales.
- El CVV ficticio se verifica contra un hash y nunca se muestra en las respuestas.
- Una clave de idempotencia impide repetir el cobro de la misma solicitud.
- Una transacción rechazada no altera fondos ni confirma la reserva.
- La cuenta administrativa de prueba se crea solo si introduces sus variables de entorno.
- La separación por esquemas sigue compartiendo una conexión de base de datos: es un simulador local, **no** una pasarela bancaria certificada.
- No despliegues este módulo con el perfil `local` en el VPS.

**Nota:** Los scripts SQL previos quedan como historial y no forman parte del arranque JPA del entorno local.
