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
      <h2>Edit Federation Entity</h2>
      <v-btn
          id="btn-back"
          color="grey"
          @click="router.push('/')"
      >
        Back
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
          <p class="mt-4 text-grey">Loading entity...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <v-form ref="form" @submit.prevent="saveEntity">
          <div class="d-flex align-start gap-2 mb-4">
            <v-text-field
                v-model="entityIdentifier"
                label="Entity Identifier"
                :rules="[rules.required]"
                :disabled="saving"
                required
                hint="The entity identifier (entity ID)"
                persistent-hint
                class="flex-grow-1"
            ></v-text-field>
            <EntityConfigurationViewer
                v-if="entityIdentifier"
                :entity-id="entityIdentifier"
                class="mt-1"
            />
          </div>

          <ListField
              v-model="crit"
              label="Crit"
              hint="Crit list"
              :disabled="saving"
          />

          <v-card-actions class="px-0">
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
                id="btn-save-entity"
                color="primary"
                type="submit"
                :loading="saving"
                :disabled="saving"
            >
              Save Entity
            </v-btn>
          </v-card-actions>
        </v-form>

        <v-divider class="my-6"></v-divider>

        <h3 class="mb-4">Module Configuration</h3>
        <p class="text-caption mb-4">You can only have one module from Group 1 (Trustanchor or Intermediate) and one
          from Group 2 (Resolver or Trustmark Issuer).</p>

        <v-expansion-panels v-model="expandedPanels" multiple>
          <!-- Trustanchor Panel -->
          <v-expansion-panel value="trustanchor">
            <v-expansion-panel-title>
              <div class="d-flex align-center">
                <span class="font-weight-medium">Trustanchor</span>
                <v-chip
                    v-if="modules.trustanchor.id"
                    :color="modules.trustanchor.active ? 'success' : 'warning'"
                    size="small"
                    class="ml-2"
                >
                  {{ modules.trustanchor.active ? 'Active' : 'Inactive' }}
                </v-chip>
                <v-chip
                    v-else
                    color="grey"
                    size="small"
                    class="ml-2"
                >
                  Not configured
                </v-chip>
              </div>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <v-form ref="trustanchorForm">
                <v-switch
                    v-model="modules.trustanchor.active"
                    label="Active"
                    :disabled="savingModule"
                    class="mb-4"
                ></v-switch>
                <v-combobox
                    v-model="modules.trustanchor.trustMarkIssuers"
                    label="Trust Mark Issuers"
                    multiple
                    chips
                    closable-chips
                    :disabled="savingModule"
                    hint="Trust Mark Issuers"
                    persistent-hint
                    class="mb-4"
                ></v-combobox>
                <v-card-actions class="px-0">
                  <v-spacer></v-spacer>
                  <v-btn
                      v-if="modules.trustanchor.id"
                      id="btn-delete-trustanchor"
                      color="error"
                      variant="text"
                      @click="deleteModule('trustanchor')"
                      :loading="deletingModule === 'trustanchor'"
                      :disabled="savingModule || deletingModule"
                      class="mr-2"
                  >
                    Delete
                  </v-btn>
                  <v-btn
                      id="btn-save-trustanchor"
                      color="primary"
                      @click="saveModule('trustanchor')"
                      :loading="savingModule === 'trustanchor'"
                      :disabled="savingModule || deletingModule"
                  >
                    Save
                  </v-btn>
                </v-card-actions>
              </v-form>
            </v-expansion-panel-text>
          </v-expansion-panel>

          <!-- Intermediate Panel -->
          <v-expansion-panel value="intermediate">
            <v-expansion-panel-title>
              <div class="d-flex align-center">
                <span class="font-weight-medium">Intermediate</span>
                <v-chip
                    v-if="modules.intermediate.id"
                    :color="modules.intermediate.active ? 'success' : 'warning'"
                    size="small"
                    class="ml-2"
                >
                  {{ modules.intermediate.active ? 'Active' : 'Inactive' }}
                </v-chip>
                <v-chip
                    v-else
                    color="grey"
                    size="small"
                    class="ml-2"
                >
                  Not configured
                </v-chip>
              </div>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <v-form ref="intermediateForm">
                <v-switch
                    v-model="modules.intermediate.active"
                    label="Active"
                    :disabled="savingModule"
                    class="mb-4"
                ></v-switch>

                <template v-if="modules.intermediate.id && modules.intermediate.active">
                  <v-divider class="mb-4"></v-divider>
                  <div class="text-subtitle-2 mb-2">Registration Flows</div>

                  <div class="d-flex align-center gap-2 mb-3">
                    <v-select
                        v-model="selectedFlowToAdd"
                        :items="availableFlowsForSelect"
                        item-title="name"
                        item-value="flowId"
                        label="Add registration flow"
                        :disabled="addingFlow || availableFlowsForSelect.length === 0"
                        clearable
                        return-object
                        hide-details
                        class="flex-grow-1"
                    ></v-select>
                    <v-btn
                        id="btn-assign-flow"
                        color="primary"
                        :disabled="!selectedFlowToAdd || addingFlow"
                        :loading="addingFlow"
                        @click="assignFlow"
                    >
                      Add
                    </v-btn>
                  </div>

                  <v-list v-if="assignedFlows.length > 0" density="compact" class="mb-2">
                    <v-list-item
                        v-for="assignment in assignedFlows"
                        :key="assignment.assignId"
                        :title="assignment.name"
                        :subtitle="assignment.description"
                    >
                      <template #append>
                        <v-btn
                            :id="`btn-unassign-flow-${assignment.assignId}`"
                            icon
                            size="small"
                            color="error"
                            variant="text"
                            :loading="removingAssignId === assignment.assignId"
                            :disabled="removingAssignId !== null"
                            @click="unassignFlow(assignment.assignId)"
                        >
                          <v-icon>mdi-close</v-icon>
                        </v-btn>
                      </template>
                    </v-list-item>
                  </v-list>
                  <div v-else class="text-body-2 text-medium-emphasis mb-3">No flows assigned.</div>
                </template>

                <v-card-actions class="px-0">
                  <v-spacer></v-spacer>
                  <v-btn
                      v-if="modules.intermediate.id"
                      id="btn-delete-intermediate"
                      color="error"
                      variant="text"
                      @click="deleteModule('intermediate')"
                      :loading="deletingModule === 'intermediate'"
                      :disabled="savingModule || deletingModule"
                      class="mr-2"
                  >
                    Delete
                  </v-btn>
                  <v-btn
                      id="btn-save-intermediate"
                      color="primary"
                      @click="saveModule('intermediate')"
                      :loading="savingModule === 'intermediate'"
                      :disabled="savingModule || deletingModule"
                  >
                    Save
                  </v-btn>
                </v-card-actions>
              </v-form>
            </v-expansion-panel-text>
          </v-expansion-panel>

          <!-- Resolver Panel -->
          <v-expansion-panel value="resolver">
            <v-expansion-panel-title>
              <div class="d-flex align-center">
                <span class="font-weight-medium">Resolver</span>
                <v-chip
                    v-if="modules.resolver.id"
                    :color="modules.resolver.active ? 'success' : 'warning'"
                    size="small"
                    class="ml-2"
                >
                  {{ modules.resolver.active ? 'Active' : 'Inactive' }}
                </v-chip>
                <v-chip
                    v-else
                    color="grey"
                    size="small"
                    class="ml-2"
                >
                  Not configured
                </v-chip>
              </div>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <v-form ref="resolverForm">
                <v-switch
                    v-model="modules.resolver.active"
                    label="Active"
                    :disabled="savingModule"
                    class="mb-4"
                ></v-switch>
                <v-text-field
                    v-model="modules.resolver.resolveResponseDuration"
                    label="Resolve Response Duration"
                    :disabled="savingModule"
                    hint="Duration of the response (e.g., PT1H)"
                    persistent-hint
                    class="mb-4"
                ></v-text-field>
                <v-text-field
                    v-model="modules.resolver.trustAnchor"
                    label="Trust Anchor URL"
                    :disabled="savingModule"
                    hint="URL to trustanchor that will be used to build trust chain"
                    persistent-hint
                    class="mb-4"
                ></v-text-field>
                <v-textarea
                    v-model="modules.resolver.trustedKeys"
                    label="Trusted Keys (JWKS)"
                    :disabled="savingModule"
                    :rows="5"
                    auto-grow
                    hint="Trusted keys in JWKS format"
                    persistent-hint
                    class="mb-4"
                    style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
                ></v-textarea>
                <div v-if="resolverJwksLoadedFrom" class="text-body-2 text-medium-emphasis mb-2">
                  JWKS was loaded from url: {{ resolverJwksLoadedFrom }}
                </div>
                <v-btn
                    id="btn-load-resolver-jwks"
                    color="secondary"
                    variant="outlined"
                    :disabled="!modules.resolver.trustAnchor || !modules.resolver.trustAnchor.trim() || loadingResolverJwks || savingModule"
                    :loading="loadingResolverJwks"
                    @click="loadResolverJwks"
                    class="mb-4"
                >
                  Load JWKS
                </v-btn>
                <v-text-field
                    v-model="modules.resolver.stepRetryDuration"
                    label="Step Retry Duration"
                    :disabled="savingModule"
                    hint="Time between a failed step and retry (e.g., PT1M)"
                    persistent-hint
                    class="mb-4"
                ></v-text-field>
                <v-text-field
                    v-model.number="modules.resolver.stepCachedValueThreshold"
                    label="Step Cached Value Threshold"
                    type="number"
                    :disabled="savingModule"
                    hint="Step cached value threshold (integer)"
                    persistent-hint
                    class="mb-4"
                ></v-text-field>
                <v-card-actions class="px-0">
                  <v-spacer></v-spacer>
                  <v-btn
                      v-if="modules.resolver.id"
                      id="btn-delete-resolver"
                      color="error"
                      variant="text"
                      @click="deleteModule('resolver')"
                      :loading="deletingModule === 'resolver'"
                      :disabled="savingModule || deletingModule"
                      class="mr-2"
                  >
                    Delete
                  </v-btn>
                  <v-btn
                      id="btn-save-resolver"
                      color="primary"
                      @click="saveModule('resolver')"
                      :loading="savingModule === 'resolver'"
                      :disabled="savingModule || deletingModule"
                  >
                    Save
                  </v-btn>
                </v-card-actions>
              </v-form>
            </v-expansion-panel-text>
          </v-expansion-panel>

          <!-- Trustmark Issuer Panel -->
          <v-expansion-panel value="trustmarkissuer">
            <v-expansion-panel-title>
              <div class="d-flex align-center">
                <span class="font-weight-medium">Trustmark Issuer</span>
                <v-chip
                    v-if="modules.trustmarkissuer.id"
                    :color="modules.trustmarkissuer.active ? 'success' : 'warning'"
                    size="small"
                    class="ml-2"
                >
                  {{ modules.trustmarkissuer.active ? 'Active' : 'Inactive' }}
                </v-chip>
                <v-chip
                    v-else
                    color="grey"
                    size="small"
                    class="ml-2"
                >
                  Not configured
                </v-chip>
              </div>
            </v-expansion-panel-title>
            <v-expansion-panel-text>
              <v-form ref="trustmarkissuerForm">
                <v-switch
                    v-model="modules.trustmarkissuer.active"
                    label="Active"
                    :disabled="savingModule"
                    class="mb-4"
                ></v-switch>
                <v-text-field
                    v-model="modules.trustmarkissuer.trustMarkTokenValidityDuration"
                    label="Trust Mark Token Validity Duration"
                    :disabled="savingModule"
                    hint="Validity for the token representing the trustmark (e.g., PT1H)"
                    persistent-hint
                    class="mb-4"
                ></v-text-field>
                <v-card-actions class="px-0">
                  <v-spacer></v-spacer>
                  <v-btn
                      v-if="modules.trustmarkissuer.id"
                      id="btn-delete-trustmarkissuer"
                      color="error"
                      variant="text"
                      @click="deleteModule('trustmarkissuer')"
                      :loading="deletingModule === 'trustmarkissuer'"
                      :disabled="savingModule || deletingModule"
                      class="mr-2"
                  >
                    Delete
                  </v-btn>
                  <v-btn
                      id="btn-save-trustmarkissuer"
                      color="primary"
                      @click="saveModule('trustmarkissuer')"
                      :loading="savingModule === 'trustmarkissuer'"
                      :disabled="savingModule || deletingModule"
                  >
                    Save
                  </v-btn>
                </v-card-actions>
              </v-form>
            </v-expansion-panel-text>
          </v-expansion-panel>
        </v-expansion-panels>
      </v-card-text>
    </v-card>

    <!-- Resolver JWKS Picker Dialog -->
    <v-dialog v-model="resolverJwksPickerDialog" max-width="640" scrollable>
      <v-card>
        <v-card-title>Select Entity</v-card-title>
        <v-card-text>
          <v-list lines="two">
            <v-list-item
                v-for="item in resolverJwksPickerItems"
                :key="item.entityId"
                :title="item.entityId"
                :subtitle="item.ecLocation"
                style="cursor: pointer"
                @click="applyResolverJwks(item)"
            ></v-list-item>
          </v-list>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="resolverJwksPickerDialog = false">Cancel</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete the {{ deleteModuleType }} module? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-delete-module-cancel"
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deletingModule"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-delete-module-confirm"
              color="error"
              @click="confirmDeleteModule"
              :loading="deletingModule"
              :disabled="deletingModule"
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
import {useLoadJwks} from '@/api/composables/jwks';
import ListField from '@/components/ListField.vue';
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import {
  federationEntityPath,
  intermediateFlowAssignmentsPath,
  intermediateFlowAssignPath,
  intermediateFlowUnassignPath,
  intermediateModulePath,
  registrationFlowsPath,
  resolverModulePath,
  trustAnchorModulePath,
  trustmarkIssuerModulePath,
} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPut, requestPost, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();
const {loadJwks: loadJwksFromApi, loading: loadingResolverJwks} = useLoadJwks();

