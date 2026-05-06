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

import {ref} from "vue";
import {useFetch} from "./fetch";
import {useErrorStore} from "@/stores/errorStore";

function _errorMap(error) {
    if (error.includes("Value is not a valid JSON.")) {
        return 'Invalid JSON format.';
    } else if (error.includes("invalid")) {
        return 'Invalid request.';
    }
    return 'An unexpected error occurred.';
}

export function useRequest(logError = true) {
    const {fetchData, json, ok, status, correlationId} = useFetch();
    const errorStore = useErrorStore();

    const error = ref(null);
    const loading = ref(false);

    const errorInterpreter = ref(_errorMap);

    async function _request(path, payload, method) {
        loading.value = true;
        error.value = null;
        errorStore.clearError();
        try {
            return await fetchData(path, payload, method);
        } catch (err) {
            console.log(err);
            let errorMessage = 'Unknown error';
            if (err instanceof Error) {
                errorMessage = errorInterpreter.value(err.message);
                // Also set in error store for global display
                if (status.value === 401) {
                    console.info("401 redirects to login — no error banner");
                    return;
                } else if (err.message.includes('Failed to fetch')) {
                    errorStore.setError('Failed to connect to server. Please check your connection.');
                } else {
                    // Try to extract error message from response
                    if (json.value && json.value.detail) {
                        errorStore.setError(json.value.detail);
                    } else if (json.value && json.value.message) {
                        errorStore.setError(json.value.message);
                    } else {
                        errorStore.setError(errorMessage);
                    }
                }
            }
            error.value = errorMessage;
            if (logError) {
                console.error(`[CorrelationId: ${correlationId.value ?? '-'}]`, err);
            }
        } finally {
            loading.value = false;
        }
        return null;
    }

    async function requestDelete(path) {
        return await _request(path, null, 'DELETE');
    }

    async function requestGet(path) {
        return await _request(path, null, 'GET');
    }

    async function requestPatch(path, payload = null) {
        return await _request(path, payload, 'PATCH');
    }

    async function requestPost(path, payload = null) {
        return await _request(path, payload, 'POST');
    }

    async function requestPut(path, payload = null) {
        const result = await _request(path, payload, 'PUT');
        // PUT might return the updated resource, so check if we got data back
        if (ok.value && json.value) {
            return json.value;
        }
        return result;
    }

    return {
        error,
        errorInterpreter,
        json,
        loading,
        ok,
        status,
        requestDelete,
        requestGet,
        requestPatch,
        requestPost,
        requestPut,
    }
}

