import { API_URL, getSession, homeForRole } from './auth.js';

export function sesionRequerida() {
 const session = getSession();
 if (!session) { window.location.href='/login'; throw new Error('Inicia sesión'); }
 return session;
}
async function consultarApi(base: string, path: string, options: RequestInit = {}) {
 const sesion=sesionRequerida();
 const res=await fetch(API_URL+base+path,{
  ...options,
  headers:{Authorization:'Bearer '+sesion.accessToken,
   ...(options.body?{'Content-Type':'application/json'}:{}),...options.headers}
 });
 if(res.status===401||res.status===403){
  if(res.status===401){window.location.href='/login';}
  throw new Error('No tienes permiso para esta operación');
 }
 if(!res.ok){
  let err='Error HTTP '+res.status;
  try{const j=await res.json();err=j.detail||j.message||j.error||err;}catch{}
  throw Object.assign(new Error(err),{status:res.status});
 }
 return res.status===204?null:res.json();
}
export function apiLocal(path: string, options: RequestInit = {}) {
 return consultarApi('/api/v1/operaciones', path, options);
}
// Exclusivo de pagos de prueba: no se usa para operaciones reales en VPS.
export function apiPagoLocal(path: string, options: RequestInit = {}) {
 return consultarApi('/api/v1/local', path, options);
}
export function apiSegura(path: string, options: RequestInit = {}) {
 return consultarApi('/api/v1', path, options);
}
export function horaLocal(iso: string|null|undefined) {
 return iso?new Date(iso).toLocaleString('es-PE',{dateStyle:'short',timeStyle:'short',timeZone:'America/Lima'}):'—';
}
export function importe(value: number|string|null|undefined){
 return new Intl.NumberFormat('es-PE',{style:'currency',currency:'PEN'}).format(Number(value||0));
}
export function texto(element: Element|null,value: unknown){
 if(element)element.textContent=String(value??'');
}

export async function simulacionDisponible():Promise<boolean>{
 const datos=await apiLocal('/capacidades');
 return datos?.pagosSimulados===true;
}