const form = ref(null);
const saving = ref(false);
const entityId = ref(null);
const entityIdentifier = ref('');
const crit = ref([]);
const expandedPanels = ref([]);
const savingModule = ref(null);
const deletingModule = ref(null);
const deleteDialog = ref(false);
const moduleToDelete = ref(null);

const availableFlows = ref([]);
const assignedFlows = ref([]);
const selectedFlowToAdd = ref(null);
const addingFlow = ref(false);
const removingAssignId = ref(null);

const resolverJwksLoadedFrom = ref('');
const resolverJwksPickerDialog = ref(false);
const resolverJwksPickerItems = ref([]);

const availableFlowsForSelect = computed(() =>
    availableFlows.value.filter(f => !assignedFlows.value.some(a => a.flowId === f.flowId))
);

const deleteModuleType = computed(() => {
  if (!moduleToDelete.value) return '';
  const types = {
    trustanchor: 'Trustanchor',
    intermediate: 'Intermediate',
    resolver: 'Resolver',
    trustmarkissuer: 'Trustmark Issuer',
  };
  return types[moduleToDelete.value] || moduleToDelete.value;
});

// Module state - each module has its own object
const modules = ref({
  trustanchor: {
    id: null,
    active: true,
    trustMarkIssuers: [],
  },
  intermediate: {
    id: null,
    active: true,
  },
  resolver: {
    id: null,
    active: true,
    resolveResponseDuration: 'PT1H',
    trustAnchor: '',
    trustedKeys: '',
    stepRetryDuration: 'PT1M',
    stepCachedValueThreshold: null,
  },
  trustmarkissuer: {
    id: null,
    active: true,
    trustMarkTokenValidityDuration: 'PT1H',
  },
});

