import { adminRequest } from './admin-api';
import { API_URL, getSession } from './auth.js';

type AnyRecord = Record<string, any>;
const ROLES = ['ADMINISTRADOR','ACOMPANANTE','ADULTO_MAYOR','SOPORTE','FAMILIAR'];
const LABELS: Record<string,string> = {
  ADMINISTRADOR:'Administrador',ACOMPANANTE:'Acompañante',
  ADULTO_MAYOR:'Adulto mayor',SOPORTE:'Soporte',FAMILIAR:'Familiar'
};
const PROFILE_FIELDS: Record<string,Array<[string,string,string]>> = {
  ADMINISTRADOR:[['areaODepartamento','Área o departamento','text'],['nivelAcceso','Nivel de acceso','number']],
  ACOMPANANTE:[['carrera','Carrera','text'],['ciclo','Ciclo','number'],['tarifaBasePorHora','Tarifa base por hora','number'],['tarifaActualPorHora','Tarifa actual por hora','number'],['biografia','Biografía','text']],
  ADULTO_MAYOR:[['fechaNacimiento','Fecha de nacimiento','date'],['direccionResidencia','Dirección','text'],['contactoEmergencia','Contacto de emergencia','text'],['telefonoEmergencia','Teléfono de emergencia','text'],['observacionesMedicas','Observaciones médicas','text']],
  SOPORTE:[['turno','Turno','text'],['extensionTelefonica','Extensión','text']],
  FAMILIAR:[['parentescoPrincipalId','ID parentesco (catálogo)','number'],['telefonoAlternativo','Teléfono alternativo','text']]
};
const PROFILE_KEY: Record<string,string> = {
  ADMINISTRADOR:'admin',ACOMPANANTE:'acompanante',ADULTO_MAYOR:'adultoMayor',
  SOPORTE:'soporte',FAMILIAR:'familiar'
};
const $ = <T extends HTMLElement>(root: ParentNode, selector: string): T | null => root.querySelector<T>(selector);
function node(tag: string, text = '', className = ''): HTMLElement {
  const el = document.createElement(tag);
  el.textContent = text;
  if (className) el.className = className;
  return el;
}
function action(label: string, handler: () => void, cls = ''): HTMLButtonElement {
  const btn = document.createElement('button');
  btn.type = 'button'; btn.textContent = label; btn.className = 'admin-btn ' + cls;
  btn.style.marginRight = '6px';
  btn.addEventListener('click', handler);
  return btn;
}
function status(root: HTMLElement, message: string, error = false) {
  const p = $<HTMLElement>(root, '#module-message');
  if (p) { p.textContent = message; p.classList.toggle('error',error); }
}
function field(key: string,label: string,type = 'text',required = false): HTMLLabelElement {
  const l = document.createElement('label'); l.textContent = label;
  const input = document.createElement('input');
  input.name = key; input.type = type; input.className = 'admin-filter'; input.required = required;
  if (type === 'number') { input.step = 'any'; input.min = '0'; }
  l.appendChild(input); return l;
}
function values(form: HTMLFormElement): AnyRecord {
  const data: AnyRecord = {};
  for (const el of Array.from(form.elements)) {
    if (!(el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement)) continue;
    if (!el.name || el.disabled) continue;
    if (el instanceof HTMLInputElement && el.type === 'checkbox') data[el.name] = el.checked;
    else if (el.value.trim() !== '') data[el.name] = el.type === 'number' ? Number(el.value) : el.value.trim();
  }
  return data;
}
function fill(form: HTMLFormElement, data: AnyRecord) {
  for (const el of Array.from(form.elements)) {
    if (!(el instanceof HTMLInputElement || el instanceof HTMLSelectElement || el instanceof HTMLTextAreaElement)) continue;
    if (!el.name) continue;
    const value = data[el.name];
    if (el instanceof HTMLInputElement && el.type === 'checkbox') el.checked = Boolean(value);
    else if (value != null) el.value = String(value);
  }
}
function formBox(root: HTMLElement, title: string, fields: Array<[string,string,string]>,
                 defaults: AnyRecord, onSave: (data: AnyRecord) => Promise<void>) {
  const container = $<HTMLElement>(root,'#module-content');
  if (!container) return;
  $<HTMLElement>(container,'#editor')?.remove();
  const panel = node('section','','admin-shell'); panel.id='editor';
  panel.appendChild(node('h3',title));
  const form = document.createElement('form'); form.className='admin-form';
  for (const [key,label,type] of fields) form.appendChild(field(key,label,type));
  fill(form,defaults);
  const actions = node('div'); actions.style.cssText='grid-column:1/-1;display:flex;gap:9px';
  const save = document.createElement('button'); save.type='submit';save.className='admin-btn';save.textContent='Guardar';
  actions.append(save,action('Cancelar',()=>panel.remove(),'admin-secondary'));
  form.appendChild(actions);panel.appendChild(form);container.prepend(panel);
  form.addEventListener('submit',async event=>{
    event.preventDefault();save.disabled=true;
    try { await onSave(values(form)); panel.remove(); status(root,'Cambios guardados correctamente.'); }
    catch(e){status(root,e instanceof Error?e.message:'Error al guardar.',true);}
    finally{save.disabled=false;}
  });
  panel.scrollIntoView({behavior:'smooth',block:'nearest'});
}
function table(root: HTMLElement, headers: string[], rows: AnyRecord[], render: (row: AnyRecord,tr: HTMLTableRowElement)=>void) {
  const content = $<HTMLElement>(root,'#module-content');
  if (!content) return;
  $<HTMLElement>(content,'#listing')?.remove();
  const wrap=node('div','','admin-table-wrap');wrap.id='listing';
  const t=document.createElement('table');t.className='admin-table';
  const head=document.createElement('thead');const hr=document.createElement('tr');
  for(const h of headers)hr.appendChild(node('th',h));
  head.appendChild(hr);t.appendChild(head);
  const body=document.createElement('tbody');
  for(const row of rows){const tr=document.createElement('tr');render(row,tr);body.appendChild(tr);}
  t.appendChild(body);wrap.appendChild(t);content.appendChild(wrap);
}
function td(tr: HTMLTableRowElement,value:unknown) {
  tr.appendChild(node('td',value == null || value === '' ? '—' : String(value)));
}
function topButton(root:HTMLElement,label:string,callback:()=>void) {
  const content=$<HTMLElement>(root,'#module-content');if(!content)return;
  let actions=$<HTMLElement>(content,'#module-actions');
  if(!actions){actions=node('div','','admin-links');actions.id='module-actions';content.prepend(actions);}
  actions.appendChild(action(label,callback));
}
async function usersModule(root: HTMLElement) {
  const filter=root.dataset.roleFilter || '';
  const content=$<HTMLElement>(root,'#module-content');
  if(!content)return;
  const roleSelect=document.createElement('select');roleSelect.className='admin-filter';
  roleSelect.style.maxWidth='250px';
  for(const r of ['',...ROLES]){const o=document.createElement('option');o.value=r;o.textContent=r?LABELS[r]:'Todos los roles';roleSelect.appendChild(o);}
  roleSelect.value=filter;roleSelect.disabled=Boolean(filter);
  const search=document.createElement('input');search.className='admin-filter';search.placeholder='Buscar por DNI, nombre o correo';search.style.maxWidth='360px';
  const filters=node('div','','admin-links');filters.append(roleSelect,search);content.appendChild(filters);
  let all:AnyRecord[]=[];
  const render=()=>{
    const q=search.value.trim().toLowerCase();
    const list=all.filter(u=>(!roleSelect.value||u.rolNombre===roleSelect.value)&&
      [u.dni,u.nombres,u.apellidos,u.correoElectronico].some(x=>String(x||'').toLowerCase().includes(q)));
    table(root,['DNI','Nombre','Correo','Rol','Estado','Acciones'],list,(u,tr)=>{
      td(tr,u.dni);td(tr,[u.nombres,u.apellidos].join(' '));td(tr,u.correoElectronico);
      td(tr,LABELS[u.rolNombre]||u.rolNombre);td(tr,u.estaActivo?'Activo':'Inactivo');
      const actions=document.createElement('td');
      actions.append(action('Ver',()=>void viewUser(u.dni)));
      actions.append(action('Editar',()=>void editUser(u.dni),'admin-secondary'));
      actions.append(action(u.estaActivo?'Desactivar':'Activar',()=>void toggleUser(u),'admin-danger'));
      tr.appendChild(actions);
    });
    status(root,list.length+' usuario(s) encontrados.');
  };
  const refresh=async()=>{all=await adminRequest<AnyRecord[]>('/usuarios');render();};
  const viewUser=async(dni:string)=>{
    try {
      const d=await adminRequest<AnyRecord>('/usuarios/'+encodeURIComponent(dni));
      const profile=d[PROFILE_KEY[d.usuario.rolNombre]];
      const details={...d.usuario,...(profile||{})};
      const existing=$<HTMLElement>(root,'#user-detail');existing?.remove();
      const panel=node('section','','admin-shell');panel.id='user-detail';
      panel.appendChild(node('h3','Detalle del usuario · '+dni));
      for(const [k,v] of Object.entries(details)){
        if(v!==null && typeof v!=='object')panel.appendChild(node('p',k+': '+String(v),'admin-note'));
      }
      panel.appendChild(action('Cerrar',()=>panel.remove(),'admin-secondary'));
      content.prepend(panel);
    }catch(e){status(root,e instanceof Error?e.message:'No se pudo consultar.',true);}
  };
  const editUser=async(dni:string)=>{
    try{
      const d=await adminRequest<AnyRecord>('/usuarios/'+encodeURIComponent(dni));
      const role=d.usuario.rolNombre;
      const fields:Array<[string,string,string]>=[
        ['nombres','Nombres','text'],['apellidos','Apellidos','text'],
        ['correoElectronico','Correo electrónico','email'],['telefono','Teléfono','text'],
        ...PROFILE_FIELDS[role].map(([key,label,type])=>['profile_'+key,label,type] as [string,string,string])
      ];
      const defaults:AnyRecord={...d.usuario};
      for(const [key] of PROFILE_FIELDS[role])defaults['profile_'+key]=d[PROFILE_KEY[role]]?.[key];
      formBox(root,'Editar '+LABELS[role]+' · '+dni,fields,defaults,async vals=>{
        const common:AnyRecord={};
        for(const key of ['nombres','apellidos','correoElectronico','telefono'])if(key in vals)common[key]=vals[key];
        const profile:AnyRecord={};
        for(const [key] of PROFILE_FIELDS[role])if('profile_'+key in vals)profile[key]=vals['profile_'+key];
        common[PROFILE_KEY[role]]=profile;
        await adminRequest('/usuarios/'+dni,{method:'PUT',body:JSON.stringify(common)});
        await refresh();
      });
    }catch(e){status(root,e instanceof Error?e.message:'Error al abrir el usuario.',true);}
  };
  const toggleUser=async(u:AnyRecord)=>{
    if(!window.confirm((u.estaActivo?'¿Desactivar':'¿Activar')+' a '+u.nombres+' '+u.apellidos+'?'))return;
    try{
      if(u.estaActivo)await adminRequest('/usuarios/'+u.dni,{method:'DELETE'});
      else await adminRequest('/usuarios/'+u.dni,{method:'PUT',body:JSON.stringify({estaActivo:true})});
      await refresh();
    }catch(e){status(root,e instanceof Error?e.message:'Error al cambiar estado.',true);}
  };
  const createUser=()=>{
    const role=filter||roleSelect.value||'ACOMPANANTE';
    const fields:Array<[string,string,string]>=[
      ...(!filter?[['rol','Rol (ADMINISTRADOR, ACOMPANANTE, ADULTO_MAYOR, SOPORTE, FAMILIAR)','text'] as [string,string,string]]:[]),
      ['dni','DNI','text'],['nombres','Nombres','text'],['apellidos','Apellidos','text'],
      ['correoElectronico','Correo electrónico','email'],['contrasena','Contraseña inicial','password'],
      ['telefono','Teléfono','text'],
      ...PROFILE_FIELDS[role].map(([key,label,type])=>['profile_'+key,label,type] as [string,string,string]),
      ...(role==='ACOMPANANTE'?[['profile_institucionRuc','RUC institución (opcional)','text'] as [string,string,string]]:[])
    ];
    formBox(root,'Crear cuenta · '+LABELS[role],fields,{rol:role},async vals=>{
      const chosen=String(vals.rol||role).toUpperCase();
      if(chosen!==role)throw new Error('Selecciona el rol en el filtro y vuelve a abrir el formulario.');
      const data:AnyRecord={rol:chosen};
      for(const key of ['dni','nombres','apellidos','correoElectronico','contrasena','telefono'])data[key]=vals[key]||null;
      const profile:AnyRecord={};
      for(const [key] of PROFILE_FIELDS[role])if('profile_'+key in vals)profile[key]=vals['profile_'+key];
      if('profile_institucionRuc' in vals)profile.institucionRuc=vals.profile_institucionRuc;
      data[PROFILE_KEY[role]]=profile;
      if(role==='SOPORTE'||role==='ADMINISTRADOR'){
        Object.assign(data,profile);delete data[PROFILE_KEY[role]];
        await adminRequest('/personal',{method:'POST',body:JSON.stringify(data)});
      }else{
        await adminRequest('/usuarios',{method:'POST',body:JSON.stringify(data)});
      }
      await refresh();
    });
  };
  topButton(root,'+ Crear usuario',createUser);
  roleSelect.addEventListener('change',render);search.addEventListener('input',render);
  await refresh();
}
async function reservationsModule(root:HTMLElement){
  const refresh=async()=>{
    const rows=await adminRequest<AnyRecord[]>('/reservas');
    table(root,['ID','Cliente','Acompañante','Inicio','Estado','Total','Acciones'],rows,(r,tr)=>{
      td(tr,r.id);td(tr,r.clienteNombre);td(tr,r.acompananteNombre);
      td(tr,new Date(r.fechaHoraInicio).toLocaleString('es-PE'));
      td(tr,r.estado);td(tr,'S/ '+Number(r.montoTotal||0).toFixed(2));
      const a=document.createElement('td');
      a.append(action('Ver',()=>window.alert('Reserva #'+r.id+'\nCliente: '+r.clienteNombre+'\nAcompañante: '+r.acompananteNombre+'\nDirección: '+r.direccionEncuentro+'\nEstado: '+r.estado)));
      a.append(action('Cambiar estado',()=>{
        formBox(root,'Actualizar reserva #'+r.id,[['estado','Estado: PENDIENTE_PAGO, CONFIRMADA, EN_PROGRESO, COMPLETADA, CANCELADA','text'],['motivo','Motivo','text']],{estado:r.estado.replaceAll(' ','_')},async data=>{
          await adminRequest('/reservas/'+r.id+'/estado',{method:'PATCH',body:JSON.stringify(data)});
          await refresh();
        });
      },'admin-secondary'));tr.appendChild(a);
    });
    status(root,rows.length+' reserva(s) registradas.');
  };
  await refresh();
}
async function institutionsModule(root:HTMLElement){
  const fields:Array<[string,string,string]>=[['ruc','RUC (11 dígitos)','text'],['nombre','Nombre','text'],['detallesConvenio','Detalles del convenio','text']];
  const refresh=async()=>{
    const rows=await adminRequest<AnyRecord[]>('/instituciones');
    table(root,['RUC','Institución','Convenio','Acciones'],rows,(r,tr)=>{
      td(tr,r.ruc);td(tr,r.nombre);td(tr,r.detallesConvenio);
      const a=document.createElement('td');
      a.append(action('Editar',()=>formBox(root,'Editar institución',fields,r,async data=>{
        await adminRequest('/instituciones/'+r.ruc,{method:'PUT',body:JSON.stringify(data)});await refresh();
      })));
      a.append(action('Eliminar',async()=>{
        if(!confirm('¿Eliminar institución '+r.nombre+'?'))return;
        try{await adminRequest('/instituciones/'+r.ruc,{method:'DELETE'});await refresh();}
        catch(e){status(root,e instanceof Error?e.message:'No se puede eliminar.',true);}
      },'admin-danger'));tr.appendChild(a);
    });status(root,rows.length+' institución(es).');
  };
  topButton(root,'+ Añadir institución',()=>formBox(root,'Nueva institución',fields,{},async data=>{
    await adminRequest('/instituciones',{method:'POST',body:JSON.stringify(data)});await refresh();
  }));
  await refresh();
}
async function promotionsModule(root:HTMLElement){
  const fields:Array<[string,string,string]>=[
    ['codigoCupon','Código de cupón','text'],['descripcion','Descripción','text'],
    ['tipo','Tipo: PORCENTAJE o MONTO_FIJO','text'],['valor','Valor','number'],
    ['montoMinimoOrden','Monto mínimo','number'],['montoMaximoDescuento','Descuento máximo (opcional)','number'],
    ['limiteUsos','Límite de usos (opcional)','number'],['validoDesde','Válido desde','datetime-local'],
    ['validoHasta','Válido hasta','datetime-local']
  ];
  const save=async(id:string|undefined,data:AnyRecord)=>{
    for(const key of ['validoDesde','validoHasta'])if(data[key])data[key]=new Date(data[key]).toISOString();
    data.estaActivo=true;
    await adminRequest('/promociones'+(id?'/'+id:''),{method:id?'PUT':'POST',body:JSON.stringify(data)});
  };
  const refresh=async()=>{
    const rows=await adminRequest<AnyRecord[]>('/promociones');
    table(root,['Cupón','Tipo','Valor','Vigencia','Estado','Acciones'],rows,(r,tr)=>{
      td(tr,r.codigoCupon);td(tr,r.tipo);td(tr,r.valor);
      td(tr,new Date(r.validoDesde).toLocaleDateString('es-PE')+' – '+new Date(r.validoHasta).toLocaleDateString('es-PE'));
      td(tr,r.estaActivo?'Activo':'Inactivo');
      const a=document.createElement('td');
      a.append(action('Editar',()=>{
        const defaults={...r};
        for(const key of ['validoDesde','validoHasta'])defaults[key]=String(r[key]||'').slice(0,16);
        formBox(root,'Editar promoción',fields,defaults,async data=>{await save(r.id,data);await refresh();});
      }));
      if(r.estaActivo)a.append(action('Desactivar',async()=>{
        if(!confirm('¿Desactivar el cupón '+r.codigoCupon+'?'))return;
        try{await adminRequest('/promociones/'+r.id,{method:'DELETE'});await refresh();}
        catch(e){status(root,e instanceof Error?e.message:'No se pudo desactivar.',true);}
      },'admin-danger'));tr.appendChild(a);
    });status(root,rows.length+' promoción(es).');
  };
  topButton(root,'+ Crear promoción',()=>formBox(root,'Nueva promoción',fields,{tipo:'PORCENTAJE'},async data=>{await save(undefined,data);await refresh();}));
  await refresh();
}
export async function initAdminModule(root:HTMLElement):Promise<void>{
  try {
    switch(root.dataset.adminModule){
      case 'usuarios':await usersModule(root);break;
      case 'reservas':await reservationsModule(root);break;
      case 'instituciones':await institutionsModule(root);break;
      case 'promociones':await promotionsModule(root);break;
    }
  } catch(e){status(root,e instanceof Error?e.message:'No se pudo cargar el módulo.',true);}
}
