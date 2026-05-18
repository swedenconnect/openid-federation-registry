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
    <!-- Form -->
    <template v-if="!result">
      <div class="d-flex justify-space-between align-center mb-4">
        <h2>Trigger Registration</h2>
      </div>

      <v-card v-if="loadingFlows">
        <v-card-text>
          <div class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
            <p class="mt-4 text-grey">Loading registration flows...</p>
          </div>
        </v-card-text>
      </v-card>

      <v-card v-else>
        <v-card-text>
          <v-form ref="formRef" @submit.prevent="submit">

            <v-select
                v-model="selectedJoinId"
                :items="flows"
                item-title="displayLabel"
                item-value="joinId"
                label="Registration Flow"
                hint="Select the registration flow to apply to"
                persistent-hint
                variant="outlined"
                density="compact"
                class="mb-4"
                :rules="[v => !!v || 'Registration flow is required']"
            >
              <template #item="{ props: itemProps, item }">
                <v-list-item v-bind="itemProps">
                  <template #subtitle>
                    <div v-if="item.raw.description">{{ item.raw.description }}</div>
                  </template>
                </v-list-item>
              </template>
            </v-select>

            <v-text-field
                v-model="entityIdentifier"
                label="Entity Identifier"
                hint="Entity identifier of the applicant (e.g. https://example.com/entity)"
                persistent-hint
                variant="outlined"
                density="compact"
                class="mb-4"
                :rules="[v => !!v || 'Entity identifier is required']"
            ></v-text-field>

            <TrustmarkSourcesField
                v-model="trustmarkSources"
            ></TrustmarkSourcesField>

            <div class="d-flex justify-end mt-4">
              <v-btn
                  variant="text"
                  class="mr-2"
                  @click="cancel"
              >
                Cancel
              </v-btn>
              <v-btn
                  color="primary"
                  type="submit"
                  :loading="submitting"
              >
                Submit
              </v-btn>
            </div>
          </v-form>
        </v-card-text>
      </v-card>
    </template>

    <!-- Result -->
    <template v-else>
      <div class="d-flex justify-space-between align-center mb-4">
        <h2>Registration Result</h2>
      </div>

      <v-card class="mb-4">
        <v-card-title>Summary</v-card-title>
        <v-card-text>
          <v-table density="compact">
            <tbody>
              <tr>
                <td class="font-weight-medium">Registration ID</td>
                <td>{{ result.registrationId }}</td>
              </tr>
              <tr>
                <td class="font-weight-medium">Entity Identifier</td>
                <td>{{ result.entityIdentifier }}</td>
              </tr>
              <tr>
                <td class="font-weight-medium">Status</td>
                <td>
                  <v-chip :color="statusColor(result.status)" size="small" label>
                    {{ statusLabel(result.status) }}
                  </v-chip>
                </td>
              </tr>
              <tr>
                <td class="font-weight-medium">Pipeline</td>
                <td>
                  <v-chip :color="result.successful ? 'success' : 'error'" size="small" label>
                    {{ result.successful ? 'Completed successfully' : 'Completed with failures' }}
                  </v-chip>
                </td>
              </tr>
            </tbody>
          </v-table>

          <div v-if="result.trustmarksRequested && result.trustmarksRequested.length > 0" class="mt-3">
            <div class="text-body-2 font-weight-medium mb-1">Requested Trustmarks</div>
            <v-chip
                v-for="tm in result.trustmarksRequested"
                :key="tm"
                size="small"
                variant="outlined"
                class="mr-1 mb-1"
            >
              {{ tm }}
            </v-chip>
          </div>
        </v-card-text>
      </v-card>

      <v-card v-if="result.steps && result.steps.length > 0" class="mb-4">
        <v-card-title>Pipeline Steps</v-card-title>
        <v-card-text class="pa-0">
          <v-list lines="two">
            <v-list-item
                v-for="(step, index) in result.steps"
                :key="index"
                :class="index > 0 ? 'border-t' : ''"
            >
              <template #title>
                <div class="d-flex align-center">
                  <span class="mr-2">{{ step.stepName }}</span>
                  <v-chip :color="stepColor(step.status)" size="x-small" label>
                    {{ step.status }}
                  </v-chip>
                </div>
              </template>
              <template #subtitle>
                <div v-if="step.message" class="text-caption mt-1">{{ step.message }}</div>
                <div v-if="step.issues && step.issues.length > 0" class="mt-1">
                  <div
                      v-for="(issue, ii) in step.issues"
                      :key="ii"
                      class="d-flex align-center mb-1"
                  >
                    <v-chip :color="issueColor(issue.severity)" size="x-small" label class="mr-2">
                      {{ issue.severity }}
                    </v-chip>
                    <span class="text-caption">
                      <span v-if="issue.field" class="font-weight-medium">{{ issue.field }}: </span>
                      {{ issue.message }}
                    </span>
                  </div>
                </div>
              </template>
            </v-list-item>
          </v-list>
        </v-card-text>
      </v-card>

      <div class="d-flex justify-end">
        <v-btn
            variant="text"
            class="mr-2"
            @click="resetForm"
        >
          New Registration
        </v-btn>
        <v-btn
            color="primary"
            @click="viewRegistration"
        >
          View Registration
        </v-btn>
      </div>
    </template>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {registrationPublicFlowsPath, registrationTriggerPath} from '@/config/path';