const rules = {
  required: (value) => !!value || 'This field is required.',
};

function applyResolverJwks(item) {
  modules.value.resolver.trustedKeys = item.jwks ? JSON.stringify(item.jwks, null, 2) : '';
  resolverJwksLoadedFrom.value = item.ecLocation || '';
  resolverJwksPickerDialog.value = false;
}

async function loadResolverJwks() {
  const result = await loadJwksFromApi(modules.value.resolver.trustAnchor);
  if (!result || !Array.isArray(result) || result.length === 0) return;
  if (result.length === 1) {
    applyResolverJwks(result[0]);
  } else {
    resolverJwksPickerItems.value = result;
    resolverJwksPickerDialog.value = true;
  }
}

async function loadEntity() {
  errorStore.clearError();
  entityId.value = route.params.id;
  const response = await requestGet(`${federationEntityPath(entityId.value)}?includemodules=true`);
  if (response) {
    // Response can be FederationEntityWithModules or FederationEntity
    const entity = response.federationEntity || response;
    entityIdentifier.value = entity.entityIdentifier || '';
    crit.value = entity.crit || [];

    // Load existing modules
    if (response.trustAnchor) {
      modules.value.trustanchor = {
        id: response.trustAnchor.trustAnchorId || response.trustAnchor.id,
        active: response.trustAnchor.active !== false,
        trustMarkIssuers: response.trustAnchor.trustMarkIssuers || [],
      };
    }

    if (response.intermediate) {
      const taImId = response.intermediate.intermediateId || response.intermediate.id;
      modules.value.intermediate = {
        id: taImId,
        active: response.intermediate.active !== false,
      };
      if (modules.value.intermediate.active) {
        await loadFlowData(taImId);
      }
    }

    if (response.resolver) {
      modules.value.resolver = {
        id: response.resolver.resolverId || response.resolver.id,
        active: response.resolver.active !== false,
        resolveResponseDuration: response.resolver.resolveResponseDuration || 'PT1H',
        trustAnchor: response.resolver.trustAnchor || '',
        trustedKeys: response.resolver.trustedKeys ? JSON.stringify(response.resolver.trustedKeys, null, 2) : '',
        stepRetryDuration: response.resolver.stepRetryDuration || 'PT1M',
        stepCachedValueThreshold: response.resolver.stepCachedValueThreshold ?? null,
      };
    }

    if (response.trustmarkIssuer) {
      modules.value.trustmarkissuer = {
        id: response.trustmarkIssuer.trustmarkIssuerId || response.trustmarkIssuer.id,
        active: response.trustmarkIssuer.active !== false,
        trustMarkTokenValidityDuration: response.trustmarkIssuer.trustMarkTokenValidityDuration || 'PT1H',
      };
    }
  }
}

