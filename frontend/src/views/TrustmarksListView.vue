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

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading trustmarks...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="trustmarks.length > 0">
      <v-table>
        <thead>
        <tr>
          <th class="text-left">Trustmark Type</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="trustmark in trustmarks" :key="trustmark.trustmarkId">
          <td>{{ trustmark.trustmarkType || 'N/A' }}</td>
          <td class="text-right">
            <v-btn
                :id="'btn-edit-trustmark-' + trustmark.trustmarkId"
                color="primary"
                variant="text"
                size="small"
                @click="editTrustmark(trustmark.trustmarkId)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                :id="'btn-subjects-trustmark-' + trustmark.trustmarkId"
                color="secondary"
                variant="text"
                size="small"
                @click="viewSubjects(trustmark.trustmarkId)"
                class="mr-2"
            >
              Subjects
            </v-btn>
            <v-btn
                :id="'btn-delete-trustmark-' + trustmark.trustmarkId"
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(trustmark)"
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
          <p class="text-grey">No trustmarks found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete trustmark "{{ deleteTrustmarkLabel }}"? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-delete-trustmark-cancel"
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deleting"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-delete-trustmark-confirm"
              color="error"
              @click="deleteTrustmark"
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
import {trustmarksListingPath, trustmarksPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();

const trustmarks = ref([]);
const deleteDialog = ref(false);
const deleting = ref(false);
const trustmarkToDelete = ref(null);

const entityId = computed(() => route.params.entityId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

const deleteTrustmarkLabel = computed(() => {
  if (!trustmarkToDelete.value) return '';
  return trustmarkToDelete.value.trustmarkType || 'N/A';
});

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

  const response = await requestGet(trustmarksListingPath(trustmarkIssuerId.value));

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

function confirmDelete(trustmark) {
  trustmarkToDelete.value = trustmark;
  deleteDialog.value = true;
}

async function deleteTrustmark() {
  if (!trustmarkToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    const trustmarkId = trustmarkToDelete.value.trustmarkId;

    if (!trustmarkId) {
      errorStore.setError('Trustmark ID not found');
      deleting.value = false;
      return;
    }

    await requestDelete(`${trustmarksPath(trustmarkIssuerId.value)}/${trustmarkId}`);

    if (ok.value) {
      deleteDialog.value = false;
      trustmarkToDelete.value = null;
      await loadTrustmarks();
    }
  } catch (error) {
    console.error('Error deleting trustmark:', error);
  } finally {
    deleting.value = false;
  }
}

function goBack() {
  router.push('/');
}

onMounted(() => {
  loadTrustmarks();
});
</script>
