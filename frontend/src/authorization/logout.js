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

import {csrfToken} from '@/api/composables/fetch';
import {logoutPath} from '@/config/path';

/**
 * Logs out by posting a form to the backend, with the CSRF token as a parameter. A form is used instead of fetch,
 * since the backend answers with a redirect to the identity provider's logout page.
 */
export function logout() {
    const form = document.createElement('form');
    form.method = 'POST';
    form.action = logoutPath;
    const token = csrfToken();
    if (token) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = '_csrf';
        input.value = token;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
}