async function loadFlowData(taImId) {
  const [flows, assignments] = await Promise.all([
    requestGet(registrationFlowsPath),
    requestGet(intermediateFlowAssignmentsPath(taImId)),
  ]);
  availableFlows.value = Array.isArray(flows) ? flows : [];
  assignedFlows.value = Array.isArray(assignments) ? assignments : [];
}

async function assignFlow() {
  if (!selectedFlowToAdd.value) return;
  addingFlow.value = true;
  try {
    const result = await requestPost(intermediateFlowAssignPath(modules.value.intermediate.id), {
      flowId: selectedFlowToAdd.value.flowId,
    });
    if (ok.value && result) {
      assignedFlows.value.push({
        assignId: result.assignId,
        flowId: selectedFlowToAdd.value.flowId,
        name: selectedFlowToAdd.value.name,
        description: selectedFlowToAdd.value.description,
      });
      selectedFlowToAdd.value = null;
    }
  } finally {
    addingFlow.value = false;
  }
}

async function unassignFlow(assignId) {
  removingAssignId.value = assignId;
  try {
    await requestDelete(intermediateFlowUnassignPath(modules.value.intermediate.id, assignId));
    if (ok.value) {
      assignedFlows.value = assignedFlows.value.filter(a => a.assignId !== assignId);
    }
  } finally {
    removingAssignId.value = null;
  }
}

