/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import {fileURLToPath, URL} from 'node:url';
import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
// Imports only the Vuetify components the templates use, instead of all of them.
import vuetify from 'vite-plugin-vuetify';

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue(), vuetify({autoImport: true})],
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8020',
                changeOrigin: true,
            },
            '/admin': {
                target: 'http://localhost:8020',
                changeOrigin: true,
            },
            '/registry': {
                target: 'http://localhost:8020',
                changeOrigin: true,
            },
            '/authenticate': {
                target: 'http://localhost:8020',
                changeOrigin: true,
            },
        },
    },
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    base: './',
    build: {
        // Vuetify and the remaining dependencies get chunks of their own, so that no chunk grows past the size
        // vite warns about and a release only invalidates the chunks that changed.
        rollupOptions: {
            output: {
                manualChunks(id) {
                    if (id.includes('node_modules/vuetify')) {
                        return 'vuetify';
                    }
                    if (id.includes('node_modules')) {
                        return 'vendor';
                    }
                },
            },
        },
    },
    // jsdom because the modules under test read the document at load time, e.g. the base href the paths are built
    // from and the XSRF-TOKEN cookie.
    test: {
        environment: 'jsdom',
        // An origin of its own: localStorage, which the stores and Pinia's devtools support use, is not available
        // on the opaque origin of the default about:blank.
        environmentOptions: {jsdom: {url: 'http://localhost/'}},
        // Node brings a localStorage of its own, which takes precedence over the one of the jsdom environment and
        // throws unless node is started with --localstorage-file. The test processes run without it.
        pool: 'forks',
        execArgv: ['--no-experimental-webstorage'],
        include: ['src/**/*.spec.js'],
    }
});

