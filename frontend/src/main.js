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

import '@mdi/font/css/materialdesignicons.css'
import './assets/main.css';
import {defaultConfig, plugin} from '@formkit/vue';
import {createPinia} from 'pinia';
import {createApp} from 'vue';
import vuetify from './plugins/vuetify';

import App from './App.vue';
import router from './router';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(vuetify);
app.use(plugin, defaultConfig({
    // FormKit global messages
    messages: {
        en: {
            validation: {
                json: "Invalid JSON.",
                required: "This field is required.",
            },
        },
    },
    // FormKit global JSON validation
    rules: {
        json: (node) => {
            const value = node.value;
            if (!value) return true;
            try {
                JSON.parse(value);
                return true;
            } catch (e) {
                return false;
            }
        },
    },
}));

app.mount('#app');

