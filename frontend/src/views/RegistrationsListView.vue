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
      <h2>Registrations</h2>
      <v-btn
          id="btn-trigger-registration"
          color="primary"
          @click="router.push({ name: 'registration-trigger' })"
      >
        Trigger Registration
      </v-btn>
    </div>

    <div class="d-flex align-center gap-3 mb-4 flex-wrap">
      <v-text-field
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          label="Search by entity ID, intermediate, domain or organization"
          single-line
          hide-details
          clearable
          variant="outlined"
          density="compact"
          class="flex-grow-1"
      ></v-text-field>
      <v-btn-toggle
          v-model="typeFilter"
          mandatory
          color="primary"
          variant="outlined"
          density="compact"
      >
        <v-btn value="ALL">All</v-btn>
        <v-btn value="SUBORDINATE">IM</v-btn>
        <v-btn value="TRUST_MARK_SUBORDINATE">TM</v-btn>
        <v-btn value="DOMAIN">Domain</v-btn>
      </v-btn-toggle>
      <v-switch
          id="switch-show-history"
          v-model="showHistory"
          label="Show history"
          color="primary"
          density="compact"
          hide-details
          class="flex-grow-0"
      ></v-switch>
    </div>

    <v-card v-if="loading || domainsLoading">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">Loading registrations...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="filteredRows.length > 0">
      <v-table>
        <caption class="sr-only">List of registration and domain requests</caption>
        <thead>
          <tr>
            <th class="text-left">Entity ID / Subordinate Entity ID / Domain</th>
            <th class="text-left">Intermediate Entity ID / Trust Mark Type / Organization</th>
            <th class="text-left">Type</th>
            <th class="text-left">Status</th>
            <th class="text-left">Requested</th>
            <th class="text-left">Reason</th>
          </tr>
        </thead>
        <tbody>
          <tr
              v-for="row in filteredRows"
              :key="row.key"
              role="button"
              tabindex="0"
              :aria-label="row.ariaLabel"
              class="clickable-row"
              @click="openDetail(row)"
              @keydown.enter.prevent="openDetail(row)"
              @keydown.space.prevent="openDetail(row)"
          >
            <td>{{ row.primary }}</td>
            <td>
              <div>{{ row.secondary }}</div>
              <div v-if="row.secondaryCaption" class="text-caption text-grey">{{ row.secondaryCaption }}</div>
            </td>
            <td>
              <v-chip :color="typeColor(row.type)" size="small" label>
                {{ typeLabel(row.type) }}
              </v-chip>
            </td>
            <td>
              <v-chip :color="statusColor(row.status)" size="small" label>
                {{ statusLabel(row.status) }}
              </v-chip>
            </td>
            <td>{{ formatDate(row.date) }}</td>
            <td>{{ row.rejectionReason || '' }}</td>
          </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p v-if="!showHistory" class="text-grey">
            No pending requests. History is hidden — turn on <strong>Show history</strong> to see handled requests.
          </p>
          <p v-else class="text-grey">No registrations found.</p>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {registrationAdminDomainsPath, registrationAdminPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, loading} = useRequest();
// The domain list is an operator-only endpoint: a caller without a review queue gets a 404, which is the normal
// answer for everyone else and must not raise an error banner over the registrations they are allowed to see.
const {requestGet: requestDomains, ok: domainsOk, loading: domainsLoading} = useRequest(false);
const errorStore = useErrorStore();
const userStore = useUserStore();

// Registrations and domain requests are two queues of the same kind of work — something an operator has to
// approve or reject — so they are listed together here. DOMAIN is a type alongside IM and TM rather than a
// separate view.
const TYPE_DOMAIN = 'DOMAIN';

// Domain statuses are mapped onto the registration status vocabulary so one set of chips covers both.
const DOMAIN_STATUS_TO_REGISTRATION_STATUS = {
  PENDING: 'PENDING_APPROVAL',
  VALIDATED: 'APPROVED',
  REJECTED: 'REJECTED',
};

const UNHANDLED_STATUSES = ['PENDING_APPROVAL', 'STARTED'];

const SHOW_HISTORY_STORAGE_KEY = 'oidf.registrations.showHistory';

const registrations = ref([]);
const domains = ref([]);

// The Organizations view links here with ?type=DOMAIN&org=<orgNumber>&status=ALL to show one organization's
// domains; the query seeds the same controls the operator would otherwise set by hand.
const search = ref(route.query.org ?? '');
const typeFilter = ref(route.query.type ?? 'ALL');
// A deep link that targets an already handled item passes ?history=1 (or a ?status= other than the pending one)
// so the item it points at is actually in the list when the page opens.
const statusQuery = ref(route.query.status ?? null);
const showHistory = ref(
    route.query.history === '1' || (statusQuery.value !== null && statusQuery.value !== 'PENDING')
        ? true
        : readStoredShowHistory());

