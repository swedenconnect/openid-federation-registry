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
              <tr v-if="registration.subordinateEntityId">
                <td class="font-weight-bold field-label">Subordinate Entity ID</td>
                <td class="text-mono">{{ registration.subordinateEntityId }}</td>
              </tr>
              <tr v-if="registration.registrationType !== 'TRUST_MARK_SUBORDINATE' && registration.tags && registration.tags.length">
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
              <tr v-if="registration.registrationType !== 'TRUST_MARK_SUBORDINATE' && registration.isHosted !== undefined && registration.isHosted !== null">
                <td class="font-weight-bold field-label">Hosted</td>
                <td>{{ registration.isHosted ? 'Yes' : 'No' }}</td>
              </tr>
              <tr v-if="registration.registrationType !== 'TRUST_MARK_SUBORDINATE' && registration.hostedId">
                <td class="font-weight-bold field-label">Hosted ID</td>
                <td class="text-mono">{{ registration.hostedId }}</td>
              </tr>
            </tbody>
          </v-table>
        </v-card-text>
      </v-card>

      <!-- Pipeline graph -->
      <v-card class="mb-4">
        <v-card-title>Pipeline</v-card-title>
        <v-card-text v-if="registration.steps && registration.steps.length">
          <div class="pipeline">
            <div
                v-for="(step, i) in registration.steps"
                :key="i"
                class="pipeline-step"
            >
              <!-- connector line -->
              <div v-if="i > 0" class="pipeline-connector"></div>

              <div class="d-flex align-start gap-3">
                <!-- status icon -->
                <div class="pipeline-icon-wrap">
                  <v-icon :color="stepColor(step.status)" size="28">
                    {{ stepIcon(step.status) }}
                  </v-icon>
                </div>

                <div class="flex-grow-1">
                  <div class="d-flex align-center gap-2 mb-1">
                    <span class="text-body-1 font-weight-medium">{{ step.stepName }}</span>
                    <v-chip :color="stepColor(step.status)" size="x-small" label>
                      {{ step.status }}
                    </v-chip>
                    <v-btn
                        v-if="step.status === 'PENDING_APPROVAL'"
                        :id="`btn-approve-step-${i}`"
                        color="success"
                        size="x-small"
                        variant="tonal"
                        :loading="approvingStepIndex === i"
                        :disabled="approvingStepIndex !== null"
                        @click="approveStep(i)"
                    >
                      Approve
                    </v-btn>
                    <v-btn
                        :id="`btn-diff-step-${i}`"
                        size="x-small"
                        variant="tonal"
                        @click="openStepInfo(step)"
                    >Diff</v-btn>
                  </div>
                  <div v-if="step.message" class="text-body-2 text-medium-emphasis mb-1">
                    {{ step.message }}
                  </div>
                  <div v-if="step.issues && step.issues.length" class="mt-1">
                    <div
                        v-for="(issue, j) in step.issues"
                        :key="j"
                        class="d-flex align-start gap-1 mb-1"
                    >
                      <v-icon :color="issueColor(issue.severity)" size="14" class="mt-1">
                        {{ issueIcon(issue.severity) }}
                      </v-icon>
                      <span class="text-caption">
                        <strong v-if="issue.field">{{ issue.field }}:</strong>
                        {{ issue.message }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </v-card-text>
        <v-card-text v-else>
          <p class="text-grey text-center py-4">No pipeline data available.</p>
        </v-card-text>
      </v-card>

      <!-- Tabs -->
      <v-card v-if="registration.registrationType !== 'TRUST_MARK_SUBORDINATE'">
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

    <!-- Step Diff Dialog -->
    <v-dialog v-model="stepInfoDialog" max-width="1000" scrollable>
      <v-card v-if="stepInfoTarget">
        <v-card-title class="d-flex align-center gap-2">
          {{ stepInfoTarget.stepName }}
          <v-chip :color="stepColor(stepInfoTarget.status)" size="x-small" label>
            {{ stepInfoTarget.status }}
          </v-chip>
        </v-card-title>
        <v-card-text>
          <div v-if="stepInfoTarget.message" class="text-body-2 text-medium-emphasis mb-4">
            {{ stepInfoTarget.message }}
          </div>

          <div v-if="stepInfoTarget.contextDiff && stepInfoTarget.contextDiff.length">
            <div class="text-subtitle-2 mb-3">Context changes</div>
            <div
                v-for="entry in stepInfoTarget.contextDiff"
                :key="entry.key"
                class="diff-entry mb-4"
            >
              <div class="d-flex align-center gap-2 mb-2">
                <v-chip :color="diffColor(entry.changeType)" size="x-small" label>
                  {{ entry.changeType }}
                </v-chip>
                <span class="text-mono text-body-2 font-weight-medium">{{ entry.key }}</span>
              </div>
              <div v-if="entry.changeType !== 'ADDED'" class="mb-2">
                <div class="text-caption text-medium-emphasis mb-1">Before</div>
                <pre class="diff-value-block diff-before">{{ formatDiffValue(entry.before) }}</pre>
              </div>
              <div v-if="entry.changeType !== 'REMOVED'">
                <div class="text-caption text-medium-emphasis mb-1">After</div>
                <pre class="diff-value-block diff-after">{{ formatDiffValue(entry.after) }}</pre>
              </div>
            </div>
          </div>
          <p v-else class="text-grey text-body-2">No context changes recorded for this step.</p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" variant="text" @click="stepInfoDialog = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
  registrationAdminApproveStepPath,
  registrationAdminItemPath,
  registrationAdminRejectPath,
  trustmarksPath,
} from '@/config/path';

