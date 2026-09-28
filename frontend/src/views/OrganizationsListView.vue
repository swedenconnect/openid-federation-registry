<!--
  - Copyright 2026 Sweden Connect
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  -  limitations under the License.
  -->

<template>
  <div>
    <div class="d-flex justify-space-between align-center mb-4">
      <h2>Organizations</h2>
    </div>

    <div class="d-flex align-center mb-4">
      <v-text-field
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          label="Search by name or organization number"
          single-line
          hide-details
          clearable
          variant="outlined"
          density="compact"
          class="flex-grow-1"
      ></v-text-field>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">Loading organizations...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="filteredOrganizations.length > 0">
      <v-table>
        <caption class="sr-only">Organizations on this tenant</caption>
        <thead>
          <tr>
            <th class="text-left">Legal name</th>
            <th class="text-left">Org name</th>
            <th class="text-left">Org number</th>
            <th class="text-left">Domains</th>
            <th class="text-left">Pre-validated trust marks</th>
            <th class="text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="organization in filteredOrganizations" :key="organization.orgNumber">
            <td>{{ organization.legalName || '—' }}</td>
            <td>{{ organization.orgName || '—' }}</td>
            <td>{{ organization.orgNumber }}</td>
            <td>
              <RouterLink :to="domainsLink(organization)" class="domain-link">
                {{ organization.domainCount ?? 0 }}
              </RouterLink>
              <v-chip
                  v-if="(organization.pendingDomainCount ?? 0) > 0"
                  color="warning"
                  size="x-small"
                  label
                  class="ml-2"
              >
                {{ organization.pendingDomainCount }} pending
              </v-chip>
            </td>
            <td>
              <span v-if="!organization.preValidatedTrustMarks?.length" class="text-grey">None</span>
              <v-chip
                  v-for="trustMarkType in organization.preValidatedTrustMarks ?? []"
                  :key="trustMarkType"
                  size="small"
                  label
                  class="mr-1 mb-1"
              >
                {{ trustMarkType }}
              </v-chip>
            </td>
            <td class="text-right">
              <v-btn
                  :id="`btn-edit-trustmarks-${organization.orgNumber}`"
                  color="primary"
                  variant="text"
                  size="small"
                  @click="openTrustMarkDialog(organization)"
              >
                Edit
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No organizations found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Edit trust marks dialog -->
    <v-dialog v-model="trustMarkDialog" max-width="640" aria-labelledby="edit-trustmarks-dialog-title">
      <v-card>
        <v-card-title id="edit-trustmarks-dialog-title" class="text-h5">Pre-validated trust marks</v-card-title>
        <v-card-text>
          <p class="mb-3">
            Trust mark types pre-approved for
            <strong>{{ editTarget?.legalName || editTarget?.orgName || editTarget?.orgNumber }}</strong>.
            A registration requesting one of these enrolls without stopping for manual review.
          </p>
          <v-combobox
              id="input-prevalidated-trustmarks"
              v-model="editTrustMarks"
              :items="trustMarkTypes"
              label="Trust mark types"
              hint="Pick a type issued on this tenant, or type a new one and press enter."
              persistent-hint
              multiple
              chips
              closable-chips
              clearable
              variant="outlined"
          ></v-combobox>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-edit-trustmarks-cancel"
              variant="text"
              color="grey"
              :disabled="saving"
              @click="closeTrustMarkDialog"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-edit-trustmarks-save"
              color="primary"
              :loading="saving"
              :disabled="saving"
              @click="saveTrustMarks"
          >
            Save
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <v-snackbar v-model="snackbar" :timeout="6000" color="primary">
      {{ snackbarMessage }}
    </v-snackbar>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {RouterLink} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {
  registrationAdminOrganizationTrustmarksPath,
  registrationAdminOrganizationsPath,
  registrationAdminTrustmarkTypesPath,
} from '@/config/path';

const errorStore = useErrorStore();
const userStore = useUserStore();

const {requestGet, loading} = useRequest();
// The trust mark picker is a convenience, not a gate: an operator may pre-approve a type the tenant has not
// issued yet, so a failure to load the suggestions leaves the combobox empty rather than raising an error.
const {requestGet: requestTrustMarkTypes, ok: trustMarkTypesOk} = useRequest(false);
const {requestPut: requestSaveTrustMarks, ok: saveOk, loading: saving} = useRequest();

const organizations = ref([]);
const trustMarkTypes = ref([]);
const search = ref('');

const trustMarkDialog = ref(false);
const editTarget = ref(null);
const editTrustMarks = ref([]);

const snackbar = ref(false);
const snackbarMessage = ref('');

const filteredOrganizations = computed(() => {
  const q = (search.value ?? '').toLowerCase();
  return organizations.value.filter(organization => !q
      || organization.orgNumber?.toLowerCase().includes(q)
      || organization.legalName?.toLowerCase().includes(q)
      || organization.orgName?.toLowerCase().includes(q));
});

function domainsLink(organization) {
  return {path: '/registrations', query: {type: 'DOMAIN', org: organization.orgNumber, status: 'ALL', history: '1'}};
}

function showSnackbar(message) {
  snackbarMessage.value = message;
  snackbar.value = true;
}

async function loadOrganizations() {
  errorStore.clearError();
  const response = await requestGet(
      registrationAdminOrganizationsPath(userStore.selectedTenant, userStore.orgNumber));
  organizations.value = Array.isArray(response) ? response : [];
}

async function loadTrustMarkTypes() {
  const response = await requestTrustMarkTypes(
      registrationAdminTrustmarkTypesPath(userStore.selectedTenant, userStore.orgNumber));
  trustMarkTypes.value = trustMarkTypesOk.value && Array.isArray(response) ? response : [];
}

function openTrustMarkDialog(organization) {
  editTarget.value = organization;
  editTrustMarks.value = [...(organization.preValidatedTrustMarks ?? [])];
  trustMarkDialog.value = true;
}

function closeTrustMarkDialog() {
  trustMarkDialog.value = false;
  editTarget.value = null;
  editTrustMarks.value = [];
}

async function saveTrustMarks() {
  errorStore.clearError();
  const target = editTarget.value;
  // The combobox hands back whatever was typed, and the backend rejects a blank or a repeat with a 400 — trim
  // and de-duplicate here so an operator is not shown an error for something the form can settle itself.
  const payload = [...new Set((editTrustMarks.value ?? [])
      .map(trustMarkType => String(trustMarkType).trim())
      .filter(trustMarkType => trustMarkType.length > 0))];

  const updated = await requestSaveTrustMarks(
      registrationAdminOrganizationTrustmarksPath(userStore.selectedTenant, userStore.orgNumber, target.orgNumber),
      {preValidatedTrustMarks: payload});

  if (saveOk.value) {
    const row = organizations.value.find(organization => organization.orgNumber === target.orgNumber);
    if (row) {
      row.preValidatedTrustMarks = updated?.preValidatedTrustMarks ?? payload;
    }
    // A newly invented type becomes a suggestion for the next organization without a round trip.
    trustMarkTypes.value = [...new Set([...trustMarkTypes.value, ...payload])].sort();
    closeTrustMarkDialog();
    showSnackbar(`Pre-validated trust marks updated for ${target.legalName || target.orgNumber}.`);
  }
}

onMounted(async () => {
  await loadOrganizations();
  await loadTrustMarkTypes();
});
</script>

<style scoped>
.domain-link {
  color: inherit;
}
</style>
