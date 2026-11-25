import {apiPagoLocal,horaLocal,importe} from './local-api';

export type Recibo={referencia:string,reservaId:number,estado:string,monto:number|string,moneda:string,fecha:string|null,metodo:string|null,ultimosCuatro:string|null};
export async function consultarRecibos():Promise<Recibo[]>{
 return (import.meta.env.DEV || import.meta.env.PUBLIC_PAGOS_SIMULADOS === 'true') ? await apiPagoLocal('/recibos') : [];
}
export function reciboDeReserva(recibos:Recibo[],id:unknown){
 return recibos.find(r=>Number(r.reservaId)===Number(id));
}
export function abrirRecibo(r:Recibo){
 const previo=document.querySelector<HTMLDialogElement>('#seniorlink-recibo-compartido');
 if(previo?.open){previo.close();previo.remove();}
 const modal=document.createElement('dialog');
 modal.id='seniorlink-recibo-compartido';modal.className='sl-recibo';
 modal.setAttribute('aria-labelledby','sl-recibo-titulo');
 // La estructura es estática. Los datos de la API se insertan como texto.
 modal.innerHTML=`<header class="sl-recibo-header"><div><div class="sl-recibo-brand">SeniorLink <span>Pay</span></div><h2 id="sl-recibo-titulo">Comprobante de operación</h2></div><button type="button" class="sl-recibo-x" data-cerrar aria-label="Cerrar recibo">×</button></header>
 <div class="sl-recibo-body"><div class="sl-recibo-resumen"><svg viewBox="0 0 24 24" width="36" height="36" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg><strong data-campo="estado"></strong><span data-campo="monto"></span></div>
 <dl><dt>Reserva</dt><dd data-campo="reserva"></dd><dt>Fecha de operación</dt><dd data-campo="fecha"></dd><dt>Método de pago</dt><dd data-campo="metodo"></dd><dt>Número de operación</dt><dd data-campo="referencia"></dd></dl>
 <p class="sl-recibo-aviso">Entorno de pruebas · Registro de operación</p></div>
 <footer class="sl-recibo-footer"><button type="button" class="sl-recibo-exportar" disabled title="Exportación no disponible"><svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 3v12m-4-4 4 4 4-4M4 16v4h16v-4"/></svg>Exportar a PDF</button><button type="button" class="sl-recibo-cerrar" data-cerrar>Cerrar</button></footer>`;
 const campo=(clave:string,valor:string)=>{modal.querySelector('[data-campo="'+clave+'"]')!.textContent=valor;};
 const reembolso=r.estado==='REEMBOLSADO';
 campo('estado',reembolso?'Pago reembolsado':'Pago procesado correctamente');
 campo('monto',importe(r.monto));campo('reserva','#'+r.reservaId);
 campo('fecha',horaLocal(r.fecha));campo('referencia',r.referencia||'—');
 campo('metodo',r.metodo&&r.ultimosCuatro?(r.metodo==='DEBITO'?'Débito':'Crédito')+' · •••• '+r.ultimosCuatro:'Pago registrado');
 modal.querySelector<HTMLElement>('.sl-recibo-resumen')!.dataset.reembolsado=String(reembolso);
 const foco=document.activeElement as HTMLElement|null;
 const scroll=document.body.style.overflow;
 modal.querySelectorAll<HTMLButtonElement>('[data-cerrar]').forEach(b=>b.addEventListener('click',()=>modal.close()));
 modal.addEventListener('click',e=>{if(e.target===modal){const b=modal.getBoundingClientRect();if(e.clientX<b.left||e.clientX>b.right||e.clientY<b.top||e.clientY>b.bottom)modal.close();}});
 modal.addEventListener('close',()=>{document.body.style.overflow=scroll;modal.remove();if(foco?.isConnected)foco.focus();},{once:true});
 document.body.append(modal);modal.showModal();document.body.style.overflow='hidden';
}
export function botonRecibo(r:Recibo,clase='local-secondary-action'){
 const boton=document.createElement('button');boton.type='button';boton.className=clase;boton.textContent='Ver recibo';boton.setAttribute('aria-haspopup','dialog');boton.addEventListener('click',()=>abrirRecibo(r));return boton;
}