const route = useRoute();
const router = useRouter();
const errorStore = useErrorStore();

const {requestGet, loading} = useRequest();
const {requestPost, loading: rejecting, ok} = useRequest();
const {requestPost: requestApprove, ok: approveOk} = useRequest();
const approvingStepIndex = ref(null);

const registration = ref(null);
const activeTab = ref('entityStatement');

const rejectDialog = ref(false);
const rejectionReason = ref('');
const reasonError = ref(false);

const stepInfoDialog = ref(false);
const stepInfoTarget = ref(null);

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

function stepColor(status) {
  const map = {SUCCESS: 'success', SKIPPED: 'grey', FAILURE: 'error', WARNING: 'warning', PENDING_APPROVAL: 'orange'};
  return map[status] ?? 'grey';
}

function stepIcon(status) {
  const map = {SUCCESS: 'mdi-check-circle', SKIPPED: 'mdi-skip-next-circle-outline', FAILURE: 'mdi-close-circle', WARNING: 'mdi-alert-circle', PENDING_APPROVAL: 'mdi-clock-outline'};
  return map[status] ?? 'mdi-circle-outline';
}

function issueColor(severity) {
  const map = {ERROR: 'error', WARNING: 'warning', INFO: 'info'};
  return map[severity] ?? 'grey';
}

function issueIcon(severity) {
  const map = {ERROR: 'mdi-alert-circle', WARNING: 'mdi-alert', INFO: 'mdi-information'};
  return map[severity] ?? 'mdi-circle-small';
}

async function approveStep(stepIndex) {
  approvingStepIndex.value = stepIndex;
  errorStore.clearError();
  try {
    await requestApprove(registrationAdminApproveStepPath(route.params.id, stepIndex), {});
    if (approveOk.value) {
      await loadRegistration();
    }
  } finally {
    approvingStepIndex.value = null;
  }
}

function openStepInfo(step) {
  stepInfoTarget.value = step;
  stepInfoDialog.value = true;
}

function diffColor(changeType) {
  const map = {ADDED: 'success', CHANGED: 'warning', REMOVED: 'error'};
  return map[changeType] ?? 'grey';
}

function formatDiffValue(value) {
  if (value == null) return '—';

  const looksLikeJson = typeof value === 'string'
    && (value.trimStart().startsWith('{') || value.trimStart().startsWith('['));

  if (!looksLikeJson) return value;

  // Try to parse as-is first (complete JSON)
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    // Value may be truncated — strip the trailing ellipsis and try again
    const trimmed = value.endsWith('…') ? value.slice(0, -1) : value;
    try {
      return JSON.stringify(JSON.parse(trimmed), null, 2) + '\n… (truncated)';
    } catch {
      // Still not valid JSON; return the raw string so at least something is shown
      return value;
    }
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

.pipeline {
  position: relative;
}

.pipeline-step {
  position: relative;
}

.pipeline-connector {
  width: 2px;
  height: 16px;
  background-color: #e0e0e0;
  margin-left: 13px;
  margin-bottom: 4px;
}

.pipeline-icon-wrap {
  flex-shrink: 0;
  width: 28px;
  margin-right: 4px;
}

.gap-3 {
  gap: 12px;
}

.gap-1 {
  gap: 4px;
}

.gap-2 {
  gap: 8px;
}

.diff-entry {
  border-left: 3px solid #e0e0e0;
  padding-left: 12px;
}

.diff-value-block {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 10px 12px;
  font-family: monospace;
  font-size: 0.82rem;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 320px;
  overflow: auto;
  margin: 0;
}

.diff-before {
  border-left: 3px solid #ef5350;
}

.diff-after {
  border-left: 3px solid #66bb6a;
}
</style>
