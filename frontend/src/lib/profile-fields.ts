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

export function montarEditorContacto(data: Record<string, any>): void {
  const rol=data.usuario?.rolNombre;
  const host=document.querySelector<HTMLElement>('#mi-perfil .profile-box, section.profile-box[aria-labelledby="own-profile-title"]');
  if(!host || !rol)return;
  const existente=host.querySelector<HTMLDialogElement>('#perfil-editor');
  if(existente){existente.dispatchEvent(new CustomEvent('datos-perfil',{detail:data}));return;}
  let actual=data,guardando=false;
  const abrir=document.createElement('button');abrir.type='button';abrir.textContent='Editar mi perfil';abrir.className='perfil-editar-principal';
  abrir.style.cssText='margin-top:16px;border:0;border-radius:9px;background:#087a70;color:white;padding:12px 16px;font:inherit;cursor:pointer';
  const modal=document.createElement('dialog');modal.id='perfil-editor';modal.className='perfil-editor-modal';modal.setAttribute('aria-labelledby','perfil-editor-titulo');
  modal.style.cssText='position:fixed;inset:0;margin:auto;width:min(620px,calc(100% - 32px));max-height:calc(100dvh - 32px);overflow:auto;border:1px solid #d5ebe1;border-radius:16px;padding:0;background:white;color:#173d35';
  const form=document.createElement('form');form.style.cssText='display:grid;gap:12px;padding:24px;background:#f5faf8';
  const titulo=document.createElement('h3');titulo.id='perfil-editor-titulo';titulo.textContent='Editar mi perfil';titulo.style.margin='0';const cabecera=document.createElement('header');cabecera.className='perfil-editor-cabecera';const cerrar=document.createElement('button');cerrar.type='button';cerrar.textContent='×';cerrar.setAttribute('aria-label','Cerrar edición');cerrar.className='perfil-editor-cerrar';cabecera.append(titulo,cerrar);form.append(cabecera);
  const controles:Array<{ruta:string;input:HTMLInputElement|HTMLTextAreaElement|HTMLSelectElement;numero:boolean}>=[];
  const estilo='display:block;width:100%;box-sizing:border-box;border:1px solid #cbded6;border-radius:8px;padding:10px;margin-top:5px;background:white;color:#173d35;font:inherit';
  function seccion(texto:string){const h=document.createElement('h4');h.textContent=texto;h.style.cssText='margin:8px 0 0;color:#087a70';form.append(h);}
  function campo(ruta:string,etiqueta:string,max:number,tipo='text',obligatorio=false){
    const label=document.createElement('label');label.textContent=etiqueta;
    const input=tipo==='textarea'?document.createElement('textarea'):document.createElement('input');
    if(input instanceof HTMLInputElement){input.type=tipo;if(tipo==='number'){input.min='1';input.max='20';input.step='1';}}
    else input.rows=3;
    if(tipo!=='number')input.maxLength=max;
    input.required=obligatorio;input.style.cssText=estilo;label.append(input);form.append(label);controles.push({ruta,input,numero:tipo==='number'});return input;
  }
  seccion('Datos personales');
  campo('usuario.nombres','Nombres',100,'text',true);campo('usuario.apellidos','Apellidos',100,'text',true);campo('usuario.telefono','Teléfono personal',20,'tel');
  let parentesco:HTMLSelectElement|null=null;
  if(rol==='ACOMPANANTE'&&data.acompanante){seccion('Perfil profesional');campo('acompanante.carrera','Carrera',150,'text',true);campo('acompanante.ciclo','Ciclo académico',20,'number');campo('acompanante.biografia','Biografía',3000,'textarea');}
  if(rol==='ADULTO_MAYOR'&&data.adultoMayor){seccion('Perfil de adulto mayor');campo('adultoMayor.direccionResidencia','Dirección de residencia',1000,'textarea');campo('adultoMayor.contactoEmergencia','Contacto de emergencia',100,'text',true);campo('adultoMayor.telefonoEmergencia','Teléfono de emergencia',20,'tel',true);campo('adultoMayor.observacionesMedicas','Observaciones médicas',3000,'textarea');}
  if(rol==='FAMILIAR'&&data.familiar){
    seccion('Perfil familiar');const label=document.createElement('label');label.textContent='Parentesco principal';parentesco=document.createElement('select');parentesco.style.cssText=estilo;label.append(parentesco);form.append(label);controles.push({ruta:'familiar.parentescoPrincipalId',input:parentesco,numero:true});campo('familiar.telefonoAlternativo','Teléfono alternativo',20,'tel');
  }
  if(rol==='SOPORTE'&&data.soporte){seccion('Perfil de soporte');campo('soporte.extensionTelefonica','Extensión telefónica',20);}
  const nota=document.createElement('p');nota.textContent='Los datos de identificación, correo, permisos y asignaciones institucionales requieren revisión administrativa. Las tarifas se gestionan en su módulo.';nota.style.fontSize='13px';form.append(nota);
  const estado=document.createElement('p');estado.setAttribute('role','status');estado.setAttribute('aria-live','polite');
  const botones=document.createElement('div');botones.style.cssText='display:flex;gap:10px;justify-content:flex-end;flex-wrap:wrap';
  const cancelar=document.createElement('button');cancelar.type='button';cancelar.textContent='Cancelar';
  const guardar=document.createElement('button');guardar.type='submit';guardar.textContent='Guardar cambios';
  cancelar.style.cssText='border:0;border-radius:8px;padding:11px;background:#e3f1eb;color:#087a70;cursor:pointer';
  guardar.style.cssText='border:0;border-radius:8px;padding:11px;background:#087a70;color:white;cursor:pointer';
  botones.append(cancelar,guardar);form.append(estado,botones);modal.append(form);const guia=document.createElement('section');guia.className='perfil-guia';
  const encabezado=document.createElement('h3');encabezado.textContent='Tu cuenta, en un solo lugar';
  const intro=document.createElement('p');intro.textContent='Consulta tus datos, mantén tu información actualizada y utiliza los accesos de tu rol.';
  const pasos=document.createElement('ol');
  for(const [titulo,descripcion] of [['01 · Revisa','Comprueba tus datos personales y los de tu perfil.'],['02 · Edita','Actualiza únicamente los campos que necesitas cambiar.'],['03 · Guarda','Verifica la confirmación y vuelve a tus actividades.']]){const li=document.createElement('li');const strong=document.createElement('strong');strong.textContent=titulo;const texto=document.createElement('p');texto.textContent=descripcion;li.append(strong,texto);pasos.append(li);}
  guia.append(encabezado,intro,pasos);host.prepend(guia);
  const acciones=document.createElement('section');acciones.className='perfil-accesos';
  const tituloAcciones=document.createElement('h3');tituloAcciones.textContent='Gestiona tu cuenta';acciones.append(tituloAcciones);
  const tarjeta=document.createElement('div');tarjeta.className='perfil-acceso';const texto=document.createElement('div');const nombre=document.createElement('strong');nombre.textContent='Información personal y del perfil';const descripcion=document.createElement('p');descripcion.textContent='Revisa y modifica tus datos permitidos.';texto.append(nombre,descripcion);tarjeta.append(texto,abrir);acciones.append(tarjeta);
  const anteriores=Array.from(host.querySelectorAll<HTMLElement>('.comp-profile-actions,.support-profile-actions,.familiar-profile-action,.adult-profile-action'));
  for(const anterior of anteriores){const elementos=anterior.matches('button')?[anterior]:Array.from(anterior.querySelectorAll<HTMLElement>('button,a'));for(const el of elementos){const fila=document.createElement('div');fila.className='perfil-acceso';const txt=document.createElement('div');const label=document.createElement('strong');label.textContent=el.tagName==='BUTTON'?'Fotografía de perfil':'Herramientas de tu rol';const nota=document.createElement('p');nota.textContent=el.tagName==='BUTTON'?'Mantén actualizada tu imagen de cuenta.':'Accede a las funciones de trabajo relacionadas.';txt.append(label,nota);fila.append(txt,el);acciones.append(fila);}if(!anterior.matches('button'))anterior.remove();}
  host.append(acciones,modal);
  const aviso=document.createElement('p');aviso.setAttribute('role','status');host.append(aviso);
  const valor=(ruta:string)=>ruta.split('.').reduce<any>((v,k)=>v?.[k],actual);
  modal.addEventListener('datos-perfil',e=>{actual=(e as CustomEvent).detail;});
  abrir.onclick=async()=>{
    estado.textContent='';modal.showModal();guardar.disabled=true;
    if(parentesco){parentesco.replaceChildren();parentesco.disabled=true;const opt=new Option(actual.familiar?.parentescoPrincipalNombre||'Sin parentesco',String(actual.familiar?.parentescoPrincipalId??''));parentesco.add(opt);
      try{const lista=await apiSegura('/usuarios/me/parentescos');parentesco.replaceChildren(new Option('Selecciona un parentesco',''));for(const item of lista)parentesco.add(new Option(item.nombre,String(item.id)));parentesco.disabled=false;}
      catch(e){estado.textContent='No se pudo cargar el catálogo de parentescos. Puedes editar otros campos.';}
    }
    for(const c of controles)c.input.value=String(valor(c.ruta)??'');
    guardar.disabled=false;
  };
  cancelar.onclick=()=>{if(!guardando)modal.close();};cerrar.onclick=()=>{if(!guardando)modal.close();};modal.addEventListener('cancel',e=>{if(guardando)e.preventDefault();});
  form.onsubmit=async(e)=>{
    e.preventDefault();if(guardando)return;
    const payload:Record<string,any>={};
    for(const c of controles){
      if(c.input.disabled)continue;
      const raw=c.input.value.trim();if(raw===String(valor(c.ruta)??''))continue;
      if(c.numero&&!raw)continue;
      const [grupo,key]=c.ruta.split('.');const v=c.numero?Number(raw):raw;
      if(grupo==='usuario')payload[key]=v;else (payload[grupo]??={})[key]=v;
    }
    if(!Object.keys(payload).length){modal.close();return;}
    guardando=true;guardar.disabled=true;cancelar.disabled=true;cerrar.disabled=true;estado.textContent='Guardando cambios…';
    try{actual=await apiSegura('/usuarios/me/perfil',{method:'PUT',body:JSON.stringify(payload)});renderProfileFields(actual);document.dispatchEvent(new CustomEvent('seniorlink:profile',{detail:actual}));modal.close();aviso.textContent='Perfil actualizado correctamente.';}
    catch(e){estado.textContent=e instanceof Error?e.message:String(e);}
    finally{guardando=false;guardar.disabled=false;cancelar.disabled=false;cerrar.disabled=false;}
  };
}
