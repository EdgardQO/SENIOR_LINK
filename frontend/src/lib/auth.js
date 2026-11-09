export const AUTH_KEY = 'seniorlink_auth';
export const ROLE_PATHS = Object.freeze({
  ADMINISTRADOR: '/admin',
  ACOMPANANTE: '/acompanante',
  ADULTO_MAYOR: '/adulto-mayor',
  SOPORTE: '/soporte',
  FAMILIAR: '/familiar'
});
export const API_URL = import.meta.env.PUBLIC_API_URL || 'http://localhost:8080';

function fromStorage(storage) {
  try {
    const raw = storage.getItem(AUTH_KEY);
    if (!raw) return null;
    const data = JSON.parse(raw);
    if (!data?.accessToken || !ROLE_PATHS[data.rolNombre] ||
        !/^\d{8}$/.test(String(data.dni || '')) ||
        !Number.isFinite(Number(data.expiresAt)) ||
        Number(data.expiresAt) <= Date.now()) {
      storage.removeItem(AUTH_KEY);
      return null;
    }
    return data;
  } catch {
    storage.removeItem(AUTH_KEY);
    return null;
  }
}
export function getSession() {
  if (typeof window === 'undefined') return null;
  return fromStorage(window.sessionStorage) || fromStorage(window.localStorage);
}
export function clearSession() {
  if (typeof window === 'undefined') return;
  window.sessionStorage.removeItem(AUTH_KEY);
  window.localStorage.removeItem(AUTH_KEY);
}
export function homeForRole(role) { return ROLE_PATHS[role] || '/login'; }
export function saveSession(data, remember) {
  if (!data?.accessToken || !ROLE_PATHS[data.rolNombre] ||
      !/^\d{8}$/.test(String(data.dni || '')) ||
      !Number.isFinite(Number(data.expiresIn)) || Number(data.expiresIn) <= 0)
    throw new Error('Respuesta de autenticación incompleta.');
  clearSession();
  (remember ? localStorage : sessionStorage).setItem(AUTH_KEY, JSON.stringify({
    accessToken: data.accessToken,
    expiresAt: Date.now() + Number(data.expiresIn) * 1000,
    dni: data.dni,
    rolNombre: data.rolNombre,
    nombres: data.nombres || ''
  }));
  return homeForRole(data.rolNombre);
}
export async function fetchProfile(session) {
  const res = await fetch(API_URL + '/api/v1/usuarios/' + encodeURIComponent(session.dni), {
    headers: { Authorization: 'Bearer ' + session.accessToken }
  });
  if (res.status === 401 || res.status === 403) {
    const err = new Error('No autorizado');
    err.status = res.status;
    throw err;
  }
  if (!res.ok) throw new Error('No se pudo cargar el perfil desde el servidor.');
  return res.json();
}
