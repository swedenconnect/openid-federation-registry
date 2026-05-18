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
      <h2>Registration Detail</h2>
      <div>
        <v-btn
            v-if="registration && registration.statusFedreg === 'PENDING_APPROVAL'"
            id="btn-reject"
            color="error"
            class="mr-2"
            @click="rejectDialog = true"
        >
          Reject
        </v-btn>
        <v-btn
            id="btn-back"
            color="grey"
            @click="router.push({name: 'registrations-list'})"
        >
          Back
        </v-btn>
      </div>
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
                <td class="text-mono">{{ registration.entityIdentifier }}</td>
              </tr>
              <tr>
                <td class="font-weight-bold field-label">Intermediate Entity ID</td>
                <td class="text-mono">{{ registration.intermediateEntityId }}</td>
              </tr>
              <tr v-if="registration.organizationName">
                <td class="font-weight-bold field-label">Organization</td>
                <td>{{ registration.organizationName }}</td>
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
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>

      <!-- Tabs -->
      <v-card>
        <v-tabs v-model="activeTab" color="primary">
          <v-tab value="entityStatement">Entity Statement</v-tab>
          <v-tab value="metadataPolicy">Metadata Policy</v-tab>
          <v-tab value="trustmarkRequests">Trustmark requests</v-tab>
        </v-tabs>

        <v-divider></v-divider>

        <v-window v-model="activeTab">

          <!-- Entity Statement tab -->
          <v-window-item value="entityStatement">
            <v-card-text>
              <div v-if="entityStatementLoading" class="text-center py-12">
                <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
                <p class="mt-4 text-grey">Fetching entity configuration…</p>
              </div>
              <v-alert
                  v-else-if="entityStatementError"
                  type="error"
                  variant="tonal"
                  class="mt-2"
              >
                {{ entityStatementError }}
              </v-alert>
              <template v-else-if="entityStatement">
                <p class="text-overline text-grey mb-1 mt-2">Header</p>
                <pre class="json-block mb-6">{{ JSON.stringify(entityStatement.header, null, 2) }}</pre>
                <p class="text-overline text-grey mb-1">Payload</p>
                <pre class="json-block">{{ JSON.stringify(entityStatement.payload, null, 2) }}</pre>
              </template>
              <p v-else class="text-grey text-center py-6">No entity configuration available.</p>
            </v-card-text>
          </v-window-item>

          <!-- Metadata Policy tab -->
          <v-window-item value="metadataPolicy">
            <v-card-text>
              <pre
                  v-if="registration.metadataPolicy"
                  class="json-block"
              >{{ JSON.stringify(registration.metadataPolicy, null, 2) }}</pre>
              <p v-else class="text-grey text-center py-6">No metadata policy available.</p>
            </v-card-text>
          </v-window-item>

          <!-- Trustmark requests tab -->
          <v-window-item value="trustmarkRequests">
            <v-card-text>
              <div v-if="trustmarksTabLoading" class="text-center py-12">
                <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
                <p class="mt-4 text-grey">Loading trustmark data…</p>
              </div>
              <template v-else-if="registration.statusTrustmarks?.length">
                <v-table density="compact">
                  <thead>
                    <tr>
                      <th class="text-left">Trustmark type</th>
                      <th class="text-left">Status</th>
                      <th class="text-left">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in trustmarkStatuses" :key="item.type">
                      <td class="text-mono text-body-2">{{ item.type }}</td>
                      <td>
                        <v-chip
                            v-if="item.status === 'enrolled'"
                            color="success"
                            size="small"
                            label
                        >Enrolled</v-chip>
                        <v-chip
                            v-else-if="item.status === 'not_subject'"
                            color="warning"
                            size="small"
                            label
                        >Not yet enrolled</v-chip>
                        <v-chip
                            v-else
                            color="default"
                            size="small"
                            label
                        >Not in organization</v-chip>
                      </td>
                      <td>
                        <v-btn
                            v-if="item.status === 'not_subject' && item.addSubjectLink"
                            color="primary"
                            variant="text"
                            size="small"
                            @click="router.push(item.addSubjectLink)"
                        >
                          Add as subject
                        </v-btn>
                      </td>
                    </tr>
                  </tbody>
                </v-table>
              </template>
              <p v-else class="text-grey text-center py-6">No trustmarks requested.</p>
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
            <strong>{{ registration?.entityIdentifier }}</strong>.
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
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {
  entityConfigurationViewPath,
  modulesPath,
  registrationAdminItemPath,
  registrationAdminRejectPath,
  trustmarksPath,
} from '@/config/path';

