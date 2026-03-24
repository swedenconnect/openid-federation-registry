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

import {defineStore} from 'pinia';
import {computed, ref} from 'vue';
import {useRequest} from '@/api/composables/request';
import {useAuthorizationStatusStore} from '@/authorization/stores/authorizationStatusStore';
import {userInfoPath} from '@/config/path';

export const useUserStore = defineStore('userStore', () => {
    const {requestGet, requestPut, ok} = useRequest(false);
    const authorizationStatusStore = useAuthorizationStatusStore();

    const userName = ref('');
    const givenName = ref('');
    const familyName = ref('');
    const fullName = ref('');
    const orgNumber = ref('');
    const orgName = ref('');
    const entityPrefix = ref('');
    const organizations = ref([]);

    const isAuthorized = computed(() => authorizationStatusStore.isAuthorized === true);

    async function fetchUser() {
        const response = await requestGet(userInfoPath);
        if (ok.value && response) {
            userName.value = response.userName || '';
            givenName.value = response.givenName || '';
            familyName.value = response.familyName || '';
            fullName.value = response.fullName || '';
            orgNumber.value = response.orgNumber || '';
            orgName.value = response.orgName || '';
            entityPrefix.value = response.entityPrefix || '';
            organizations.value = response.orgInfo?.organizations || [];
            authorizationStatusStore.isAuthorized = true;

            // If only one organization, auto-select it
            if (!orgNumber.value && organizations.value.length === 1) {
                await selectOrganization(organizations.value[0].orgNumber);
            }
        } else {
            authorizationStatusStore.isAuthorized = false;
        }
    }

    async function selectOrganization(newOrgNumber) {
        await requestPut(`${userInfoPath}?orgNumber=${encodeURIComponent(newOrgNumber)}`);
        if (ok.value) {
            globalThis.location.reload();
        }
    }

    return {
        userName,
        givenName,
        familyName,
        fullName,
        orgNumber,
        orgName,
        entityPrefix,
        organizations,
        isAuthorized,
        fetchUser,
        selectOrganization,
    };
});