const STATUS_ORDER = {PENDING_APPROVAL: 0, STARTED: 1, APPROVED: 2, REJECTED: 3};

function readStoredShowHistory() {
  try {
    return globalThis.localStorage?.getItem(SHOW_HISTORY_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

// Only an actual toggle by the operator is persisted; a ?history=1 deep link does not change the stored default.
watch(showHistory, (value) => {
  try {
    globalThis.localStorage?.setItem(SHOW_HISTORY_STORAGE_KEY, String(value));
  } catch {
    // storage unavailable — the choice simply does not survive the reload
  }
});

const rows = computed(() => {
  const registrationRows = registrations.value.map(registration => {
    const isTrustMark = registration.registrationType === 'TRUST_MARK_SUBORDINATE';
    const primary = isTrustMark ? registration.subordinateEntityId : registration.entityIdentifier;
    return {
      key: `registration-${registration.registrationId}`,
      kind: 'registration',
      id: registration.registrationId,
      type: registration.registrationType,
      primary,
      secondary: isTrustMark ? registration.entityIdentifier : registration.intermediateEntityId,
      secondaryCaption: null,
      status: registration.statusFedreg,
      date: null,
      rejectionReason: registration.rejectionReason,
      ariaLabel: `View registration for ${primary}`,
      searchable: [registration.entityIdentifier, registration.intermediateEntityId,
        registration.subordinateEntityId, registration.organizationName],
    };
  });

  const domainRows = domains.value.map(domain => ({
    key: `domain-${domain.domainId}`,
    kind: 'domain',
    id: domain.domainId,
    type: TYPE_DOMAIN,
    primary: domain.domain,
    secondary: domain.legalName || domain.orgName || domain.orgNumber,
    secondaryCaption: (domain.legalName || domain.orgName) ? domain.orgNumber : null,
    status: DOMAIN_STATUS_TO_REGISTRATION_STATUS[domain.status] ?? domain.status,
    domainStatus: domain.status,
    date: domain.createdDate,
    rejectionReason: domain.rejectionReason,
    ariaLabel: `View domain request for ${domain.domain}`,
    searchable: [domain.domain, domain.orgNumber, domain.legalName, domain.orgName],
  }));

  return [...registrationRows, ...domainRows];
});

const filteredRows = computed(() => {
  const q = (search.value ?? '').toLowerCase();
  return rows.value
      .filter(row => !q || row.searchable.some(value => value?.toLowerCase().includes(q)))
      .filter(row => typeFilter.value === 'ALL' || row.type === typeFilter.value)
      .filter(row => showHistory.value || UNHANDLED_STATUSES.includes(row.status))
      .filter(row => !statusQuery.value
          || statusQuery.value === 'ALL'
          || row.kind !== 'domain'
          || row.domainStatus === statusQuery.value)
      .sort((a, b) => {
        const byStatus = (STATUS_ORDER[a.status] ?? 99) - (STATUS_ORDER[b.status] ?? 99);
        if (byStatus !== 0) return byStatus;
        return dateValue(b.date) - dateValue(a.date);
      });
});

function dateValue(value) {
  if (!value) return 0;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? 0 : date.getTime();
}

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function statusColor(status) {
  const colors = {PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'error', STARTED: 'info'};
  return colors[status] ?? 'default';
}

function statusLabel(status) {
  const labels = {PENDING_APPROVAL: 'Pending Approval', APPROVED: 'Approved', REJECTED: 'Rejected', STARTED: 'Started'};
  return labels[status] ?? status;
}

function typeColor(type) {
  if (type === TYPE_DOMAIN) return 'teal';
  return type === 'TRUST_MARK_SUBORDINATE' ? 'secondary' : 'primary';
}

function typeLabel(type) {
  if (type === TYPE_DOMAIN) return 'Domain';
  return type === 'TRUST_MARK_SUBORDINATE' ? 'TM' : 'IM';
}

function openDetail(row) {
  if (row.kind === 'domain') {
    router.push({name: 'domain-request-detail', params: {domainId: row.id}});
    return;
  }
  router.push({name: 'registration-detail', params: {id: row.id}});
}

async function loadRegistrations() {
  const response = await requestGet(registrationAdminPath(userStore.selectedTenant, userStore.orgNumber));
  registrations.value = Array.isArray(response) ? response : [];
}

async function loadDomains() {
  const response = await requestDomains(
      registrationAdminDomainsPath(userStore.selectedTenant, userStore.orgNumber));
  domains.value = domainsOk.value && Array.isArray(response) ? response : [];
}

onMounted(() => {
  errorStore.clearError();
  Promise.all([loadRegistrations(), loadDomains()]);
});
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}
.clickable-row:hover td {
  background-color: rgba(0, 0, 0, 0.04);
}
.gap-3 {
  gap: 12px;
}
</style>
