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
import {tenantsPath, userInfoPath} from '@/config/path';

// The backend no longer remembers the selected tenant/organization (the PUT to
// /userinfo that used to persist it server-side has been removed) and /userinfo no
// longer resolves a default organization at all: orgNumber, orgName and entityPrefix
// are sourced exclusively from /tenants now. The selection therefore lives purely in
// the frontend, kept here so it survives the page reload triggered on every switch.
const SELECTED_TENANT_STORAGE_KEY = 'oidf.selectedTenant';
const SELECTED_ORG_NUMBER_STORAGE_KEY = 'oidf.selectedOrgNumber';

export const useUserStore = defineStore('userStore', () => {
    const {requestGet, ok} = useRequest(false);
    const authorizationStatusStore = useAuthorizationStatusStore();

    const userName = ref('');
    const givenName = ref('');
    const familyName = ref('');
    const fullName = ref('');
    const orgNumber = ref('');
    const orgName = ref('');
    const entityPrefix = ref('');
    const tenants = ref([]);
    const selectedTenant = ref('');

    const isAuthorized = computed(() => authorizationStatusStore.isAuthorized === true);

    const organizations = computed(() =>
        tenants.value.find((tenant) => tenant.tenant === selectedTenant.value)?.organizations || []);

    async function fetchUser() {
        const response = await requestGet(userInfoPath);
        if (ok.value && response) {
            userName.value = response.userName || '';
            givenName.value = response.givenName || '';
            familyName.value = response.familyName || '';
            fullName.value = response.fullName || '';
            authorizationStatusStore.isAuthorized = true;
        } else {
            authorizationStatusStore.isAuthorized = false;
        }
    }

    async function fetchTenants() {
        const response = await requestGet(tenantsPath);
        if (ok.value && response) {
            tenants.value = response.tenants || [];
            if (tenants.value.length > 0) {
                applySelection(resolveInitialSelection());
            }
        }
    }

    // Resolves which tenant/organization to activate on load: a previously stored
    // selection wins as long as it still refers to a tenant/org the user currently
    // has access to, otherwise fall back to the first tenant/organization returned
    // by /tenants (there is no backend-resolved default anymore).
    function resolveInitialSelection() {
        const storedTenant = globalThis.localStorage?.getItem(SELECTED_TENANT_STORAGE_KEY);
        const storedOrgNumber = globalThis.localStorage?.getItem(SELECTED_ORG_NUMBER_STORAGE_KEY);
        const storedTenantEntry = storedTenant
            ? tenants.value.find((tenant) => tenant.tenant === storedTenant)
            : null;
        if (storedTenantEntry?.organizations?.some((org) => org.orgNumber === storedOrgNumber)) {
            return {tenant: storedTenant, orgNumber: storedOrgNumber};
        }

        const tenant = tenants.value[0];
        return {tenant: tenant.tenant, orgNumber: tenant.organizations?.[0]?.orgNumber || ''};
    }

    // Applies a tenant/organization pair to the store's state and persists it
    // client-side so it survives the reload triggered by selectTenant/selectOrganization.
    // orgName/entityPrefix are looked up from the matching /tenants organization entry —
    // the only place that data comes from now.
    function applySelection({tenant, orgNumber: newOrgNumber}) {
        selectedTenant.value = tenant;
        orgNumber.value = newOrgNumber || '';

        const org = organizations.value.find((o) => o.orgNumber === orgNumber.value);
        orgName.value = org?.orgName || '';
        entityPrefix.value = org?.entityPrefix || '';

        if (globalThis.localStorage) {
            globalThis.localStorage.setItem(SELECTED_TENANT_STORAGE_KEY, selectedTenant.value);
            globalThis.localStorage.setItem(SELECTED_ORG_NUMBER_STORAGE_KEY, orgNumber.value);
        }
    }

    function selectTenant(tenantName) {
        // Keep the selected organization consistent with the selected tenant: if the
        // currently selected organization does not belong to this tenant, fall back to
        // the tenant's first organization.
        const tenantOrganizations = tenants.value.find((tenant) => tenant.tenant === tenantName)?.organizations || [];
        const newOrgNumber = tenantOrganizations.some((org) => org.orgNumber === orgNumber.value)
            ? orgNumber.value
            : (tenantOrganizations[0]?.orgNumber || '');

        applySelection({tenant: tenantName, orgNumber: newOrgNumber});
        globalThis.location.reload();
    }

    function selectOrganization(newOrgNumber) {
        applySelection({tenant: selectedTenant.value, orgNumber: newOrgNumber});
        globalThis.location.reload();
    }

    return {
        userName,
        givenName,
        familyName,
        fullName,
        orgNumber,
        orgName,
        entityPrefix,
        tenants,
        selectedTenant,
        organizations,
        isAuthorized,
        fetchUser,
        fetchTenants,
        selectTenant,
        selectOrganization,
    };
});
