# SeniorLink — Paleta CSS y documentación

Este ZIP contiene los archivos de color para la interfaz de **SeniorLink**, una plataforma de acompañamiento no clínico para personas adultas mayores.

## Archivos

- `02_paleta_css/palette.css`: variables CSS listas para utilizar, estados, ejemplos de botones, tarjetas, avisos y foco accesible.
- `02_paleta_css/palette.md`: catálogo de colores (HEX), usos sugeridos, reglas de accesibilidad e integración en React.

## Instalación rápida (React)

1. Copia `palette.css` a `src/styles/palette.css` de tu proyecto.
2. Importa el archivo desde `src/main.jsx` o desde tu punto de entrada: `import './styles/palette.css';` (ajusta la ruta según tu estructura).
3. Utiliza `var(--color-primary)`, `var(--color-surface)`, etc. desde tus componentes.
4. Ajusta clases existentes si el proyecto utiliza Tailwind u otra biblioteca.

## Consideraciones

- La paleta está pensada para interfaces claras en el portal público y en el panel administrativo.
- No contiene fotografías, logos oficiales universitarios ni vídeos.
- Revisa contraste y accesibilidad de cada componente antes de publicar.
