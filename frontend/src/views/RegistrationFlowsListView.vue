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
      <h2>Registration Flows</h2>
      <v-btn
          id="btn-add-flow"
          color="primary"
          @click="addFlow"
      >
        Add Registration Flow
      </v-btn>
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
          <p class="mt-4 text-grey">Loading registration flows...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="flows.length > 0">
      <v-table>
        <caption class="sr-only">List of registration flows</caption>
        <thead>
        <tr>
          <th class="text-left">Name</th>
          <th class="text-left">Description</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="flow in flows" :key="flow.flowId">
          <td>{{ flow.name || 'N/A' }}</td>
          <td>{{ flow.description || '' }}</td>
          <td class="text-right">
            <v-btn
                :id="'btn-edit-flow-' + flow.flowId"
                color="primary"
                variant="text"
                size="small"
                @click="editFlow(flow.flowId)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                :id="'btn-delete-flow-' + flow.flowId"
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(flow)"
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
          <p class="text-grey">No registration flows found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500" aria-labelledby="delete-flow-dialog-title">
      <v-card>
        <v-card-title id="delete-flow-dialog-title" class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete registration flow "{{ deleteFlowLabel }}"? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-delete-flow-cancel"
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deleting"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-delete-flow-confirm"
              color="error"
              @click="deleteFlow"
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
import {useErrorStore} from '@/stores/errorStore';
import {registrationFlowPath, registrationFlowsPath} from '@/config/path';

const router = useRouter();
const {requestGet, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();

const flows = ref([]);
const deleteDialog = ref(false);
const deleting = ref(false);
const flowToDelete = ref(null);

const deleteFlowLabel = computed(() => flowToDelete.value?.name || 'N/A');

async function loadFlows() {
  errorStore.clearError();
  const response = await requestGet(registrationFlowsPath);
  flows.value = Array.isArray(response) ? response : [];
}

function addFlow() {
  router.push({name: 'registration-flow-new'});
}

function editFlow(flowId) {
  router.push({name: 'registration-flow-edit', params: {id: flowId}});
}

function confirmDelete(flow) {
  flowToDelete.value = flow;
  deleteDialog.value = true;
}

async function deleteFlow() {
  if (!flowToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    await requestDelete(registrationFlowPath(flowToDelete.value.flowId));
    if (ok.value) {
      deleteDialog.value = false;
      flowToDelete.value = null;
      await loadFlows();
    }
  } catch (error) {
    console.error('Error deleting registration flow:', error);
  } finally {
    deleting.value = false;
  }
}

onMounted(() => {
  loadFlows();
});
</script>