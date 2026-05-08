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
    <!-- Header -->
    <div class="d-flex align-center mb-4 gap-4">
      <v-btn
          id="btn-back"
          variant="text"
          prepend-icon="mdi-arrow-left"
          @click="router.push({name: 'registrations-list'})"
      >
        Back
      </v-btn>
      <h2 class="flex-grow-1">Registration Detail</h2>
      <v-btn
          v-if="registration && registration.statusFedreg === 'PENDING_APPROVAL'"
          id="btn-reject"
          color="error"
          @click="rejectDialog = true"
      >
        Reject
      </v-btn>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
          <p class="mt-4 text-grey">Loading registration...</p>
        </div>
      </v-card-text>
    </v-card>

    <template v-else-if="registration">
      <!-- Summary card -->
      <v-card class="mb-4">
        <v-card-title>Summary</v-card-title>
        <v-card-text>
          <v-table density="compact">
            <tbody>
              <tr>
                <td class="font-weight-bold field-label">Registration ID</td>
                <td class="text-mono">{{ registration.registrationId }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold field-label">Join ID</td>
                <td class="text-mono">{{ registration.joinId }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold field-label">Entity ID</td>
                <td class="text-mono">{{ registration.entityId }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold field-label">Intermediate Entity ID</td>
                <td class="text-mono">{{ registration.intermediateEntityId }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold field-label">Status</td>
                <td>
                  <v-chip :color="statusColor(registration.statusFedreg)" size="small" label>
                    {{ statusLabel(registration.statusFedreg) }}
                  </v-chip>
                </td>
              </tr>
              <tr v-if="registration.rejectionReason">
                <td class="font-weight-bold field-label">Rejection Reason</td>
                <td>{{ registration.rejectionReason }}</td>
              </tr>
              <tr v-if="registration.tags && registration.tags.length">
                <td class="font-weight-bold field-label">Tags</td>
                <td>
                  <v-chip
                      v-for="tag in registration.tags"
                      :key="tag"
                      size="small"
                      class="mr-1"
                  >{{ tag }}</v-chip>
                </td>
              </tr>
              <tr v-if="registration.isHosted !== undefined && registration.isHosted !== null">
                <td class="font-weight-bold field-label">Hosted</td>
                <td>{{ registration.isHosted ? 'Yes' : 'No' }}</td>
              </tr>
              <tr v-if="registration.hostedId">
                <td class="font-weight-bold field-label">Hosted ID</td>
                <td class="text-mono">{{ registration.hostedId }}</td>
              </tr>
              <tr v-if="registration.trustmarksRequested && registration.trustmarksRequested.length">
                <td class="font-weight-bold field-label">Trustmarks Requested</td>
                <td>
                  <div
                      v-for="tm in registration.trustmarksRequested"
                      :key="tm"
                      class="text-mono text-body-2"
                  >{{ tm }}</div>
                </td>
              </tr>
              <tr v-if="registration.statusTrustmarks && registration.statusTrustmarks.length">
                <td class="font-weight-bold field-label">Trustmark Status</td>
                <td>
                  <div
                      v-for="ts in registration.statusTrustmarks"
                      :key="ts.trustmarkId"
                      class="text-body-2"
                  >{{ ts.trustmarkId }}: {{ ts.status }}</div>
                </td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>

      <!-- Tabs: Entity Statement / Metadata Policy -->
      <v-card>
        <v-tabs v-model="activeTab" color="primary">
          <v-tab value="jwks">Entity Statement (JWKS)</v-tab>
          <v-tab value="metadataPolicy">Metadata Policy</v-tab>
        </v-tabs>

        <v-divider></v-divider>

        <v-window v-model="activeTab">
          <v-window-item value="jwks">
            <v-card-text>
              <pre
                  v-if="registration.jwks"
                  class="json-block"
              >{{ JSON.stringify(registration.jwks, null, 2) }}</pre>
              <p v-else class="text-grey text-center py-6">No JWKS data available.</p>
            </v-card-text>
          </v-window-item>

          <v-window-item value="metadataPolicy">
            <v-card-text>
              <pre
                  v-if="registration.metadataPolicy"
                  class="json-block"
              >{{ JSON.stringify(registration.metadataPolicy, null, 2) }}</pre>
              <p v-else class="text-grey text-center py-6">No metadata policy available.</p>
            </v-card-text>
          </v-window-item>
        </v-window>
      </v-card>
    </template>

    <!-- Reject Dialog -->
    <v-dialog v-model="rejectDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Reject Registration</v-card-title>
        <v-card-text>
          <p class="mb-3">
            You are about to reject the registration for
            <strong>{{ registration?.entityId }}</strong>.
          </p>
          <v-textarea
              v-model="rejectionReason"
              label="Rejection reason"
              rows="3"
              variant="outlined"
              :error="reasonError"
              :error-messages="reasonError ? 'A rejection reason is required.' : ''"
              auto-grow
          ></v-textarea>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-reject-cancel"
              variant="text"
              color="grey"
              :disabled="rejecting"
              @click="closeRejectDialog"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-reject-confirm"
              color="error"
              :loading="rejecting"
              :disabled="rejecting"
              @click="submitReject"
          >
            Confirm Reject
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {registrationAdminItemPath, registrationAdminRejectPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const errorStore = useErrorStore();

const {requestGet, loading} = useRequest();
const {requestPost, loading: rejecting, ok} = useRequest();

const registration = ref(null);
const activeTab = ref('jwks');

const rejectDialog = ref(false);
const rejectionReason = ref('');
const reasonError = ref(false);

function statusColor(status) {
  const colors = {PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'error', STARTED: 'info'};
  return colors[status] ?? 'default';
}

function statusLabel(status) {
  const labels = {PENDING_APPROVAL: 'Pending Approval', APPROVED: 'Approved', REJECTED: 'Rejected', STARTED: 'Started'};
  return labels[status] ?? status;
}

function closeRejectDialog() {
  rejectDialog.value = false;
  rejectionReason.value = '';
  reasonError.value = false;
}

async function submitReject() {
  if (!rejectionReason.value.trim()) {
    reasonError.value = true;
    return;
  }
  reasonError.value = false;

  errorStore.clearError();
  await requestPost(registrationAdminRejectPath(route.params.id), {
    rejectionReason: rejectionReason.value.trim(),
  });

  if (ok.value) {
    closeRejectDialog();
    router.push({name: 'registrations-list'});
  }
}

async function loadRegistration() {
  errorStore.clearError();
  registration.value = await requestGet(registrationAdminItemPath(route.params.id));
}

onMounted(() => {
  loadRegistration();
});
</script>

<style scoped>
.gap-4 {
  gap: 16px;
}

.field-label {
  width: 220px;
  white-space: nowrap;
  color: #5c5c5c;
}

.text-mono {
  font-family: monospace;
  font-size: 0.85rem;
  word-break: break-all;
}

.json-block {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 16px;
  font-family: monospace;
  font-size: 0.82rem;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 480px;
}
</style>
