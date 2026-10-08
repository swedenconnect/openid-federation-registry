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
      <h2>Trustmarks</h2>
      <div>
        <v-btn
            id="btn-add-trustmark"
            color="primary"
            @click="addTrustmark"
            class="mr-2"
        >
          Add Trustmark
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
        <v-icon aria-hidden="true" class="mr-2">mdi-certificate-outline</v-icon>
        <span class="text-caption text-uppercase font-weight-medium mr-3">Trustmark Issuer</span>
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
          <p class="mt-4 text-grey">Loading trustmarks...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="trustmarks.length > 0">
      <v-table>
        <caption class="sr-only">List of trustmarks</caption>
        <thead>
        <tr>
          <th class="text-left">Trustmark Type</th>
          <th class="text-right"><span class="sr-only">Actions</span></th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="trustmark in trustmarks"
            :key="trustmark.trustmarkId"
            class="clickable-row"
            tabindex="0"
            @click="viewSubjects(trustmark.trustmarkId)"
            @keydown.enter="viewSubjects(trustmark.trustmarkId)"
        >
          <td>{{ trustmark.trustmarkType || 'N/A' }}</td>
          <td class="text-right">
            <v-btn
                :id="'btn-edit-trustmark-' + trustmark.trustmarkId"
                icon="mdi-cog-outline"
                variant="text"
                size="small"
                aria-label="Edit trustmark"
                title="Edit trustmark"
                @click.stop="editTrustmark(trustmark.trustmarkId)"
                @keydown.enter.stop
            ></v-btn>
          </td>
        </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No trustmarks found.</p>
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
import {federationEntityPath, trustmarksListingPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, loading} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const trustmarks = ref([]);
const entityIdentifier = ref(null);

const entityId = computed(() => route.params.entityId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

function listBasePath() {
  return `/entities/${entityId.value}/modules/trustmarkissuer/trustmarks`;
}

function queryParams() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  return params.toString();
}

async function loadTrustmarks() {
  errorStore.clearError();

  if (!trustmarkIssuerId.value) {
    trustmarks.value = [];
    return;
  }

  const response = await requestGet(trustmarksListingPath(userStore.selectedTenant, userStore.orgNumber, trustmarkIssuerId.value));

  if (response && Array.isArray(response)) {
    trustmarks.value = response;
  } else {
    trustmarks.value = [];
  }
}

function addTrustmark() {
  router.push(`${listBasePath()}/new?${queryParams()}`);
}

function editTrustmark(trustmarkId) {
  router.push(`${listBasePath()}/${trustmarkId}/edit?${queryParams()}`);
}

function viewSubjects(trustmarkId) {
  router.push(`${listBasePath()}/${trustmarkId}/subjects?${queryParams()}`);
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
  loadTrustmarks();
});
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}
</style>
