# Autenticación JWT

Configurar antes de iniciar: `JWT_SECRET_BASE64` con 32 bytes aleatorios codificados Base64. No subir secretos al repositorio.

PowerShell:
```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
.\mvnw.cmd spring-boot:run
```

POST /api/v1/auth/login con JSON `{"correoElectronico":"usuario@example.com","contrasena":"ClaveSegura123"}`.
La respuesta incluye `accessToken`, `tokenType`, `expiresIn`, `rolNombre` y `rutaInicio`.
Rutas sugeridas: /admin, /soporte, /acompanante, /adulto-mayor, /familiar. El frontend debe realizar la redirección.
Enviar `Authorization: Bearer <accessToken>` en solicitudes protegidas.

Login y registro públicos; demás rutas requieren JWT. Listado de usuarios solo ADMINISTRADOR; detalle solo propietario o administrador; edición de usuario solo administrador por ahora.

PENDIENTES antes de producción: autorización por recurso en reservas/instituciones, validación de firma de webhooks Mercado Pago, revocación de JWT al desactivar usuario o cambiar rol, configuración de CORS, protección del frontend y eliminación de claves antiguas en texto plano (ya no admitidas en login).
