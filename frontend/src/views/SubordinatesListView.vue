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
      <h2>Subordinates</h2>
      <div>
        <v-btn
            id="btn-add-subordinate"
            color="primary"
            @click="addSubordinate"
            class="mr-2"
        >
          Add Subordinate
        </v-btn>
        <v-btn
            id="btn-back"
            color="grey"
            @click="goBack"
        >
          Back
        </v-btn>
      </div>
    </div>

    <v-card v-if="entityIdentifier" variant="tonal" color="primary" class="mb-4">
      <v-card-text class="d-flex align-center py-2">
        <v-icon aria-hidden="true" class="mr-2">{{
            moduleType === 'trustanchor' ? 'mdi-shield-check' : 'mdi-transit-connection-variant'
          }}
        </v-icon>
        <span class="text-caption text-uppercase font-weight-medium mr-3">{{
            moduleType === 'trustanchor' ? 'Trust Anchor' : 'Intermediate'
          }}</span>
        <span class="text-body-2">{{ entityIdentifier }}</span>
      </v-card-text>
    </v-card>

    <div class="d-flex align-center mb-4">
      <v-text-field
          id="subordinate-search"
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          label="Search by entity identifier"
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
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
              aria-hidden="true"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading subordinates...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <v-data-table
            :headers="headers"
            :items="tableItems"
            :search="search"
            :filter-keys="['entityIdentifier']"
            v-model:page="page"
            v-model:items-per-page="itemsPerPage"
            item-value="subordinateId"
            hover
            @click:row="openSubordinate"
        >
          <template #item.statusText="{ item }">
            <v-tooltip v-if="hasEcLocation(item)" text="EC Location configured" location="top">
              <template v-slot:activator="{ props }">
                <span v-bind="props" tabindex="0" role="img" aria-label="EC Location configured">
                  <v-icon aria-hidden="true" size="small" class="mr-1">mdi-link</v-icon>
                </span>
              </template>
            </v-tooltip>
            <v-tooltip v-if="isRemote(item)" text="Remote entity" location="top">
              <template v-slot:activator="{ props }">
                <span v-bind="props" tabindex="0" role="img" aria-label="Remote entity">
                  <v-icon aria-hidden="true" size="small">mdi-cloud-outline</v-icon>
                </span>
              </template>
            </v-tooltip>
          </template>
          <template #no-data>
            <p class="text-grey py-8">No subordinates found.</p>
          </template>
          <template #bottom>
            <TablePager v-model:page="page" v-model:items-per-page="itemsPerPage" :total="filteredCount"/>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import TablePager from '@/components/TablePager.vue';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {federationEntityPath, intermediateModulePath, trustAnchorModulePath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, loading} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const subordinates = ref([]);
const entityIdentifier = ref(null);
const search = ref('');
const page = ref(1);
const itemsPerPage = ref(50);

const headers = [
  {title: 'Entity Identifier', key: 'entityIdentifier', sortable: true},
  {title: 'Status', key: 'statusText', sortable: true},
];

// Status is a sortable text, the icons are rendered from the flags in the table slot.
const tableItems = computed(() => subordinates.value.map((subordinate) => ({
  ...subordinate,
  statusText: [
    hasEcLocation(subordinate) ? 'EC Location configured' : '',
    isRemote(subordinate) ? 'Remote entity' : '',
  ].filter(Boolean).join(', '),
})));

const filteredCount = computed(() => {
  const term = (search.value || '').toLowerCase();
  if (!term) return subordinates.value.length;
  return subordinates.value.filter((subordinate) => (subordinate.entityIdentifier || '').toLowerCase().includes(term)).length;
});

watch(search, () => {
  page.value = 1;
});

const entityId = computed(() => route.params.entityId);
const moduleType = computed(() => route.params.moduleType);
const taImId = computed(() => route.query.taImId || null);

function listBasePath() {
  return `/entities/${entityId.value}/modules/${moduleType.value}/subordinates`;
}

function queryParams() {
  const params = new URLSearchParams();
  if (taImId.value) params.set('taImId', taImId.value);
  return params.toString();
}

async function loadSubordinates() {
  errorStore.clearError();

  if (!taImId.value) {
    subordinates.value = [];
    return;
  }

  let modulePath = null;
  if (moduleType.value === 'trustanchor') {
    modulePath = trustAnchorModulePath(userStore.selectedTenant, userStore.orgNumber, taImId.value);
  } else if (moduleType.value === 'intermediate') {
    modulePath = intermediateModulePath(userStore.selectedTenant, userStore.orgNumber, taImId.value);
  }

  if (!modulePath) {
    subordinates.value = [];
    return;
  }

  const response = await requestGet(modulePath);

  if (response && response.subordinates && Array.isArray(response.subordinates)) {
    subordinates.value = response.subordinates;
  } else {
    subordinates.value = [];
  }
}

function hasEcLocation(subordinate) {
  return !!subordinate.ecLocation || !!subordinate.ecLocationAutomaticResolve;
}

function isRemote(subordinate) {
  if (subordinate.ecLocationAutomaticResolve) return false;
  const prefix = userStore.entityPrefix;
  if (!prefix || !subordinate.entityIdentifier) return false;
  return !subordinate.entityIdentifier.startsWith(prefix);
}

function addSubordinate() {
  router.push(`${listBasePath()}/new?${queryParams()}`);
}

function openSubordinate(_event, {item}) {
  router.push(`${listBasePath()}/${item.subordinateId}/edit?${queryParams()}`);
}

function goBack() {
  router.push({name: 'federation-entities'});
}

async function loadEntityIdentifier() {
  if (!entityId.value) return;
  const response = await requestGet(federationEntityPath(userStore.selectedTenant, userStore.orgNumber, entityId.value));
  if (response) {
    entityIdentifier.value = response.entityIdentifier || response.federationEntity?.entityIdentifier || null;
  }
}

onMounted(() => {
  loadEntityIdentifier();
  loadSubordinates();
});
</script>
