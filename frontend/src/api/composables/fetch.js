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

import {useAuthorizationStatusStore} from "@/authorization/stores/authorizationStatusStore";
import router from "@/router";
import {ref} from "vue";

function _getRequestOptions(payload, method) {
    const headers = {};
    headers['Content-Type'] = 'application/json';
    return {
        method,
        credentials: 'include',
        headers,
        body: payload ? JSON.stringify(payload) : payload,
        redirect: 'error',
    };
}

export function useFetch() {
    const _authorizationStatusStore = useAuthorizationStatusStore();

    const status = ref(null);
    const json = ref(null);
    const ok = ref(null);

    async function fetchData(path, payload = null, method = 'GET') {
        status.value = null;
        json.value = null;
        ok.value = null;

        let response;
        try {
            response = await fetch(path, _getRequestOptions(payload, method));
        } catch (err) {
            if (err instanceof Error) {
                if (err.message === 'Failed to fetch') {
                    _authorizationStatusStore.isAuthorized = false;
                    router.push({name: 'login'});
                }
                throw new Error(err.message);
            }
            throw new Error('unknown');
        }

        status.value = response.status;
        ok.value = response.ok;

        if (!ok.value) {
            if (status.value === 401) {
                _authorizationStatusStore.isAuthorized = false;
                router.push({name: 'login'});
                throw new Error('unauthorized');
            }
            const errData = await response.json();
            json.value = errData;
            if (status.value === 400) {
                throw new Error('invalid');
            } else if (errData.detail) {
                throw new Error(errData.detail);
            }
            throw new Error('unknown');
        }

        // Return data or null depending on HTTPMethod
        if (method === 'DELETE' || method === 'PUT') {
            return null;
        }
        const data = await response.json();
        json.value = data;
        return data;
    }

    return {
        fetchData,
        json,
        ok,
        status,
    }
}

