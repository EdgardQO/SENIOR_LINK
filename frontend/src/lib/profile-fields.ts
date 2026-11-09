/** Muestra los datos del perfil entregados por el backend. */
export function renderProfileFields(data: Record<string, any>): void {
  document.querySelectorAll<HTMLElement>('[data-field]').forEach((field) => {
    const path = field.dataset.field;
    if (!path) return;
    let value: unknown = path === 'usuario.nombreCompleto'
      ? [data.usuario?.nombres, data.usuario?.apellidos].filter(Boolean).join(' ')
      : path.split('.').reduce<any>((current, key) => current?.[key], data);
    if (path === 'acompanante.tarifaBasePorHora' && value != null) {
      value = 'S/ ' + Number(value).toFixed(2);
    }
    field.textContent = value == null || String(value).trim() === ''
      ? 'No registrado' : String(value);
  });
}
