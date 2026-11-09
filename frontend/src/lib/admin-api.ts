import { API_URL, clearSession, getSession } from './auth.js';

export async function adminRequest<T = any>(path: string, options: RequestInit = {}): Promise<T> {
  const session = getSession();
  if (!session || session.rolNombre !== 'ADMINISTRADOR') {
    window.location.replace('/login');
    throw new Error('Sesión administrativa requerida.');
  }

  const response = await fetch(API_URL + '/api/v1/admin' + path, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      Authorization: 'Bearer ' + session.accessToken,
      ...options.headers
    }
  });

  if (response.status === 401 || response.status === 403) {
    clearSession();
    window.location.replace('/login');
    throw new Error('Tu sesión no tiene permisos de administración.');
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new Error(response.status === 409
      ? 'La operación entra en conflicto con datos existentes.'
      : error.detail || error.message || error.error || 'No se pudo completar la operación.');
  }

  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export function setStatus(id: string, message: string, error = false): void {
  const element = document.getElementById(id);
  if (!element) return;
  element.textContent = message;
  element.style.color = error ? '#b42336' : '#087a70';
}
