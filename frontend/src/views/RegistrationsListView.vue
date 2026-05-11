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

    <v-text-field
        v-model="search"
        prepend-inner-icon="mdi-magnify"
        label="Search by entity ID or intermediate"
        single-line
        hide-details
        clearable
        variant="outlined"
        density="compact"
        class="mb-4"
    ></v-text-field>

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
          <p class="mt-4 text-grey">Loading registrations...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="filteredRegistrations.length > 0">
      <v-table>
        <thead>
          <tr>
            <th class="text-left">Entity ID</th>
            <th class="text-left">Intermediate Entity ID</th>
            <th class="text-left">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr
              v-for="reg in filteredRegistrations"
              :key="reg.registrationId"
              class="clickable-row"
              @click="openDetail(reg.registrationId)"
          >
            <td>{{ reg.entityIdentifyer }}</td>
            <td>{{ reg.intermediateEntityId }}</td>
            <td>
              <v-chip :color="statusColor(reg.statusFedreg)" size="small" label>
                {{ statusLabel(reg.statusFedreg) }}
              </v-chip>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No registrations found.</p>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {registrationAdminPath} from '@/config/path';

const router = useRouter();
const {requestGet, loading} = useRequest();
const errorStore = useErrorStore();

const registrations = ref([]);
const search = ref('');

const STATUS_ORDER = {PENDING_APPROVAL: 0, STARTED: 1, APPROVED: 2, REJECTED: 3};

const filteredRegistrations = computed(() => {
  const q = (search.value ?? '').toLowerCase();
  return registrations.value
      .filter(r => !q
          || r.entityIdentifyer?.toLowerCase().includes(q)
          || r.intermediateEntityId?.toLowerCase().includes(q))
      .sort((a, b) => (STATUS_ORDER[a.statusFedreg] ?? 99) - (STATUS_ORDER[b.statusFedreg] ?? 99));
});

function statusColor(status) {
  const colors = {PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'error', STARTED: 'info'};
  return colors[status] ?? 'default';
}

function statusLabel(status) {
  const labels = {PENDING_APPROVAL: 'Pending Approval', APPROVED: 'Approved', REJECTED: 'Rejected', STARTED: 'Started'};
  return labels[status] ?? status;
}

function openDetail(id) {
  router.push({name: 'registration-detail', params: {id}});
}

async function loadRegistrations() {
  errorStore.clearError();
  const response = await requestGet(registrationAdminPath);
  registrations.value = Array.isArray(response) ? response : [];
}

onMounted(() => {
  loadRegistrations();
});
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}
.clickable-row:hover td {
  background-color: rgba(0, 0, 0, 0.04);
}
</style>