const route = useRoute();
const router = useRouter();
const errorStore = useErrorStore();

const {requestGet, loading} = useRequest();
const {requestPost, loading: rejecting, ok} = useRequest();

const registration = ref(null);
const activeTab = ref('entityStatement');

const rejectDialog = ref(false);
const rejectionReason = ref('');
const reasonError = ref(false);

// Entity statement tab
const entityStatement = ref(null);
const entityStatementLoading = ref(false);
const entityStatementError = ref(null);
const entityStatementLoaded = ref(false);

// Trustmark requests tab
const allTrustmarks = ref([]);
const trustmarkIssuersMap = ref({});
const trustmarksTabLoading = ref(false);
const trustmarksLoaded = ref(false);

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

async function loadEntityStatement() {
  if (entityStatementLoaded.value || !registration.value?.entityIdentifier) return;
  entityStatementLoading.value = true;
  entityStatementError.value = null;
  try {
    const res = await fetch(entityConfigurationViewPath, {
      method: 'POST',
      credentials: 'include',
      headers: {'Content-Type': 'text/plain'},
      body: registration.value.entityIdentifier,
    });
    if (res.ok) {
      entityStatement.value = await res.json();
    } else {
      entityStatementError.value = 'Could not fetch entity configuration. The entity may be unreachable.';
    }
  } catch {
    entityStatementError.value = 'Could not fetch entity configuration. The entity may be unreachable.';
  } finally {
    entityStatementLoading.value = false;
    entityStatementLoaded.value = true;
  }
}

async function loadTrustmarkData() {
  if (trustmarksLoaded.value) return;
  trustmarksTabLoading.value = true;
  try {
    const [tmRes, modRes] = await Promise.all([
      fetch(`${trustmarksPath}?includeSubjects=true`, {credentials: 'include'}),
      fetch(`${modulesPath}?type=trustmarkissuer`, {credentials: 'include'}),
    ]);
    if (tmRes.ok) {
      allTrustmarks.value = await tmRes.json();
    }
    if (modRes.ok) {
      const modData = await modRes.json();
      const map = {};
      (modData.trustmarkIssuers ?? []).forEach(m => {
        map[m.trustmarkIssuerId] = m.entityId;
      });
      trustmarkIssuersMap.value = map;
    }
  } catch {
    // empty state handles this gracefully
  } finally {
    trustmarksTabLoading.value = false;
    trustmarksLoaded.value = true;
  }
}

const trustmarkStatuses = computed(() => {
  if (!registration.value?.statusTrustmarks?.length) return [];
  const allTypes = registration.value.statusTrustmarks.flatMap(tm => (tm.trustmarkStatus ?? []).map(s => s.trustmarkType));
  return allTypes.map(requestedType => {
    const match = allTrustmarks.value.find(tm => tm.trustmarkType === requestedType);
    if (!match) {
      return {type: requestedType, status: 'not_in_org'};
    }
    const isSubject = (match.trustmarkSubjects ?? []).some(
        s => s.subject === registration.value.entityIdentifier,
    );
    if (isSubject) {
      return {type: requestedType, status: 'enrolled', trustmark: match};
    }
    const entityId = trustmarkIssuersMap.value[match.trustmarkissuerId];
    const addSubjectLink = entityId
        ? `/entities/${entityId}/modules/trustmarkissuer/trustmarks/${match.trustmarkId}/subjects/new?trustmarkIssuerId=${match.trustmarkissuerId}&subject=${encodeURIComponent(registration.value.entityIdentifier)}`
        : null;
    return {type: requestedType, status: 'not_subject', trustmark: match, addSubjectLink};
  });
});

watch(activeTab, (tab) => {
  if (tab === 'trustmarkRequests' && !trustmarksLoaded.value) {
    loadTrustmarkData();
  }
});

async function loadRegistration() {
  errorStore.clearError();
  registration.value = await requestGet(registrationAdminItemPath(route.params.id));
  if (registration.value) {
    loadEntityStatement();
  }
}

onMounted(() => {
  loadRegistration();
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
