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
import {useErrorStore} from "@/stores/errorStore";
import {useAuthorizationStatusStore} from "@/authorization/stores/authorizationStatusStore";
import router from "@/router";

export function useLoadJwks() {
    const errorStore = useErrorStore();
    const authorizationStatusStore = useAuthorizationStatusStore();
    const loading = ref(false);

    async function loadJwks(entityIdentifier) {
        if (!entityIdentifier || !entityIdentifier.trim()) {
            return null;
        }

        loading.value = true;
        errorStore.clearError();

        try {
            // Make POST request with plain text body
            const response = await fetch('/admin/support/jwks', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'text/plain',
                },
                body: entityIdentifier.trim(),
            });

            if (response.ok) {
                const jwksData = await response.json();
                return jwksData;
            } else {
                // Handle error response
                let errorMessage = 'Failed to load JWKS';
                try {
                    const errorData = await response.json();
                    if (errorData.detail) {
                        errorMessage = errorData.detail;
                    } else if (errorData.message) {
                        errorMessage = errorData.message;
                    }
                } catch (e) {
                    // If response is not JSON, use status text
                    errorMessage = response.statusText || 'Failed to load JWKS';
                }

                if (response.status === 401) {
                    authorizationStatusStore.isAuthorized = false;
                    router.push({name: 'login'});
                    errorMessage = 'Unauthorized. Please login.';
                }

                errorStore.setError(errorMessage);
                return null;
            }
        } catch (error) {
            console.error('Error loading JWKS:', error);
            if (error.message === 'Failed to fetch') {
                authorizationStatusStore.isAuthorized = false;
                router.push({name: 'login'});
                errorStore.setError('Failed to connect to server. Please check your connection.');
            } else {
                errorStore.setError('Failed to load JWKS. Please check your connection and try again.');
            }
            return null;
        } finally {
            loading.value = false;
        }
    }

    return {
        loadJwks,
        loading,
    };
}
