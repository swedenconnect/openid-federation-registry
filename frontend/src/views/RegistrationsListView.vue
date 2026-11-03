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
      <v-card>
        <v-card-text>
          <v-data-table
              :headers="entityHeaders"
              :items="filteredEntities"
              :search="search"
              :filter-keys="entityFilterKeys"
              :custom-filter="entityFilterRow"
              v-model:page="entityPage"
              v-model:items-per-page="entityItemsPerPage"
              item-value="registrationId"
              :row-props="entityRowProps"
              hover
              @click:row="(_event, {item}) => openEntity(item)"
          >
            <template #item.entityId="{ item }">
              <span class="mono">{{ item.entityId }}</span>
              <v-chip v-if="item.hosted" size="x-small" color="info" label class="ml-2">Hosted</v-chip>
              <v-chip v-if="item.update" size="x-small" color="warning" label class="ml-2">Update</v-chip>
            </template>
            <template #item.organizationName="{ item }">
              <div>{{ item.organizationName }}</div>
              <div class="text-caption text-grey">{{ item.organizationNumber }}</div>
            </template>
            <template #item.waiting="{ item }">
              <div class="d-flex flex-wrap ga-1">
                <v-chip v-for="chip in waitingChips(item)" :key="chip" size="small" label>{{ chip }}</v-chip>
                <span v-if="!waitingChips(item).length" class="text-grey">—</span>
              </div>
            </template>
            <template #item.reviewState="{ item }">
              <v-chip :color="reviewStateColor(item.reviewState)" size="small" label>
                {{ reviewStateLabel(item.reviewState) }}
              </v-chip>
              <div v-if="item.reviewState === 'IN_REVIEW'" class="text-caption text-grey">
                {{ item.decidedItems }} decided · started by {{ item.startedBy }}
              </div>
            </template>
            <template #item.submittedAt="{ item }">{{ formatDate(item.submittedAt) }}</template>
            <template #item.action="{ item }">
              <v-btn
                  v-if="item.reviewState !== 'COMPLETED'"
                  :id="'btn-review-' + item.registrationId"
                  size="small"
                  :color="item.reviewState === 'IN_REVIEW' ? undefined : 'primary'"
                  :variant="item.reviewState === 'IN_REVIEW' ? 'outlined' : 'flat'"
                  @click.stop="openReview(item)"
              >
                {{ item.reviewState === 'IN_REVIEW' ? 'Continue review' : 'Start review' }}
              </v-btn>
            </template>
            <template #no-data>
              <p v-if="!showHistory" class="text-grey py-8">
                Nothing waits for review. Turn on <strong>Show history</strong> to see reviewed entities.
              </p>
              <p v-else class="text-grey py-8">No entities found.</p>
            </template>
            <template #bottom>
              <TablePager
                  v-model:page="entityPage"
                  v-model:items-per-page="entityItemsPerPage"
                  :total="entityFilteredCount"
              />
            </template>
          </v-data-table>
        </v-card-text>
      </v-card>
    </template>

    <!-- Domain claims: also those no entity is registered under yet -->
    <template v-else>
      <v-card>
        <v-card-text>
          <v-data-table
              :headers="domainHeaders"
              :items="filteredDomains"
              :search="search"
              :filter-keys="domainFilterKeys"
              :custom-filter="domainFilterRow"
              v-model:page="domainPage"
              v-model:items-per-page="domainItemsPerPage"
              item-value="domainId"
              :row-props="domainRowProps"
              hover
              @click:row="(_event, {item}) => openDomain(item)"
          >
            <template #item.organizationText="{ item }">
              <div>{{ item.organizationText }}</div>
              <div v-if="item.legalName || item.orgName" class="text-caption text-grey">{{ item.orgNumber }}</div>
            </template>
            <template #item.status="{ item }">
              <v-chip :color="domainStatusColor(item.status)" size="small" label>
                {{ domainStatusLabel(item.status) }}
              </v-chip>
            </template>
            <template #item.createdDate="{ item }">{{ formatDate(item.createdDate) }}</template>
            <template #item.rejectionReason="{ item }">{{ item.rejectionReason || '' }}</template>
            <template #no-data>
              <p v-if="!showHistory" class="text-grey py-8">
                No pending domain claims. Turn on <strong>Show history</strong> to see decided ones.
              </p>
              <p v-else class="text-grey py-8">No domain claims found.</p>
            </template>
            <template #bottom>
              <TablePager
                  v-model:page="domainPage"
                  v-model:items-per-page="domainItemsPerPage"
                  :total="domainFilteredCount"
              />
            </template>
          </v-data-table>
        </v-card-text>
      </v-card>
    </template>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import TablePager from '@/components/TablePager.vue';
