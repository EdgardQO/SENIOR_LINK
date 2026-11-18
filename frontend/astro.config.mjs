// @ts-check
import { defineConfig } from 'astro/config';

import tailwindcss from '@tailwindcss/vite';

// https://astro.build/config
export default defineConfig({
  vite: {
    plugins: [tailwindcss()],
    server: {
      allowedHosts: ['.trycloudflare.com'],
      proxy: {
        '/api': {
          target: process.env.API_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          // The browser calls Astro on the same origin. This is an internal
          // server-to-server proxy, not a cross-origin browser request.
          // Do not forward the browser Origin to Spring Security's CORS filter.
          configure(proxy) {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.removeHeader('origin');
            });
          }
        }
      }
    }
  }
});