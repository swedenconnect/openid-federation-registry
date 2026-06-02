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
    <v-card v-if="isEdit && loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading trustmark...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Trustmark' : 'Create Trustmark' }}</h2>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="submitForm">
          <v-text-field
              v-model="trustmarkType"
              label="Trustmark Type"
              :rules="[rules.required]"
              :disabled="saving"
              required
              hint="Type of trustmark (required)"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-text-field
              v-model="logoUri"
              label="Logo URI"
              :disabled="saving"
              hint="URI to logo image"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-text-field
              v-model="refUri"
              label="Reference URI"
              :disabled="saving"
              hint="Reference URI"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-textarea
              v-model="delegation"
              label="Delegation"
              :disabled="saving"
              :rows="4"
              auto-grow
              hint="Delegation information"
              persistent-hint
              class="mb-4"
          ></v-textarea>

          <template v-if="isEdit">
            <v-divider class="mb-4"></v-divider>
            <div class="text-subtitle-2 mb-2">Registration Flow</div>

            <template v-if="flowAssignment">
              <v-list density="compact" class="pa-0 mb-1">
                <v-list-item
                    :title="flowAssignment.name"
                    :subtitle="flowAssignment.description"
                >
                  <template #append>
                    <v-btn
                        id="btn-unassign-tm-flow"
                        icon
                        size="small"
                        color="error"
                        variant="text"
                        :loading="removingAssignId !== null"
                        :disabled="removingAssignId !== null"
                        @click="unassignFlow(flowAssignment.assignId)"
                    >
                      <v-icon>mdi-close</v-icon>
                    </v-btn>
                  </template>
                </v-list-item>
              </v-list>
            </template>
            <div v-else class="d-flex align-center gap-2 mb-3">
              <v-select
                  v-model="selectedFlow"
                  :items="availableFlows"
                  item-title="name"
                  item-value="flowId"
                  label="Assign flow"
                  :disabled="addingFlow || availableFlows.length === 0"
                  clearable
                  return-object
                  hide-details
                  class="flex-grow-1"
              ></v-select>
              <v-btn
                  id="btn-assign-tm-flow"
                  color="primary"
                  :disabled="!selectedFlow || addingFlow"
                  :loading="addingFlow"
                  @click="assignFlow"
              >
                Assign
              </v-btn>
            </div>
          </template>

          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
                id="btn-cancel"
                color="grey"
                variant="text"
                @click="cancel"
                :disabled="saving"
            >
              Cancel
            </v-btn>
            <v-btn
                id="btn-save"
                color="primary"
                type="submit"
                :loading="saving"
                :disabled="saving"
            >
              {{ isEdit ? 'Save' : 'Create' }}
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {
  registrationFlowsPath,
  tmFlowAssignPath,
  tmFlowUnassignPath,
  tmIssuerTrustmarkAssignmentsPath,
  trustmarksPath,
} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);

const trustmarkId = ref(null);
const trustmarkissuerId = ref(null);
const trustmarkType = ref('');
const logoUri = ref('');
const refUri = ref('');
const delegation = ref('');

const flowAssignment = ref(null);
const availableFlows = ref([]);
const selectedFlow = ref(null);
const addingFlow = ref(false);
const removingAssignId = ref(null);

const isEdit = computed(() => !!route.params.id);
const entityId = computed(() => route.params.entityId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

const rules = {
  required: (value) => {
    if (typeof value === 'string') {
      return !!value.trim() || 'This field is required.';
    }
    return !!value || 'This field is required.';
  },
};

async function loadFlowData() {
  if (!trustmarkIssuerId.value || !trustmarkId.value) return;
  const [flows, assignments] = await Promise.all([
    requestGet(registrationFlowsPath),
    requestGet(tmIssuerTrustmarkAssignmentsPath(trustmarkIssuerId.value)),
  ]);
  availableFlows.value = Array.isArray(flows)
      ? flows.filter(f => f.flowType === 'TRUST_MARK_ISSUER') : [];
  const assignment = Array.isArray(assignments)
      ? assignments.find(a => a.trustmarkId === trustmarkId.value) : null;
  flowAssignment.value = assignment || null;
}

async function assignFlow() {
  if (!selectedFlow.value) return;
  addingFlow.value = true;
  try {
    const result = await requestPost(tmFlowAssignPath(trustmarkId.value), {flowId: selectedFlow.value.flowId});
    if (ok.value && result) {
      flowAssignment.value = {
        assignId: result.assignId,
        trustmarkId: trustmarkId.value,
        flowId: selectedFlow.value.flowId,
        name: selectedFlow.value.name,
        description: selectedFlow.value.description,
      };
      selectedFlow.value = null;
    }
  } finally {
    addingFlow.value = false;
  }
}

async function unassignFlow(assignId) {
  removingAssignId.value = assignId;
  try {
    await requestDelete(tmFlowUnassignPath(trustmarkId.value, assignId));
    if (ok.value) {
      flowAssignment.value = null;
    }
  } finally {
    removingAssignId.value = null;
  }
}

async function loadTrustmark() {
  errorStore.clearError();
  trustmarkId.value = route.params.id;

  const response = await requestGet(`${trustmarksPath}/${trustmarkId.value}`);
  if (response) {
    trustmarkissuerId.value = response.trustmarkissuerId || trustmarkIssuerId.value || null;
    trustmarkType.value = response.trustmarkType || '';
    logoUri.value = response.logoUri || '';
    refUri.value = response.refUri || '';
    delegation.value = response.delegation || '';
  }
  await loadFlowData();
}

async function submitForm() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const trustmarkData = {
      trustmarkissuerId: trustmarkissuerId.value || trustmarkIssuerId.value || null,
      trustmarkType: trustmarkType.value || '',
      logoUri: logoUri.value || null,
      refUri: refUri.value || null,
      delegation: delegation.value || null,
    };

    if (isEdit.value) {
      await requestPut(`${trustmarksPath}/${trustmarkId.value}`, trustmarkData);
    } else {
      await requestPost(trustmarksPath, trustmarkData);
    }

    if (ok.value) {
      navigateBack();
    }
  } catch (error) {
    console.error('Error saving trustmark:', error);
  } finally {
    saving.value = false;
  }
}

function navigateBack() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  router.push(`/entities/${entityId.value}/modules/trustmarkissuer/trustmarks?${params.toString()}`);
}

function cancel() {
  navigateBack();
}

onMounted(() => {
  errorStore.clearError();
  if (isEdit.value) {
    loadTrustmark();
  }
});
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
</style>
