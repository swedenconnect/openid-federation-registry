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
            color="primary"
            @click="addSubordinate"
            class="mr-2"
        >
          Add Subordinate
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
          <p class="mt-4 text-grey">Loading subordinates...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="subordinates.length > 0">
      <v-table>
        <thead>
        <tr>
          <th class="text-left">Entity Identifier</th>
          <th class="text-left">Status</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="subordinate in subordinates" :key="subordinate.subordinateId">
          <td>{{ subordinate.entityIdentifier || 'N/A' }}</td>
          <td>
            <v-tooltip v-if="hasEcLocation(subordinate)" text="EC Location configured" location="top">
              <template v-slot:activator="{ props }">
                <v-icon v-bind="props" size="small" class="mr-1">mdi-link</v-icon>
              </template>
            </v-tooltip>
            <v-tooltip v-if="isRemote(subordinate)" text="Remote entity" location="top">
              <template v-slot:activator="{ props }">
                <v-icon v-bind="props" size="small">mdi-cloud-outline</v-icon>
              </template>
            </v-tooltip>
          </td>
          <td class="text-right">
            <v-btn
                color="primary"
                variant="text"
                size="small"
                @click="editSubordinate(subordinate.subordinateId)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(subordinate)"
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
          <p class="text-grey">No subordinates found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete subordinate "{{ deleteSubordinateLabel }}"? This action cannot be undone.
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
              @click="deleteSubordinate"
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
import {useUserStore} from '@/stores/userStore';

const route = useRoute();
const router = useRouter();
const {requestGet, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const subordinates = ref([]);
const deleteDialog = ref(false);
const deleting = ref(false);
const subordinateToDelete = ref(null);

const entityId = computed(() => route.params.entityId);
const moduleType = computed(() => route.params.moduleType);
const taImId = computed(() => route.query.taImId || null);

const deleteSubordinateLabel = computed(() => {
  if (!subordinateToDelete.value) return '';
  return subordinateToDelete.value.entityIdentifier || 'N/A';
});

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
    modulePath = `/api/v1/modules/trust-anchor/${taImId.value}`;
  } else if (moduleType.value === 'intermediate') {
    modulePath = `/api/v1/modules/intermediate/${taImId.value}`;
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

function confirmDelete(subordinate) {
  subordinateToDelete.value = subordinate;
  deleteDialog.value = true;
}

async function deleteSubordinate() {
  if (!subordinateToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    const subordinateId = subordinateToDelete.value.subordinateId;

    if (!subordinateId) {
      errorStore.setError('Subordinate ID not found');
      deleting.value = false;
      return;
    }

    await requestDelete(`/api/v1/subordinates/${subordinateId}`);

    if (ok.value) {
      deleteDialog.value = false;
      subordinateToDelete.value = null;
      await loadSubordinates();
    }
  } catch (error) {
    console.error('Error deleting subordinate:', error);
  } finally {
    deleting.value = false;
  }
}

function goBack() {
  router.push('/');
}

onMounted(() => {
  loadSubordinates();
});
</script>
