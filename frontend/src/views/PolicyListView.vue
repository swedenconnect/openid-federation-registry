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
      <h2>Policies</h2>
      <v-btn
          color="primary"
          @click="addPolicy"
      >
        Add
      </v-btn>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading policies...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="policies.length > 0">
      <v-table>
        <thead>
        <tr>
          <th class="text-left">Name</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="policy in policies" :key="policy.policyId">
          <td>{{ policy.name || 'Unnamed Policy' }}</td>
          <td class="text-right">
            <v-btn
                color="primary"
                variant="text"
                size="small"
                @click="editPolicy(policy.policyId)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(policy)"
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
          <p class="text-grey">No policies found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete policy "{{ deletePolicyName }}"? This action cannot be undone.
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
              @click="deletePolicy"
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
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {policiesPath} from '@/config/path';
import {useErrorStore} from '@/stores/errorStore';

const router = useRouter();
const {requestGet, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();

const policies = ref([]);
const deleteDialog = ref(false);
const deleting = ref(false);
const policyToDelete = ref(null);
const deletePolicyName = computed(() => {
  if (!policyToDelete.value) return '';
  return policyToDelete.value.name || 'Unnamed Policy';
});

async function loadPolicies() {
  errorStore.clearError();
  const response = await requestGet(policiesPath);
  if (response && Array.isArray(response)) {
    policies.value = response;
  }
}

function editPolicy(policyId) {
  router.push(`/policies/${policyId}/edit`);
}

function addPolicy() {
  router.push('/policies/new');
}

function confirmDelete(policy) {
  policyToDelete.value = policy;
  deleteDialog.value = true;
}

async function deletePolicy() {
  if (!policyToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    const policyId = policyToDelete.value.policyId;

    if (!policyId) {
      errorStore.setError('Policy ID not found');
      deleting.value = false;
      return;
    }

    await requestDelete(`${policiesPath}/${policyId}`);

    if (ok.value) {
      deleteDialog.value = false;
      policyToDelete.value = null;
      await loadPolicies();
    }
  } catch (error) {
    console.error('Error deleting policy:', error);
  } finally {
    deleting.value = false;
  }
}

onMounted(() => {
  loadPolicies();
});
</script>
