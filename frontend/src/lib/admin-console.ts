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
function ensureEditorStyles() {
  if (document.getElementById('admin-modal-styles')) return;
  const style=document.createElement('style');
  style.id='admin-modal-styles';
  style.textContent=`
  .admin-modal-overlay{position:fixed;inset:0;z-index:10000;display:flex;align-items:center;justify-content:center;background:rgba(12,35,32,.55);backdrop-filter:blur(3px);padding:22px}
  .admin-modal-dialog{width:min(760px,100%);max-height:min(88vh,900px);display:flex;flex-direction:column;overflow:hidden;background:#fff;border-radius:20px;box-shadow:0 25px 90px rgba(0,0,0,.24);border:1px solid #dceae5}
  .admin-modal-header{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;padding:22px 26px;border-bottom:1px solid #e6efeb;background:#f8fcfa}
  .admin-modal-heading{margin:0;font-size:20px;color:#173d39;line-height:1.4}
  .admin-modal-subtitle{margin:6px 0 0;font-size:12px;color:#708b81}
  .admin-modal-close{border:0;border-radius:9px;background:#e9f4ef;color:#37665b;width:35px;height:35px;font-size:23px;line-height:1;cursor:pointer;flex:none}
  .admin-modal-form{display:flex;flex-direction:column;min-height:0;flex:1}
  .admin-modal-fields{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:17px 20px;padding:25px 26px;overflow-y:auto;min-height:0}
  .admin-modal-fields label{display:block;font-size:12px;font-weight:600;color:#54736a;min-width:0}
  .admin-modal-fields input,.admin-modal-fields select,.admin-modal-fields textarea{display:block;width:100%;margin-top:8px;padding:12px;border:1px solid #d5e7df;border-radius:10px;font:inherit;background:white;color:#15302e}
  .admin-modal-fields input:focus,.admin-modal-fields select:focus,.admin-modal-fields textarea:focus{outline:2px solid #83d1bd;outline-offset:1px}
  .admin-modal-actions{display:flex;justify-content:flex-end;gap:10px;flex-wrap:wrap;padding:18px 26px;border-top:1px solid #e6efeb;background:#fff}
  .admin-modal-actions .admin-btn{margin:0!important;min-width:110px}
  .admin-modal-error{color:#b42336;font-size:12px;margin:0;padding:0 26px 12px}
  @media(max-width:600px){.admin-modal-overlay{padding:10px}.admin-modal-dialog{max-height:94vh;border-radius:14px}.admin-modal-header{padding:17px}.admin-modal-fields{grid-template-columns:1fr;padding:18px;gap:14px}.admin-modal-actions{padding:15px}}
  `;
  document.head.appendChild(style);
}
function formBox(root: HTMLElement, title: string, fields: Array<[string,string,string]>,
                 defaults: AnyRecord, onSave: (data: AnyRecord) => Promise<void>) {
  ensureEditorStyles();
  document.querySelector<HTMLElement>('#admin-modal-overlay')?.remove();
  const overlay=node('div','','admin-modal-overlay');
  overlay.id='admin-modal-overlay';
  const dialog=node('section','','admin-modal-dialog');
  dialog.setAttribute('role','dialog');
  dialog.setAttribute('aria-modal','true');
  const headingId='admin-modal-heading';
  dialog.setAttribute('aria-labelledby',headingId);
  const header=node('div','','admin-modal-header');
  const heading=node('div');
  const titleNode=node('h2',title,'admin-modal-heading');
  titleNode.id=headingId;
  heading.append(titleNode,node('p','Modifica los campos y guarda los cambios.','admin-modal-subtitle'));
  const closeBtn=node('button','×','admin-modal-close') as HTMLButtonElement;
  closeBtn.type='button';closeBtn.setAttribute('aria-label','Cerrar ventana');
  header.append(heading,closeBtn);
  const form=document.createElement('form');
  form.className='admin-modal-form';
  const fieldGrid=node('div','','admin-modal-fields');
  for(const [key,label,type] of fields)fieldGrid.appendChild(field(key,label,type));
  form.appendChild(fieldGrid);
  fill(form,defaults);
  const error=node('p','','admin-modal-error');
  error.setAttribute('role','alert');error.hidden=true;
  const actions=node('div','','admin-modal-actions');
  const save=document.createElement('button');
  save.type='submit';save.className='admin-btn';save.textContent='Guardar cambios';
  const cancel=action('Cancelar',()=>close(),'admin-secondary');
  actions.append(cancel,save);
  form.append(error,actions);
  dialog.append(header,form);overlay.appendChild(dialog);
  const previousFocus=document.activeElement instanceof HTMLElement?document.activeElement:null;
  const previousOverflow=document.body.style.overflow;
  document.body.style.overflow='hidden';
  document.body.appendChild(overlay);
  let busy=false;
  function close(){
    if(busy)return;
    overlay.remove();
    document.body.style.overflow=previousOverflow;
    previousFocus?.focus();
    document.removeEventListener('keydown',onKey);
  }
  function onKey(event:KeyboardEvent){
    if(event.key==='Escape'){event.preventDefault();close();}
    if(event.key!=='Tab')return;
    const focusable=Array.from(dialog.querySelectorAll<HTMLElement>('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled])'));
    if(!focusable.length)return;
    const first=focusable[0],last=focusable[focusable.length-1];
    if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}
    else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
  }
  closeBtn.addEventListener('click',close);
  overlay.addEventListener('click',event=>{if(event.target===overlay)close();});
  document.addEventListener('keydown',onKey);
  (fieldGrid.querySelector('input,select,textarea') as HTMLElement|null)?.focus() || closeBtn.focus();
  form.addEventListener('submit',async event=>{
    event.preventDefault();if(busy)return;
    busy=true;save.disabled=true;cancel.disabled=true;closeBtn.disabled=true;
    error.hidden=true;
    try{
      await onSave(values(form));
      busy=false;
      close();
      status(root,'Cambios guardados correctamente.');
    }catch(e){
      error.textContent=e instanceof Error?e.message:'Error al guardar.';
      error.hidden=false;
      busy=false;
    }finally{save.disabled=false;cancel.disabled=false;closeBtn.disabled=false;}
  });
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
const PROFILE_COLUMNS: Record<string, Array<[string,string]>> = {
  ACOMPANANTE:[['carrera','Carrera'],['ciclo','Ciclo'],['tarifaActualPorHora','Tarifa/hora'],['promedioCalificacion','Calificación']],
  ADULTO_MAYOR:[['fechaNacimiento','Nacimiento'],['direccionResidencia','Dirección'],['contactoEmergencia','Contacto de emergencia'],['telefonoEmergencia','Teléfono emergencia'],['observacionesMedicas','Observaciones médicas']],
  FAMILIAR:[['parentescoPrincipalNombre','Parentesco'],['telefonoAlternativo','Teléfono alternativo']],
  SOPORTE:[['turno','Turno'],['extensionTelefonica','Extensión'],['casosAtendidos','Casos atendidos']],
  ADMINISTRADOR:[['areaODepartamento','Área'],['nivelAcceso','Nivel de acceso']]
};
function displayProfileValue(profile: AnyRecord, key: string): string {
  const value = profile?.[key];
  return value === null || value === undefined || value === '' ? 'No registrado' : String(value);
}
function detailPanel(root: HTMLElement, data: AnyRecord, relatives: AnyRecord[] = []): void {
  const content = $<HTMLElement>(root,'#module-content');
  if (!content || !data?.usuario) return;
  $<HTMLElement>(content,'#user-detail')?.remove();
  const user = data.usuario as AnyRecord;
  const role = String(user.rolNombre || '');
  const profile = data[PROFILE_KEY[role]] as AnyRecord | null | undefined;
  const panel = node('section','','admin-profile-detail');
  panel.id = 'user-detail';
  const header = node('div','','admin-profile-header');
  const identity = node('div');
  identity.append(
    node('div',LABELS[role] || role,'admin-profile-eyebrow'),
    node('h3',[user.nombres,user.apellidos].filter(Boolean).join(' ') || 'Usuario sin nombre'),
    node('p','DNI: '+(user.dni || 'No registrado'),'admin-note')
  );
  const close = action('Cerrar ficha',()=>panel.remove(),'admin-secondary');
  header.append(identity,close);
  panel.appendChild(header);

  const format = (value: unknown, key: string): string => {
    if (value === null || value === undefined || value === '') return 'No registrado';
    if (typeof value === 'boolean') return value ? 'Sí' : 'No';
    if (key === 'creadoEn' || key === 'actualizadoEn') {
      const date = new Date(String(value));
      return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('es-PE');
    }
    if (key === 'fechaNacimiento') {
      const parts = String(value).split('-');
      return parts.length === 3 ? parts.reverse().join('/') : String(value);
    }
    return String(value);
  };
  const section = (title: string, entries: Array<[string,string,unknown]>) => {
    const block = node('section','','admin-profile-section');
    block.appendChild(node('h4',title));
    const grid = node('div','','admin-profile-grid');
    for (const [key,label,value] of entries) {
      const item = node('div','','admin-profile-field');
      item.append(node('span',label,'admin-profile-label'),node('strong',format(value,key)));
      grid.appendChild(item);
    }
    block.appendChild(grid);
    panel.appendChild(block);
  };
  const userLabels: Record<string,string> = {
    dni:'DNI',rolId:'ID del rol',rolNombre:'Rol',correoElectronico:'Correo electrónico',
    nombres:'Nombres',apellidos:'Apellidos',telefono:'Teléfono',
    estaActivo:'Cuenta activa',creadoEn:'Fecha de registro',actualizadoEn:'Última actualización'
  };
  section('Información general del usuario',
    Object.entries(user).map(([key,value])=>[key,userLabels[key] || key,value] as [string,string,unknown]));

  const profileLabels: Record<string,string> = {
    areaODepartamento:'Área o departamento',nivelAcceso:'Nivel de acceso',
    carrera:'Carrera',ciclo:'Ciclo',tarifaBasePorHora:'Tarifa base por hora (S/)',
    tarifaActualPorHora:'Tarifa actual por hora (S/)',biografia:'Biografía',
    promedioCalificacion:'Calificación promedio',totalCalificaciones:'Total de calificaciones',
    fechaNacimiento:'Fecha de nacimiento',direccionResidencia:'Dirección de residencia',
    contactoEmergencia:'Contacto de emergencia',telefonoEmergencia:'Teléfono de emergencia',
    observacionesMedicas:'Observaciones médicas',parentescoPrincipalId:'ID de parentesco',
    parentescoPrincipalNombre:'Parentesco',telefonoAlternativo:'Teléfono alternativo',
    turno:'Turno',extensionTelefonica:'Extensión telefónica',casosAtendidos:'Casos atendidos'
  };
  if (profile) {
    section('Información específica del perfil · '+(LABELS[role] || role),
      [
        ...(PROFILE_FIELDS[role] ?? []).map(([key,label])=>[key,label,profile[key]] as [string,string,unknown]),
        ...Object.entries(profile).filter(([key])=>key!=='institucionAliada' &&
          !(PROFILE_FIELDS[role] ?? []).some(([known])=>known===key))
          .map(([key,value])=>[key,profileLabels[key] || key,value] as [string,string,unknown])
      ]);
    if (role === 'ACOMPANANTE') {
      const institution = profile.institucionAliada;
      if (institution && typeof institution === 'object') {
        const labels: Record<string,string> = {
          ruc:'RUC',nombre:'Institución',detallesConvenio:'Detalles del convenio'
        };
        section('Institución aliada',
          Object.entries(institution).map(([key,value])=>[key,labels[key] || key,value] as [string,string,unknown]));
      } else {
        section('Institución aliada',[['institucion','Institución vinculada',null]]);
      }
    }
  } else {
    const notice=node('p','El backend no devolvió información específica para este perfil.','admin-note');
    panel.appendChild(notice);
  }
  if (role === 'ADULTO_MAYOR' || role === 'FAMILIAR') {
    const linked = relatives.filter(v => v.estado === 'ACEPTADO');
    const pending = relatives.filter(v => v.estado !== 'ACEPTADO');
    const familyBlock = node('section','','admin-profile-section');
    familyBlock.appendChild(node('h4',role === 'ADULTO_MAYOR' ? 'Familiares vinculados' : 'Adultos mayores vinculados'));
    if (!linked.length) familyBlock.appendChild(node('p','No hay vinculaciones aceptadas registradas.','admin-note'));
    for (const familiar of linked) {
      const family = node('div','','admin-profile-family');
      const name = [familiar.nombres,familiar.apellidos].filter(Boolean).join(' ');
      family.appendChild(node('strong',name || 'Familiar'));
      const grid = node('div','','admin-profile-grid');
      for (const [label,value] of [
        ['DNI',familiar.dni],[role === 'FAMILIAR' ? 'Relación registrada del familiar' : 'Parentesco',familiar.parentesco],
        ['Teléfono',familiar.telefono],['Correo',familiar.correoElectronico],
        ['Vinculación',familiar.estado]
      ] as Array<[string,unknown]>) {
        const item=node('div','','admin-profile-field');
        item.append(node('span',label,'admin-profile-label'),
          node('strong',value == null || value === '' ? 'No registrado' : String(value)));
        grid.appendChild(item);
      }
      family.appendChild(grid);
      familyBlock.appendChild(family);
    }
    if (pending.length) {
      familyBlock.appendChild(node('h4','Otras solicitudes de vinculación'));
      for (const familiar of pending) {
        familyBlock.appendChild(node('p',
          [familiar.nombres,familiar.apellidos].filter(Boolean).join(' ') +
          ' · ' + (role === 'FAMILIAR' ? 'Adulto mayor vinculado' : (familiar.parentesco || 'Parentesco no registrado')) +
          ' · Estado: ' + (familiar.estado || 'No registrado'),'admin-note'));
      }
    }
    panel.appendChild(familyBlock);
  }
  panel.appendChild(node('p','La información se obtiene de los registros del backend. Los campos sin valor se indican como «No registrado».','admin-note'));
  content.prepend(panel);
  panel.scrollIntoView({behavior:'smooth',block:'start'});
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
  const filters=node('div','','admin-links');if(!filter)filters.appendChild(roleSelect);filters.appendChild(search);content.appendChild(filters);
  let all:AnyRecord[]=[];
  let profiles:Record<string,AnyRecord>={};
  let relations:Record<string,AnyRecord[]>={};
  const render=()=>{
    const q=search.value.trim().toLowerCase();
    const list=all.filter(u=>(!roleSelect.value||u.rolNombre===roleSelect.value)&&
      [u.dni,u.nombres,u.apellidos,u.correoElectronico].some(x=>String(x||'').toLowerCase().includes(q)));
    const specialized = Boolean(filter);
    const columns = specialized ? PROFILE_COLUMNS[filter] ?? [] : [];
    const hasRelations = filter === 'ADULTO_MAYOR' || filter === 'FAMILIAR';
    table(root,['DNI','Nombre','Correo','Teléfono',...(!specialized?['Rol']:[]),...columns.map(([,label])=>label),...(hasRelations? [filter === 'ADULTO_MAYOR' ? 'Familiares vinculados' : 'Adultos mayores vinculados']:[]),'Estado','Acciones'],list,(u,tr)=>{
      td(tr,u.dni);td(tr,[u.nombres,u.apellidos].join(' '));td(tr,u.correoElectronico);td(tr,u.telefono);
      if(!specialized) td(tr,LABELS[u.rolNombre] || u.rolNombre);
      const profile=profiles[u.dni]?.[PROFILE_KEY[u.rolNombre]];
      for (const [key] of columns) td(tr,profile ? displayProfileValue(profile,key) : 'No disponible');
      if (hasRelations) {
        const matches=relations[u.dni];
        td(tr,!matches ? 'No disponible' : matches.filter(v=>v.estado==='ACEPTADO')
          .map(v=>[v.nombres,v.apellidos].filter(Boolean).join(' ')+' ('+(filter === 'FAMILIAR' ? 'Adulto mayor' : (v.parentesco||'Sin parentesco'))+')')
          .join('; ') || 'Sin vinculaciones aceptadas');
      }
      td(tr,u.estaActivo?'Activo':'Inactivo');
      const actions=document.createElement('td');
      actions.className='admin-actions-cell';
      const buttons=node('div','','admin-actions-group');
      buttons.append(action('Ver perfil',()=>void viewUser(u.dni)));
      buttons.append(action('Editar',()=>void editUser(u.dni),'admin-secondary'));
      buttons.append(action(u.estaActivo?'Desactivar':'Activar',()=>void toggleUser(u),'admin-danger'));
      actions.appendChild(buttons);
      tr.appendChild(actions);
    });
    if (!list.length) {
      const listing=$<HTMLElement>(root,'#listing');
      listing?.appendChild(node('p',q?'No hay usuarios que coincidan con la búsqueda.':'No hay usuarios registrados en este perfil.','admin-note'));
    }
    if (!filter || Object.keys(profiles).length === all.length) status(root,list.length+' usuario(s) encontrados.');
  };
  const refresh=async()=>{
    status(root,'Cargando usuarios desde el backend...');
    all=await adminRequest<AnyRecord[]>(filter ? '/usuarios?rol='+encodeURIComponent(filter) : '/usuarios');
    if(filter) {
      const results=await Promise.allSettled(all.map(u=>adminRequest<AnyRecord>('/usuarios/'+encodeURIComponent(u.dni))));
      profiles={};
      results.forEach((result,i)=>{if(result.status==='fulfilled')profiles[all[i].dni]=result.value;});
      if (filter === 'ADULTO_MAYOR' || filter === 'FAMILIAR') {
        const suffix=filter === 'ADULTO_MAYOR' ? '/familiares' : '/adultos-mayores';
        const linked=await Promise.allSettled(all.map(u=>
          adminRequest<AnyRecord[]>('/usuarios/'+encodeURIComponent(u.dni)+suffix)));
        relations={};
        linked.forEach((result,i)=>{if(result.status==='fulfilled')relations[all[i].dni]=result.value;});
        if(linked.some(result=>result.status==='rejected'))
          status(root,'Algunas vinculaciones no pudieron cargarse. Verifica que el backend esté actualizado.',true);
      }
      const failed=results.filter(result=>result.status==='rejected').length;
      if(failed) status(root,failed+' perfil(es) no pudieron consultarse. Se muestran los usuarios disponibles.',true);
    }
    render();
  };
  const viewUser=async(dni:string)=>{
    try {
      const d=profiles[dni] ?? await adminRequest<AnyRecord>('/usuarios/'+encodeURIComponent(dni));
      let relatives: AnyRecord[] = [];
      if (d.usuario?.rolNombre === 'ADULTO_MAYOR' || d.usuario?.rolNombre === 'FAMILIAR') {
        try {
          relatives = await adminRequest<AnyRecord[]>('/usuarios/'+encodeURIComponent(dni)+(d.usuario.rolNombre === 'ADULTO_MAYOR' ? '/familiares' : '/adultos-mayores'));
        } catch (error) {
          status(root,'No se pudieron consultar las vinculaciones: '+String(error),true);
        }
      }
      detailPanel(root,d,relatives);
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
