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

    <v-card v-else-if="subordinates.length > 0">
      <v-table>
        <caption class="sr-only">List of subordinates</caption>
        <thead>
        <tr>
          <th class="text-left">Entity Identifier</th>
          <th class="text-left">Status</th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="subordinate in subordinates"
            :key="subordinate.subordinateId"
            :id="'row-subordinate-' + subordinate.subordinateId"
            class="clickable-row"
            tabindex="0"
            @click="editSubordinate(subordinate.subordinateId)"
            @keydown.enter="editSubordinate(subordinate.subordinateId)"
        >
          <td>{{ subordinate.entityIdentifier || 'N/A' }}</td>
          <td>
            <v-tooltip v-if="hasEcLocation(subordinate)" text="EC Location configured" location="top">
              <template v-slot:activator="{ props }">
                <span v-bind="props" tabindex="0" role="img" aria-label="EC Location configured">
                  <v-icon aria-hidden="true" size="small" class="mr-1">mdi-link</v-icon>
                </span>
              </template>
            </v-tooltip>
            <v-tooltip v-if="isRemote(subordinate)" text="Remote entity" location="top">
              <template v-slot:activator="{ props }">
                <span v-bind="props" tabindex="0" role="img" aria-label="Remote entity">
                  <v-icon aria-hidden="true" size="small">mdi-cloud-outline</v-icon>
                </span>
              </template>
            </v-tooltip>
          </td>
        </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No subordinates found.</p>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
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

function editSubordinate(subordinateId) {
  router.push(`${listBasePath()}/${subordinateId}/edit?${queryParams()}`);
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

<style scoped>
.clickable-row {
  cursor: pointer;
}
</style>
