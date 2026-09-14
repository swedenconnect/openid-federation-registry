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
    <div class="d-flex justify-space-between align-center mb-4">
      <h2>Domain Request</h2>
      <div>
        <v-btn
            v-if="isPending"
            id="btn-approve-domain"
            color="success"
            class="mr-2"
            :loading="approving"
            :disabled="approving"
            @click="approve"
        >
          Approve
        </v-btn>
        <v-btn
            v-if="isPending"
            id="btn-reject-domain"
            color="error"
            class="mr-2"
            :disabled="approving"
            @click="rejectDialog = true"
        >
          Reject
        </v-btn>
        <v-btn
            id="btn-back"
            color="grey"
            @click="router.push({name: 'registrations-list', query: {type: 'DOMAIN'}})"
        >
          Back
        </v-btn>
      </div>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">Loading domain request...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="domain" class="mb-4">
      <v-card-title>Summary</v-card-title>
      <v-card-text>
        <v-table density="compact">
          <tbody>
            <tr>
              <td class="font-weight-bold field-label">Domain</td>
              <td class="text-mono">{{ domain.domain }}</td>
            </tr>
            <tr>
              <td class="font-weight-bold field-label">Organization</td>
              <td>{{ domain.legalName || domain.orgName || '—' }}</td>
            </tr>
            <tr>
              <td class="font-weight-bold field-label">Organization Number</td>
              <td class="text-mono">{{ domain.orgNumber }}</td>
            </tr>
            <tr>
              <td class="font-weight-bold field-label">Status</td>
              <td>
                <v-chip :color="statusColor(domain.status)" size="small" label>
                  {{ statusLabel(domain.status) }}
                </v-chip>
              </td>
            </tr>
            <tr>
              <td class="font-weight-bold field-label">Requested</td>
              <td>{{ formatDate(domain.createdDate) }}</td>
            </tr>
            <tr v-if="domain.reviewedAt">
              <td class="font-weight-bold field-label">Reviewed</td>
              <td>{{ formatDate(domain.reviewedAt) }}</td>
            </tr>
            <tr v-if="domain.reviewedBy">
              <td class="font-weight-bold field-label">Reviewed By</td>
              <td>{{ domain.reviewedBy }}</td>
            </tr>
            <tr v-if="domain.rejectionReason">
              <td class="font-weight-bold field-label">Rejection Reason</td>
              <td>{{ domain.rejectionReason }}</td>
            </tr>
          </tbody>
        </v-table>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">Domain request not found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Reject Dialog -->
    <v-dialog v-model="rejectDialog" max-width="500" aria-labelledby="reject-domain-dialog-title">
      <v-card>
        <v-card-title id="reject-domain-dialog-title" class="text-h5">Reject Domain</v-card-title>
        <v-card-text>
          <p class="mb-3">
            You are about to reject <strong>{{ domain?.domain }}</strong> for
            <strong>{{ domain?.legalName || domain?.orgName || domain?.orgNumber }}</strong>.
            Registrations that rely on this domain alone are rejected with it.
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
              id="btn-reject-domain-cancel"
              variant="text"
              color="grey"
              :disabled="rejecting"
              @click="closeRejectDialog"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-reject-domain-confirm"
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

    <v-snackbar v-model="snackbar" :timeout="6000" color="primary">
      {{ snackbarMessage }}
    </v-snackbar>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {
  registrationAdminDomainApprovePath,
  registrationAdminDomainRejectPath,
  registrationAdminDomainsPath,
} from '@/config/path';

const route = useRoute();
const router = useRouter();
const errorStore = useErrorStore();
const userStore = useUserStore();

const {requestGet, loading} = useRequest();
const {requestPost: requestApprove, ok: approveOk, loading: approving} = useRequest();
const {requestPost: requestReject, ok: rejectOk, loading: rejecting} = useRequest();

const domain = ref(null);

const rejectDialog = ref(false);
const rejectionReason = ref('');
const reasonError = ref(false);

const snackbar = ref(false);
const snackbarMessage = ref('');

const isPending = computed(() => domain.value?.status === 'PENDING');

function statusColor(status) {
  const colors = {PENDING: 'warning', VALIDATED: 'success', REJECTED: 'error'};
  return colors[status] ?? 'default';
}

function statusLabel(status) {
  const labels = {PENDING: 'Pending Approval', VALIDATED: 'Approved', REJECTED: 'Rejected'};
  return labels[status] ?? status;
}

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function showSnackbar(message) {
  snackbarMessage.value = message;
  snackbar.value = true;
}

// There is no single-domain operator endpoint: the review list is the source, filtered down to the one domain
// this page is about.
async function loadDomain() {
  errorStore.clearError();
  const response = await requestGet(
      registrationAdminDomainsPath(userStore.selectedTenant, userStore.orgNumber));
  const list = Array.isArray(response) ? response : [];
  domain.value = list.find(item => String(item.domainId) === String(route.params.domainId)) ?? null;
}

async function approve() {
  errorStore.clearError();
  await requestApprove(
      registrationAdminDomainApprovePath(userStore.selectedTenant, userStore.orgNumber, route.params.domainId));
  if (approveOk.value) {
    showSnackbar(`${domain.value?.domain} approved.`);
    await loadDomain();
  }
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

  const rejected = domain.value;
  const response = await requestReject(
      registrationAdminDomainRejectPath(userStore.selectedTenant, userStore.orgNumber, route.params.domainId), {
        rejectionReason: rejectionReason.value.trim(),
      });

  if (rejectOk.value) {
    const cascaded = response?.cascadedRegistrationIds?.length ?? 0;
    closeRejectDialog();
    showSnackbar(cascaded === 0
        ? `${rejected?.domain} rejected. No registrations were affected.`
        : `${rejected?.domain} rejected. ${cascaded} registration(s) rejected with it.`);
    await loadDomain();
  }
}

onMounted(() => {
  loadDomain();
});
</script>

<style scoped>
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
</style>
