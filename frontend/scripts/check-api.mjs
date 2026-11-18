#!/usr/bin/env node
// Read-only API smoke checks. Does not create, modify or delete records.
// Usage:
//   node scripts/check-api.mjs http://localhost:4321
//   SMOKE_ADMIN_TOKEN=<JWT> node scripts/check-api.mjs https://your-domain.example
// Never paste tokens into a public shell transcript or commit them to Git.
const base = (process.argv[2] || 'http://localhost:4321').replace(/\/$/, '');
const token = process.env.SMOKE_ADMIN_TOKEN;
const checks = [
  ['GET', '/api/v1/instituciones-aliadas', false, Array.isArray],
  ...(token ? [
    ['GET', '/api/v1/admin/dashboard', true, body => body && typeof body === 'object' && Number.isFinite(body.usuarios)],
    ['GET', '/api/v1/admin/usuarios', true, Array.isArray],
    ['GET', '/api/v1/admin/instituciones', true, Array.isArray],
    ['GET', '/api/v1/admin/promociones', true, Array.isArray],
    ['GET', '/api/v1/admin/reservas', true, Array.isArray]
  ] : [])
];
let failures = 0;
for (const [method, path, authenticated, validate] of checks) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10000);
  try {
    const response = await fetch(base + path, {
      method,
      headers: authenticated ? {Authorization: 'Bearer ' + token} : {},
      signal: controller.signal
    });
    const raw = await response.text();
    let data;
    try { data = JSON.parse(raw); } catch { data = null; }
    const ok = response.ok && validate(data);
    console.log((ok ? 'PASS' : 'FAIL') + ' ' + method + ' ' + path +
      ' - HTTP ' + response.status + (ok ? '' : ' (respuesta inesperada o HTML en lugar de JSON)'));
    if (!ok) failures++;
  } catch (error) {
    failures++;
    console.log('FAIL ' + method + ' ' + path + ' - ' + (error.message || String(error)));
  } finally {
    clearTimeout(timeout);
  }
}
if (!token) console.log('Solo se verificaron rutas públicas. Configura SMOKE_ADMIN_TOKEN para probar también las listas administrativas (sin escrituras).');
if (failures) process.exitCode = 1;