async function saveEntity() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const entityData = {
      entityIdentifier: entityIdentifier.value,
      crit: crit.value.filter(c => c && c.trim() !== ''),
    };

    await requestPut(federationEntityPath(entityId.value), entityData);

    if (ok.value) {
      // Reload to get updated data
      await loadEntity();
    }
  } catch (error) {
    console.error('Error updating federation entity:', error);
  } finally {
    saving.value = false;
  }
}

async function saveModule(moduleType) {
  // Validate that only one module from each group is selected
  if (moduleType === 'trustanchor' && modules.value.intermediate.id) {
    errorStore.setError('Cannot add Trustanchor when Intermediate already exists. Please delete Intermediate first.');
    return;
  }
  if (moduleType === 'intermediate' && modules.value.trustanchor.id) {
    errorStore.setError('Cannot add Intermediate when Trustanchor already exists. Please delete Trustanchor first.');
    return;
  }
  if (moduleType === 'resolver' && modules.value.trustmarkissuer.id) {
    errorStore.setError('Cannot add Resolver when Trustmark Issuer already exists. Please delete Trustmark Issuer first.');
    return;
  }
  if (moduleType === 'trustmarkissuer' && modules.value.resolver.id) {
    errorStore.setError('Cannot add Trustmark Issuer when Resolver already exists. Please delete Resolver first.');
    return;
  }

  savingModule.value = moduleType;
  errorStore.clearError();

  try {
    const module = modules.value[moduleType];
    let moduleData;
    let endpoint;

    switch (moduleType) {
      case 'trustanchor':
        moduleData = {
          entityId: entityId.value,
          active: module.active,
          trustMarkIssuers: module.trustMarkIssuers.filter(s => s && s.trim() !== ''),
        };
        if (module.id) {
          endpoint = trustAnchorModulePath(module.id);
          await requestPut(endpoint, moduleData);
        } else {
          const moduleId = crypto.randomUUID();
          endpoint = trustAnchorModulePath(moduleId);
          await requestPost(endpoint, moduleData);
          if (ok.value) {
            module.id = moduleId;
          }
        }
        break;

      case 'intermediate':
        moduleData = {
          entityId: entityId.value,
          active: module.active,
        };
        if (module.id) {
          endpoint = intermediateModulePath(module.id);
          await requestPut(endpoint, moduleData);
        } else {
          const moduleId = crypto.randomUUID();
          endpoint = intermediateModulePath(moduleId);
          await requestPost(endpoint, moduleData);
          if (ok.value) {
            module.id = moduleId;
          }
        }
        break;

      case 'resolver':
        moduleData = {
          entityId: entityId.value,
          active: module.active,
          resolveResponseDuration: module.resolveResponseDuration,
          trustAnchor: module.trustAnchor,
          trustedKeys: module.trustedKeys && module.trustedKeys.trim() ? JSON.parse(module.trustedKeys) : null,
          stepRetryDuration: module.stepRetryDuration,
          stepCachedValueThreshold: module.stepCachedValueThreshold ?? null,
        };
        if (module.id) {
          endpoint = resolverModulePath(module.id);
          await requestPut(endpoint, moduleData);
        } else {
          const moduleId = crypto.randomUUID();
          endpoint = resolverModulePath(moduleId);
          await requestPost(endpoint, moduleData);
          if (ok.value) {
            module.id = moduleId;
          }
        }
        break;

      case 'trustmarkissuer':
        moduleData = {
          entityId: entityId.value,
          active: module.active,
          trustMarkTokenValidityDuration: module.trustMarkTokenValidityDuration,
        };
        if (module.id) {
          endpoint = trustmarkIssuerModulePath(module.id);
          await requestPut(endpoint, moduleData);
        } else {
          const moduleId = crypto.randomUUID();
          endpoint = trustmarkIssuerModulePath(moduleId);
          await requestPost(endpoint, moduleData);
          if (ok.value) {
            module.id = moduleId;
          }
        }
        break;
    }

    if (ok.value) {
      // Reload to get updated data
      await loadEntity();
    }
  } catch (error) {
    console.error(`Error saving ${moduleType} module:`, error);
  } finally {
    savingModule.value = null;
  }
}