import TrustmarkSourcesField from '@/components/TrustmarkSourcesField.vue';

const router = useRouter();
const {requestGet, requestPost, loading: loadingFlows} = useRequest();
const errorStore = useErrorStore();

const formRef = ref(null);
const flows = ref([]);
const selectedJoinId = ref(null);
const entityIdentifier = ref('');
const trustmarkSources = ref([]);
const submitting = ref(false);
const result = ref(null);

function statusColor(status) {
  const colors = {PENDING_APPROVAL: 'warning', APPROVED: 'success', REJECTED: 'error', STARTED: 'info'};
  return colors[status] ?? 'default';
}

function statusLabel(status) {
  const labels = {PENDING_APPROVAL: 'Pending Approval', APPROVED: 'Approved', REJECTED: 'Rejected', STARTED: 'Started'};
  return labels[status] ?? status;
}

function stepColor(status) {
  const colors = {SUCCESS: 'success', WARNING: 'warning', FAILURE: 'error'};
  return colors[status] ?? 'default';
}

function issueColor(severity) {
  const colors = {ERROR: 'error', WARNING: 'warning', INFO: 'info'};
  return colors[severity] ?? 'default';
}

function mapTrustmarks(sources) {
  const byIssuer = new Map();
  for (const s of sources) {
    if (!s.trustMarkIssuer && !s.trustmarkId) continue;
    const issuer = s.trustMarkIssuer || '';
    if (!byIssuer.has(issuer)) byIssuer.set(issuer, []);
    if (s.trustmarkId) byIssuer.get(issuer).push(s.trustmarkId);
  }
  if (byIssuer.size === 0) return null;
  return Array.from(byIssuer.entries()).map(([issuer, types]) => ({
    trustmarkIssuer: issuer || null,
    trustmarkType: types,
  }));
}

async function submit() {
  const {valid} = await formRef.value.validate();
  if (!valid) return;

  submitting.value = true;
  errorStore.clearError();

  try {
    const body = {
      entityIdentifier: entityIdentifier.value,
      trustmarksRequested: mapTrustmarks(trustmarkSources.value),
    };

    const response = await requestPost(registrationTriggerPath(selectedJoinId.value), body);
    if (response) {
      result.value = response;
    }
  } finally {
    submitting.value = false;
  }
}

function cancel() {
  router.push({name: 'registrations-list'});
}

function resetForm() {
  result.value = null;
  selectedJoinId.value = null;
  entityIdentifier.value = '';
  trustmarkSources.value = [];
}

function viewRegistration() {
  router.push({name: 'registration-detail', params: {id: result.value.registrationId}});
}

async function loadFlows() {
  errorStore.clearError();
  const response = await requestGet(registrationPublicFlowsPath);
  flows.value = (Array.isArray(response) ? response : []).map(f => ({
    ...f,
    displayLabel: f.intermediateEntityId ? `${f.name} - ${f.intermediateEntityId}` : f.name,
  }));
}

onMounted(() => {
  loadFlows();
});
</script>
