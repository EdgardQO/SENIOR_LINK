import {apiSegura} from './local-api';
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
  montarEditorContacto(data);
}

function montarEditorContacto(data: Record<string, any>): void {
  const rol=data.usuario?.rolNombre;
  if(!['ACOMPANANTE','FAMILIAR','ADULTO_MAYOR'].includes(rol))return;
  const host=document.querySelector<HTMLElement>('#mi-perfil .profile-box');
  if(!host || host.querySelector('#perfil-contacto-form'))return;
  const form=document.createElement('form');form.id='perfil-contacto-form';
  form.style.cssText='display:grid;gap:10px;margin-top:24px;padding:18px;background:#f1faf6;border:1px solid #d5ebe1;border-radius:12px';
  const titulo=document.createElement('h3');titulo.textContent='Editar datos de contacto';titulo.style.margin='0';
  const nota=document.createElement('p');nota.textContent='Actualiza tu teléfono personal. El número de emergencia se mantiene separado.';nota.style.margin='0';
  form.append(titulo,nota);
  function campo(nombre:string,etiqueta:string,valor:string,obligatorio:boolean){
    const label=document.createElement('label');label.textContent=etiqueta;
    const input=document.createElement('input');input.name=nombre;input.type='tel';input.maxLength=20;input.required=obligatorio;input.value=valor??'';
    input.placeholder='Celular de 9 dígitos o +código de país';
    input.style.cssText='display:block;width:100%;box-sizing:border-box;border:1px solid #cbded6;border-radius:8px;padding:11px;margin-top:6px;background:white;color:#173d35;font:inherit';
    label.append(input);form.append(label);return input;
  }
  const tel=campo('telefono','Teléfono personal',data.usuario?.telefono??'',true);
  tel.autocomplete='tel';
  const alt=rol==='FAMILIAR'?campo('telefonoAlternativo','Teléfono alternativo (opcional)',data.familiar?.telefonoAlternativo??'',false):null;
  const btn=document.createElement('button');btn.type='submit';btn.textContent='Guardar datos de contacto';btn.style.cssText='justify-self:start;border:0;border-radius:8px;background:#087a70;color:white;padding:11px 16px;font:inherit;cursor:pointer';
  const estado=document.createElement('p');estado.setAttribute('role','status');estado.setAttribute('aria-live','polite');estado.style.margin='0';
  form.append(btn,estado);host.append(form);
  form.addEventListener('submit',async(e)=>{
    e.preventDefault();btn.disabled=true;estado.textContent='Guardando…';
    try{
      const actualizado=await apiSegura('/usuarios/me/contacto',{method:'PUT',body:JSON.stringify({telefono:tel.value,...(alt?{telefonoAlternativo:alt.value}:{})})});
      tel.value=actualizado.usuario?.telefono??'';if(alt)alt.value=actualizado.familiar?.telefonoAlternativo??'';
      renderProfileFields(actualizado);estado.textContent='Datos de contacto actualizados.';
    }catch(err){estado.textContent=err instanceof Error?err.message:String(err);}
    finally{btn.disabled=false;}
  });
}
