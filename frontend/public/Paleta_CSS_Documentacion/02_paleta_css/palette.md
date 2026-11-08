# Paleta de colores — SeniorLink

**Proyecto:** Plataforma web de acompañamiento no clínico para personas adultas mayores.

La identidad combina **azul confianza**, **celeste claridad**, **verde menta bienestar** y superficies neutras. Funciona en el portal público, las cuentas familiares, el perfil del acompañante y el panel administrativo.

| Variable CSS | Color | HEX | Uso recomendado |
|---|---|---|---|
| `--color-primary` | Azul confianza | `#0B3D91` | Encabezados, botones principales, navegación |
| `--color-primary-hover` | Azul oscuro | `#082E70` | Interacción hover de botones primarios |
| `--color-primary-soft` | Azul pálido | `#EAF2FC` | Tarjetas informativas y fondos destacados |
| `--color-secondary` | Celeste claridad | `#1976D2` | Enlaces y acciones secundarias |
| `--color-secondary-hover` | Celeste oscuro | `#125CAA` | Hover de enlaces y acciones secundarias |
| `--color-accent` | Verde menta | `#4CAF87` | Detalles visuales y acentos no críticos |
| `--color-accent-soft` | Menta suave | `#E6F5EE` | Paneles de acompañamiento y bienestar |
| `--color-bg-light` | Gris muy claro | `#F7FAFC` | Fondo general de la web |
| `--color-surface` | Blanco | `#FFFFFF` | Tarjetas, ventanas modales, formularios |
| `--color-border` | Gris claro | `#D9E2EC` | Bordes y divisores |
| `--color-text-main` | Azul grisáceo profundo | `#243447` | Texto principal |
| `--color-text-soft` | Gris azulado | `#526477` | Texto de apoyo, descripciones |
| `--color-success` | Verde confirmación | `#24834B` | Textos de confirmación y estados completados |
| `--color-success-soft` | Verde tenue | `#E7F5EC` | Fondos de confirmaciones |
| `--color-warning-soft` | Amarillo crema | `#FFF3D6` | Fondo de avisos no críticos |
| `--color-warning-text` | Ocre oscuro | `#7A5000` | Texto de alertas suaves |
| `--color-danger` | Rojo sobrio | `#B42332` | Errores, validaciones y acciones destructivas |
| `--color-danger-soft` | Rosa pálido | `#FCEBED` | Fondo de mensajes de error |
| `--color-focus` | Azul foco | `#1976D2` | Contorno visible al navegar con teclado |

## Reglas de aplicación

1. **Portal público:** usar fondos claros, acentos menta y botones azules. Mostrar imágenes cálidas, sin saturación visual.
2. **Panel administrativo:** priorizar blanco, azul oscuro, bordes discretos y estados de color acompañados de texto.
3. **Lectura accesible:** preferir texto principal oscuro sobre blanco o gris muy claro. No usar verde menta como único color para texto pequeño.
4. **Personas mayores:** usar un tamaño de letra base de 16–18 px como punto de partida y áreas táctiles amplias (idealmente 44 × 44 px).
5. **Estados:** combinar etiquetas con color (por ejemplo, “Confirmada”, “Pendiente”, “Rechazada”) para no depender solamente del color.
6. **Contraste:** revisar cada combinación final con WCAG antes de producción, especialmente botones secundarios y estados.

## Ejemplo en React

```jsx
import './styles/palette.css';

export function ReservaButton() {
  return <button className="sl-button-primary">Reservar acompañamiento</button>;
}
```

## Ejemplo con variables CSS

```css
.dashboard-sidebar { background: var(--color-primary); color: #fff; }
.profile-card { background: var(--color-surface); border: 1px solid var(--color-border); }
.profile-card__title { color: var(--color-text-main); }
```

> **Nota:** los códigos HEX se han seleccionado para SeniorLink y no deben confundirse con códigos extraídos de forma exacta del logo rasterizado. Para igualar perfectamente un logo final, se pueden calibrar los tonos después.
