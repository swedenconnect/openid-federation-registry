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
          id="organization-search"
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

    <v-card v-else>
      <v-card-text>
        <v-data-table
            :headers="headers"
            :items="organizations"
            :search="search"
            :filter-keys="filterKeys"
            :custom-filter="filterRow"
            v-model:page="page"
            v-model:items-per-page="itemsPerPage"
            item-value="orgNumber"
        >
          <template #item.legalName="{ item }">{{ item.legalName || '—' }}</template>
          <template #item.orgName="{ item }">{{ item.orgName || '—' }}</template>
          <template #item.domainCount="{ item }">
            <RouterLink :to="domainsLink(item)" class="domain-link">
              {{ item.domainCount ?? 0 }}
            </RouterLink>
            <v-chip
                v-if="(item.pendingDomainCount ?? 0) > 0"
                color="warning"
                size="x-small"
                label
                class="ml-2"
            >
              {{ item.pendingDomainCount }} pending
            </v-chip>
          </template>
          <template #item.preValidatedTrustMarks="{ item }">
            <span v-if="!item.preValidatedTrustMarks?.length" class="text-grey">None</span>
            <v-chip
                v-for="trustMarkType in item.preValidatedTrustMarks ?? []"
                :key="trustMarkType"
                size="small"
                label
                class="mr-1 mb-1"
            >
              {{ trustMarkType }}
            </v-chip>
          </template>
          <template #item.actions="{ item }">
            <v-btn
                :id="`btn-edit-trustmarks-${item.orgNumber}`"
                color="primary"
                variant="text"
                size="small"
                @click="openTrustMarkDialog(item)"
            >
              Edit
            </v-btn>
          </template>
          <template #no-data>
            <EmptyState what="Organizations" :search="search"/>
          </template>
          <template #bottom>
            <TablePager v-model:page="page" v-model:items-per-page="itemsPerPage" :total="filteredCount"/>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>

    <!-- Edit trust marks dialog -->
    <v-dialog v-model="trustMarkDialog" max-width="640" aria-labelledby="edit-trustmarks-dialog-title">
      <v-card>
        <v-card-title id="edit-trustmarks-dialog-title" class="text-h5">Pre-validated Trust Marks</v-card-title>
        <v-card-text>
          <p class="mb-3">
            Trust Mark types pre-approved for
            <strong>{{ editTarget?.legalName || editTarget?.orgName || editTarget?.orgNumber }}</strong>.
            A registration requesting one of these enrolls without stopping for manual review.
          </p>
          <v-combobox
              id="input-prevalidated-trustmarks"
              v-model="editTrustMarks"
              :items="trustMarkTypes"
              label="Trust Mark types"
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

  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {RouterLink} from 'vue-router';
import EmptyState from '@/components/EmptyState.vue';
import TablePager from '@/components/TablePager.vue';
import {useListTable} from '@/composables/listTable';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useNoticeStore} from '@/stores/noticeStore';
import {useUserStore} from '@/stores/userStore';
import {
  registrationAdminOrganizationTrustmarksPath,
  registrationAdminOrganizationsPath,
  registrationAdminTrustmarkTypesPath,
} from '@/config/path';

const errorStore = useErrorStore();
const noticeStore = useNoticeStore();
const userStore = useUserStore();

const {requestGet, loading} = useRequest();
// The trust mark picker is a convenience, not a gate: an operator may pre-approve a type the tenant has not
// issued yet, so a failure to load the suggestions leaves the combobox empty rather than raising an error.
const {requestGet: requestTrustMarkTypes, ok: trustMarkTypesOk} = useRequest(false);
const {requestPut: requestSaveTrustMarks, ok: saveOk, loading: saving} = useRequest();

const organizations = ref([]);
const trustMarkTypes = ref([]);
const {search, page, itemsPerPage, filteredCount, filterKeys, filterRow} =
    useListTable(organizations, ['legalName', 'orgName', 'orgNumber']);

const headers = [
  {title: 'Legal name', key: 'legalName', sortable: true},
  {title: 'Organization name', key: 'orgName', sortable: true},
  {title: 'Organization number', key: 'orgNumber', sortable: true},
  {title: 'Domains', key: 'domainCount', sortable: true},
  {title: 'Pre-validated Trust Marks', key: 'preValidatedTrustMarks', sortable: false},
  {title: 'Actions', key: 'actions', sortable: false, align: 'end'},
];

const trustMarkDialog = ref(false);
const editTarget = ref(null);
const editTrustMarks = ref([]);

function domainsLink(organization) {
  return {path: '/registrations', query: {type: 'DOMAIN', org: organization.orgNumber, status: 'ALL', history: '1'}};
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
    noticeStore.show(`Pre-validated Trust Marks updated for ${target.legalName || target.orgNumber}.`);
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
