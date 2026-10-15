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
      <h2>Trustmark Subjects</h2>
      <div>
        <v-btn
            id="btn-add-trustmark-subject"
            color="primary"
            @click="addSubject"
            class="mr-2"
        >
          Add Trustmark Subject
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

    <v-card v-if="entityIdentifier || trustmarkType" variant="tonal" color="primary" class="mb-4">
      <v-card-text class="py-2">
        <div v-if="entityIdentifier" class="d-flex align-center">
          <v-icon aria-hidden="true" class="mr-2">mdi-certificate-outline</v-icon>
          <span class="text-caption text-uppercase font-weight-medium mr-3">Trustmark Issuer</span>
          <span class="text-body-2">{{ entityIdentifier }}</span>
        </div>
        <div v-if="trustmarkType" class="d-flex align-center" :class="{ 'mt-1': entityIdentifier }">
          <v-icon aria-hidden="true" class="mr-2">mdi-tag-outline</v-icon>
          <span class="text-caption text-uppercase font-weight-medium mr-3">Trustmark Type</span>
          <span class="text-body-2">{{ trustmarkType }}</span>
        </div>
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
          <p class="mt-4 text-grey">Loading trustmark subjects...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="subjects.length > 0">
      <v-table>
        <caption class="sr-only">List of trustmark subjects</caption>
        <thead>
        <tr>
          <th class="text-left">Subject</th>
        </tr>
        </thead>
        <tbody>
        <tr
            v-for="subject in subjects"
            :key="subject.trustmarksubjectId"
            :id="'row-subject-' + subject.trustmarksubjectId"
            class="clickable-row"
            tabindex="0"
            @click="editSubject(subject.trustmarksubjectId)"
            @keydown.enter="editSubject(subject.trustmarksubjectId)"
        >
          <td>{{ subject.subject || 'N/A' }}</td>
        </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No trustmark subjects found.</p>
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
import {federationEntityPath, trustmarksPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, loading} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const subjects = ref([]);
const entityIdentifier = ref(null);
const trustmarkType = ref(null);

const entityId = computed(() => route.params.entityId);
const trustmarkId = computed(() => route.params.trustmarkId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

function listBasePath() {
  return `/entities/${entityId.value}/modules/trustmarkissuer/trustmarks/${trustmarkId.value}/subjects`;
}

function queryParams() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  return params.toString();
}

async function loadSubjects() {
  errorStore.clearError();

  if (!trustmarkId.value) {
    subjects.value = [];
    return;
  }

  const response = await requestGet(`${trustmarksPath(userStore.selectedTenant, userStore.orgNumber)}/${trustmarkId.value}/subjects`);

  if (response) {
    trustmarkType.value = response.trustmarkType || null;
    subjects.value = response.trustmarkSubjects && Array.isArray(response.trustmarkSubjects)
        ? response.trustmarkSubjects
        : [];
  } else {
    subjects.value = [];
  }
}

function addSubject() {
  router.push(`${listBasePath()}/new?${queryParams()}`);
}

function editSubject(subjectId) {
  router.push(`${listBasePath()}/${subjectId}/edit?${queryParams()}`);
}

function goBack() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  router.push(`/entities/${entityId.value}/modules/trustmarkissuer/trustmarks?${params.toString()}`);
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
  loadSubjects();
});
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}
</style>
