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
          :label="mode === MODE_DOMAINS ? 'Search by domain or organization' : 'Search by entity or organization'"
          single-line
          hide-details
          clearable
          variant="outlined"
          density="compact"
          class="flex-grow-1"
      ></v-text-field>
      <v-btn-toggle
          v-model="mode"
          mandatory
          color="primary"
          variant="outlined"
          density="compact"
          aria-label="What to list"
      >
        <v-btn :value="MODE_ENTITIES">Entities</v-btn>
        <v-btn :value="MODE_DOMAINS">Domain claims</v-btn>
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

    <!-- Entities: everything an entity waits for, reviewed in one session -->
    <template v-else-if="mode === MODE_ENTITIES">
      <v-card v-if="filteredEntities.length > 0">
        <v-table>
          <caption class="sr-only">Entities and what each waits for</caption>
          <thead>
            <tr>
              <th class="text-left">Entity</th>
              <th class="text-left">Organization</th>
              <th class="text-left">Waiting</th>
              <th class="text-left">Review</th>
              <th class="text-left">Submitted</th>
              <th class="text-left"><span class="sr-only">Action</span></th>
            </tr>
          </thead>
          <tbody>
            <tr
                v-for="entity in filteredEntities"
                :key="entity.registrationId"
                role="button"
                tabindex="0"
                :aria-label="`View ${entity.entityId}`"
                class="clickable-row"
                @click="openEntity(entity)"
                @keydown.enter.prevent="openEntity(entity)"
                @keydown.space.prevent="openEntity(entity)"
            >
              <td>
                <span class="mono">{{ entity.entityId }}</span>
                <v-chip v-if="entity.hosted" size="x-small" color="info" label class="ml-2">Hosted</v-chip>
              </td>
              <td>
                <div>{{ entity.organizationName }}</div>
                <div class="text-caption text-grey">{{ entity.organizationNumber }}</div>
              </td>
              <td>
                <div class="d-flex flex-wrap ga-1">
                  <v-chip v-for="chip in waitingChips(entity)" :key="chip" size="small" label>{{ chip }}</v-chip>
                  <span v-if="!waitingChips(entity).length" class="text-grey">—</span>
                </div>
              </td>
              <td>
                <v-chip :color="reviewStateColor(entity.reviewState)" size="small" label>
                  {{ reviewStateLabel(entity.reviewState) }}
                </v-chip>
                <div v-if="entity.reviewState === 'IN_REVIEW'" class="text-caption text-grey">
                  {{ entity.decidedItems }} decided · started by {{ entity.startedBy }}
                </div>
              </td>
              <td>{{ formatDate(entity.submittedAt) }}</td>
              <td>
                <v-btn
                    v-if="entity.reviewState !== 'COMPLETED'"
                    :id="'btn-review-' + entity.registrationId"
                    size="small"
                    :color="entity.reviewState === 'IN_REVIEW' ? undefined : 'primary'"
                    :variant="entity.reviewState === 'IN_REVIEW' ? 'outlined' : 'flat'"
                    @click.stop="openReview(entity)"
                >
                  {{ entity.reviewState === 'IN_REVIEW' ? 'Continue review' : 'Start review' }}
                </v-btn>
              </td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
      <v-card v-else>
        <v-card-text>
          <div class="text-center py-12">
            <p v-if="!showHistory" class="text-grey">
              Nothing waits for review. Turn on <strong>Show history</strong> to see reviewed entities.
            </p>
            <p v-else class="text-grey">No entities found.</p>
          </div>
        </v-card-text>
      </v-card>
    </template>

    <!-- Domain claims: also those no entity is registered under yet -->
    <template v-else>
      <v-card v-if="filteredDomains.length > 0">
        <v-table>
          <caption class="sr-only">Domain claims</caption>
          <thead>
            <tr>
              <th class="text-left">Domain</th>
              <th class="text-left">Organization</th>
              <th class="text-left">Status</th>
              <th class="text-left">Requested</th>
              <th class="text-left">Reason</th>
            </tr>
          </thead>
          <tbody>
            <tr
                v-for="domain in filteredDomains"
                :key="domain.domainId"
                role="button"
                tabindex="0"
                :aria-label="`View domain request for ${domain.domain}`"
                class="clickable-row"
                @click="openDomain(domain)"
                @keydown.enter.prevent="openDomain(domain)"
                @keydown.space.prevent="openDomain(domain)"
            >
              <td>{{ domain.domain }}</td>
              <td>
                <div>{{ domain.legalName || domain.orgName || domain.orgNumber }}</div>
                <div v-if="domain.legalName || domain.orgName" class="text-caption text-grey">{{ domain.orgNumber }}</div>
              </td>
              <td>
                <v-chip :color="domainStatusColor(domain.status)" size="small" label>
                  {{ domainStatusLabel(domain.status) }}
                </v-chip>
              </td>
              <td>{{ formatDate(domain.createdDate) }}</td>
              <td>{{ domain.rejectionReason || '' }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card>
      <v-card v-else>
        <v-card-text>
          <div class="text-center py-12">
            <p v-if="!showHistory" class="text-grey">
              No pending domain claims. Turn on <strong>Show history</strong> to see decided ones.
            </p>
            <p v-else class="text-grey">No domain claims found.</p>
          </div>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {entityReviewsPath, registrationAdminDomainsPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, loading} = useRequest();
// The domain list is an operator-only endpoint: a caller without a review queue gets a 404, which is the normal
// answer for everyone else and must not raise an error banner.
const {requestGet: requestDomains, ok: domainsOk, loading: domainsLoading} = useRequest(false);
const errorStore = useErrorStore();
const userStore = useUserStore();

const MODE_ENTITIES = 'ENTITIES';
const MODE_DOMAINS = 'DOMAINS';
const SHOW_HISTORY_STORAGE_KEY = 'oidf.registrations.showHistory';

const entities = ref([]);
const domains = ref([]);

// The Organizations view links here with ?type=DOMAIN&org=<orgNumber>&status=ALL to show one organization's
// domains; the query seeds the same controls the operator would otherwise set by hand.
const search = ref(route.query.org ?? '');
const mode = ref(route.query.type === 'DOMAIN' ? MODE_DOMAINS : MODE_ENTITIES);
const statusQuery = ref(route.query.status ?? null);
const showHistory = ref(
    route.query.history === '1' || (statusQuery.value !== null && statusQuery.value !== 'PENDING')
        ? true
        : readStoredShowHistory());

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
  loadEntities();
});

