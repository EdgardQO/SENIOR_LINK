import { API_URL, getSession } from './auth.js';

const direcciones = new WeakMap<HTMLImageElement, string>();
const solicitudes = new WeakMap<HTMLImageElement, number>();

export async function mostrarFotoProtegida(dni: string, imagen: HTMLImageElement): Promise<boolean> {
  const sesion = getSession();
  const secuencia = (solicitudes.get(imagen) || 0) + 1;
  solicitudes.set(imagen, secuencia);
  if (!sesion || !/^\d{8}$/.test(dni)) {
    imagen.hidden = true;
    return false;
  }
  try {
    const respuesta = await fetch(API_URL + '/api/v1/usuarios/' + encodeURIComponent(dni) + '/foto', {
      headers: { Authorization: 'Bearer ' + sesion.accessToken },
      cache: 'no-store'
    });
    if (!respuesta.ok || !respuesta.headers.get('content-type')?.startsWith('image/')) {
      imagen.hidden = true;
      return false;
    }
    const nueva = URL.createObjectURL(await respuesta.blob());
    if (solicitudes.get(imagen) !== secuencia) {
      URL.revokeObjectURL(nueva);
      return false;
    }
    const previa = direcciones.get(imagen);
    imagen.src = nueva;
    imagen.hidden = false;
    direcciones.set(imagen, nueva);
    if (previa) URL.revokeObjectURL(previa);
    return true;
  } catch {
    imagen.hidden = true;
    return false;
  }
}

export function limpiarFoto(imagen: HTMLImageElement) {
  solicitudes.set(imagen, (solicitudes.get(imagen) || 0) + 1);
  const anterior = direcciones.get(imagen);
  if (anterior) URL.revokeObjectURL(anterior);
  direcciones.delete(imagen);
  imagen.removeAttribute('src');
  imagen.hidden = true;
}