function deleteModule(moduleType) {
  moduleToDelete.value = moduleType;
  deleteDialog.value = true;
}

async function confirmDeleteModule() {
  if (!moduleToDelete.value) return;

  const moduleType = moduleToDelete.value;
  const module = modules.value[moduleType];

  if (!module.id) {
    deleteDialog.value = false;
    moduleToDelete.value = null;
    return;
  }

  deletingModule.value = moduleType;
  errorStore.clearError();

  try {
    let endpoint;
    switch (moduleType) {
      case 'trustanchor':
        endpoint = trustAnchorModulePath(module.id);
        break;
      case 'intermediate':
        endpoint = intermediateModulePath(module.id);
        break;
      case 'resolver':
        endpoint = resolverModulePath(module.id);
        break;
      case 'trustmarkissuer':
        endpoint = trustmarkIssuerModulePath(module.id);
        break;
    }

    await requestDelete(endpoint);

    if (ok.value) {
      // Reset module data
      switch (moduleType) {
        case 'trustanchor':
          modules.value.trustanchor = {id: null, active: true, trustMarkIssuers: []};
          break;
        case 'intermediate':
          modules.value.intermediate = {id: null, active: true};
          break;
        case 'resolver':
          modules.value.resolver = {
            id: null,
            active: true,
            resolveResponseDuration: 'PT1H',
            trustAnchor: '',
            trustedKeys: '',
            stepRetryDuration: 'PT1M',
            stepCachedValueThreshold: null,
          };
          break;
        case 'trustmarkissuer':
          modules.value.trustmarkissuer = {
            id: null,
            active: true,
            trustMarkTokenValidityDuration: 'PT1H',
          };
          break;
      }
      deleteDialog.value = false;
      moduleToDelete.value = null;
      // Reload to get updated data
      await loadEntity();
    }
  } catch (error) {
    console.error(`Error deleting ${moduleType} module:`, error);
  } finally {
    deletingModule.value = null;
  }
}

function cancel() {
  router.push('/');
}

onMounted(() => {
  loadEntity();
});
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
</style>