import {useListTable} from '@/composables/listTable';
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

const reviewOrder = (entity) => REVIEW_ORDER[entity.reviewState] ?? 9;

// The order of the lists is what is shown until a column is sorted, and again after its sort is cleared.
const filteredEntities = computed(() => [...entities.value]
    .sort((a, b) => reviewOrder(a) - reviewOrder(b) || dateValue(b.submittedAt) - dateValue(a.submittedAt)));

const filteredDomains = computed(() => domains.value
    .filter(domain => showHistory.value || domain.status === 'PENDING')
    .filter(domain => !statusQuery.value || statusQuery.value === 'ALL' || domain.status === statusQuery.value)
    .map(domain => ({...domain, organizationText: domain.legalName || domain.orgName || domain.orgNumber}))
    .sort((a, b) => (a.status === 'PENDING' ? 0 : 1) - (b.status === 'PENDING' ? 0 : 1)
        || dateValue(b.createdDate) - dateValue(a.createdDate)));

const {
  page: entityPage,
  itemsPerPage: entityItemsPerPage,
  filteredCount: entityFilteredCount,
  filterKeys: entityFilterKeys,
  filterRow: entityFilterRow,
  rowProps: entityRowProps,
} = useListTable(filteredEntities, ['entityId', 'organizationName', 'organizationNumber'], {
  search,
  open: openEntity,
  rowLabel: (entity) => `View ${entity.entityId}`,
});

const {
  page: domainPage,
  itemsPerPage: domainItemsPerPage,
  filteredCount: domainFilteredCount,
  filterKeys: domainFilterKeys,
  filterRow: domainFilterRow,
  rowProps: domainRowProps,
} = useListTable(filteredDomains, ['domain', 'orgNumber', 'legalName', 'orgName'], {
  search,
  open: openDomain,
  rowLabel: (domain) => `View domain request for ${domain.domain}`,
});

const entityHeaders = [
  {title: 'Entity', key: 'entityId', sortable: true},
  {title: 'Organization', key: 'organizationName', sortable: true},
  {title: 'Waiting', key: 'waiting', sortable: false},
  {title: 'Review', key: 'reviewState', sortable: true, sortRaw: (a, b) => reviewOrder(a) - reviewOrder(b)},
  {
    title: 'Submitted', key: 'submittedAt', sortable: true,
    sortRaw: (a, b) => dateValue(a.submittedAt) - dateValue(b.submittedAt),
  },
  {title: '', key: 'action', sortable: false},
];

const domainHeaders = [
  {title: 'Domain', key: 'domain', sortable: true},
  {title: 'Organization', key: 'organizationText', sortable: true},
  {title: 'Status', key: 'status', sortable: true},
  {
    title: 'Requested', key: 'createdDate', sortable: true,
    sortRaw: (a, b) => dateValue(a.createdDate) - dateValue(b.createdDate),
  },
  {title: 'Reason', key: 'rejectionReason', sortable: false},
];

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
.gap-3 {
  gap: 12px;
}
.mono {
  font-family: monospace;
  overflow-wrap: anywhere;
}
</style>
