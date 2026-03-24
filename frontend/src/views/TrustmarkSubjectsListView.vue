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
      <div>
        <h2>Trustmark Subjects</h2>
        <div v-if="trustmarkType" class="text-subtitle-1 text-grey">{{ trustmarkType }}</div>
      </div>
      <div>
        <v-btn
            color="primary"
            @click="addSubject"
            class="mr-2"
        >
          Add Trustmark Subject
        </v-btn>
        <v-btn
            color="grey"
            @click="goBack"
        >
          Back
        </v-btn>
      </div>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading trustmark subjects...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="subjects.length > 0">
      <v-table>
        <thead>
        <tr>
          <th class="text-left">Subject</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="subject in subjects" :key="subject.trustmarksubjectId">
          <td>{{ subject.subject || 'N/A' }}</td>
          <td class="text-right">
            <v-btn
                color="primary"
                variant="text"
                size="small"
                @click="editSubject(subject.trustmarksubjectId)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(subject)"
            >
              Delete
            </v-btn>
          </td>
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

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete subject "{{ deleteSubjectLabel }}"? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deleting"
          >
            Cancel
          </v-btn>
          <v-btn
              color="error"
              @click="deleteSubject"
              :loading="deleting"
              :disabled="deleting"
          >
            Yes, Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {trustmarksPath, trustmarkSubjectsPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();

const subjects = ref([]);
const trustmarkType = ref('');
const deleteDialog = ref(false);
const deleting = ref(false);
const subjectToDelete = ref(null);

const entityId = computed(() => route.params.entityId);
const trustmarkId = computed(() => route.params.trustmarkId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

const deleteSubjectLabel = computed(() => {
  if (!subjectToDelete.value) return '';
  return subjectToDelete.value.subject || 'N/A';
});

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

  const response = await requestGet(`${trustmarksPath}/${trustmarkId.value}/subjects`);

  if (response) {
    trustmarkType.value = response.trustmarkType || '';
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

function confirmDelete(subject) {
  subjectToDelete.value = subject;
  deleteDialog.value = true;
}

async function deleteSubject() {
  if (!subjectToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    const subjectId = subjectToDelete.value.trustmarksubjectId;

    if (!subjectId) {
      errorStore.setError('Subject ID not found');
      deleting.value = false;
      return;
    }

    await requestDelete(`${trustmarkSubjectsPath}/${subjectId}`);

    if (ok.value) {
      deleteDialog.value = false;
      subjectToDelete.value = null;
      await loadSubjects();
    }
  } catch (error) {
    console.error('Error deleting trustmark subject:', error);
  } finally {
    deleting.value = false;
  }
}

function goBack() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  router.push(`/entities/${entityId.value}/modules/trustmarkissuer/trustmarks?${params.toString()}`);
}

onMounted(() => {
  loadSubjects();
});
</script>