const REVIEW_ORDER = {IN_REVIEW: 0, NOT_STARTED: 1, COMPLETED: 2};

function matches(values) {
  const q = (search.value ?? '').toLowerCase();
  return !q || values.some(value => value?.toLowerCase().includes(q));
}

const filteredEntities = computed(() => entities.value
    .filter(entity => matches([entity.entityId, entity.organizationName, entity.organizationNumber]))
    .sort((a, b) => (REVIEW_ORDER[a.reviewState] ?? 9) - (REVIEW_ORDER[b.reviewState] ?? 9)
        || dateValue(b.submittedAt) - dateValue(a.submittedAt)));

const filteredDomains = computed(() => domains.value
    .filter(domain => matches([domain.domain, domain.orgNumber, domain.legalName, domain.orgName]))
    .filter(domain => showHistory.value || domain.status === 'PENDING')
    .filter(domain => !statusQuery.value || statusQuery.value === 'ALL' || domain.status === statusQuery.value)
    .sort((a, b) => (a.status === 'PENDING' ? 0 : 1) - (b.status === 'PENDING' ? 0 : 1)
        || dateValue(b.createdDate) - dateValue(a.createdDate)));

function waitingChips(entity) {
  const chips = [];
  const add = (count, one, many) => {
    if (count > 0) chips.push(`${count} ${count === 1 ? one : many}`);
  };
  add(entity.pendingDomains, 'domain', 'domains');
  add(entity.pendingTrustMarks, 'trust mark', 'trust marks');
  add(entity.pendingEntitySteps, 'entity step', 'entity steps');
  return chips;
}

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

function reviewStateColor(state) {
  return {NOT_STARTED: 'warning', IN_REVIEW: 'info', COMPLETED: 'success'}[state] ?? 'default';
}

function reviewStateLabel(state) {
  return {NOT_STARTED: 'Not started', IN_REVIEW: 'In review', COMPLETED: 'Completed'}[state] ?? state;
}

function domainStatusColor(status) {
  return {PENDING: 'warning', VALIDATED: 'success', REJECTED: 'error'}[status] ?? 'default';
}

function domainStatusLabel(status) {
  return {PENDING: 'Pending', VALIDATED: 'Validated', REJECTED: 'Rejected'}[status] ?? status;
}

function openEntity(entity) {
  router.push({name: 'entity-review-detail', params: {registrationId: entity.registrationId}});
}

function openReview(entity) {
  router.push({name: 'entity-review-session', params: {registrationId: entity.registrationId}});
}

function openDomain(domain) {
  router.push({name: 'domain-request-detail', params: {domainId: domain.domainId}});
}

async function loadEntities() {
  const response = await requestGet(
      entityReviewsPath(userStore.selectedTenant, userStore.orgNumber, showHistory.value));
  entities.value = Array.isArray(response) ? response : [];
}

async function loadDomains() {
  const response = await requestDomains(
      registrationAdminDomainsPath(userStore.selectedTenant, userStore.orgNumber));
  domains.value = domainsOk.value && Array.isArray(response) ? response : [];
}

onMounted(() => {
  errorStore.clearError();
  Promise.all([loadEntities(), loadDomains()]);
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
.mono {
  font-family: monospace;
  overflow-wrap: anywhere;
}
</style>
