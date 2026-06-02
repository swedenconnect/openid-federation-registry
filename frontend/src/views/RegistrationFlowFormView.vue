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
          <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
          <p class="mt-4 text-grey">Loading registration flow...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Registration Flow' : 'Create Registration Flow' }}</h2>
      </v-card-title>

      <v-card-text>
        <v-form ref="form" @submit.prevent="submitForm">

          <!-- Name & Description -->
          <v-text-field
              v-model="name"
              label="Name"
              :rules="[rules.required]"
              :disabled="saving"
              required
              hint="Display name for this registration flow"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-textarea
              v-model="description"
              label="Description"
              :disabled="saving"
              :rows="3"
              auto-grow
              hint="Human-readable description of this flow's purpose"
              persistent-hint
              class="mb-4"
          ></v-textarea>

          <v-textarea
              v-model="descriptionSv"
              label="Description (Swedish)"
              :disabled="saving"
              :rows="2"
              auto-grow
              hint="Beskrivning av flödet på svenska"
              persistent-hint
              class="mb-4"
          ></v-textarea>

          <v-row class="mb-4">
            <v-col cols="12">
              <v-label class="text-caption text-medium-emphasis mb-1 d-block">Flow Type</v-label>
              <v-btn-toggle
                  v-model="flowType"
                  mandatory
                  :disabled="saving || isEdit"
                  color="primary"
                  variant="outlined"
                  class="mb-1"
              >
                <v-btn value="INTERMEDIATE">Intermediate</v-btn>
                <v-btn value="TRUST_MARK_ISSUER">Trust Mark</v-btn>
              </v-btn-toggle>
              <div class="text-caption text-medium-emphasis mt-1">
                Determines which steps are available and which module type can use this flow.
                Cannot be changed after creation.
              </div>
            </v-col>
          </v-row>

          <v-row v-if="flowType !== 'TRUST_MARK_ISSUER'" class="mb-6">
            <v-col cols="12" sm="6">
              <v-select
                  v-model="technology"
                  :items="technologyOptions"
                  label="Technology"
                  :disabled="saving"
                  clearable
                  :rules="[rules.required]"
                  hint="Protocol technology for entities in this flow"
                  persistent-hint
              ></v-select>
            </v-col>
            <v-col cols="12" sm="6">
              <v-combobox
                  v-model="entityType"
                  :items="entityTypeOptions"
                  label="Entity type"
                  :disabled="saving"
                  clearable
                  hint="Select from list or enter a custom EntityType value"
                  persistent-hint
              ></v-combobox>
            </v-col>
          </v-row>

          <!-- Step selector -->
          <div class="mb-2 text-subtitle-1 font-weight-medium">Pipeline Steps</div>

          <v-row class="mb-4" align="center">
            <v-col cols="12">
              <v-select
                  v-model="stepToAdd"
                  :items="availableStepsForSelect"
                  item-title="name"
                  item-value="stepId"
                  label="Add step"
                  :disabled="saving || availableStepsForSelect.length === 0"
                  clearable
                  return-object
                  hint="Steps already added to the flow are not shown"
                  persistent-hint
              ></v-select>
            </v-col>
          </v-row>

          <!-- Selected steps -->
          <div v-if="selectedSteps.length === 0" class="mb-4">
            <span :class="stepsValidationAttempted ? 'text-error' : 'text-grey'" class="text-body-2">
              No steps selected. Add at least one step above.
            </span>
          </div>

          <v-card
              v-for="(step, index) in selectedSteps"
              :key="step.stepId"
              variant="outlined"
              class="mb-3"
          >
            <v-card-title class="d-flex align-center py-2 px-4">
              <span class="text-body-1 font-weight-medium">
                {{ index + 1 }}. {{ step.name }}
              </span>
              <v-spacer></v-spacer>
              <v-btn
                  :id="'btn-settings-' + step.stepId"
                  icon="mdi-cog"
                  size="small"
                  variant="text"
                  :disabled="!step.config || step.config.length === 0 || saving"
                  @click="openStepConfig(index)"
              ></v-btn>
              <v-btn
                  :id="'btn-move-up-' + step.stepId"
                  icon="mdi-arrow-up"
                  size="small"
                  variant="text"
                  :disabled="index === 0 || saving"
                  @click="moveStep(index, -1)"
              ></v-btn>
              <v-btn
                  :id="'btn-move-down-' + step.stepId"
                  icon="mdi-arrow-down"
                  size="small"
                  variant="text"
                  :disabled="index === selectedSteps.length - 1 || saving"
                  @click="moveStep(index, 1)"
              ></v-btn>
              <v-btn
                  :id="'btn-remove-step-' + step.stepId"
                  icon="mdi-delete"
                  size="small"
                  variant="text"
                  color="error"
                  :disabled="saving"
                  @click="removeStep(index)"
              ></v-btn>
            </v-card-title>

            <v-card-text v-if="step.description" class="text-grey text-body-2 py-1 px-4">
              {{ step.description }}
            </v-card-text>
          </v-card>

          <v-card-actions class="px-0 mt-2">
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

    <!-- Step settings dialog -->
    <v-dialog v-model="configDialog.open" max-width="560" scrollable>
      <v-card v-if="configDialogStep">
        <v-card-title>{{ configDialogStep.name }} Settings</v-card-title>
        <v-card-text>
          <div
              v-for="cfg in configDialogStep.config"
              :key="cfg.key"
              class="mb-4"
          >
            <v-checkbox
                v-if="cfg.type === 'BOOLEAN'"
                v-model="cfg.value"
                :label="cfg.key"
                :hint="cfg.description"
                persistent-hint
                true-value="true"
                false-value="false"
            ></v-checkbox>

            <v-text-field
                v-else
                v-model="cfg.value"
                :label="cfg.key"
                :hint="cfg.description"
                persistent-hint
                :placeholder="String(cfg.defaultValue ?? '')"
            ></v-text-field>
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="primary" @click="configDialog.open = false">Done</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {computed, nextTick, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {registrationFlowPath, registrationFlowCreatePath, registrationFlowStepsPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);
const stepsValidationAttempted = ref(false);
const configDialog = ref({open: false, stepIndex: null});
const name = ref('');
const description = ref('');
const descriptionSv = ref('');
const technology = ref(null);
const entityType = ref(null);
const flowType = ref('INTERMEDIATE');
const availableSteps = ref([]);
const selectedSteps = ref([]);
const stepToAdd = ref(null);

const technologyOptions = ['OIDC', 'SAML'];

const oidcEntityTypes = [
  'openid_relying_party',
  'openid_provider',
  'oauth_authorization_server',
  'oauth_client',
  'oauth_resource',
  'federation_entity',
];

const samlEntityTypes = [
  'saml_identity_provider',
  'saml_service_provider',
];

const entityTypeOptions = computed(() => {
  if (technology.value === 'SAML') return samlEntityTypes;
  if (technology.value === 'OIDC') return oidcEntityTypes;
  return [...oidcEntityTypes, ...samlEntityTypes];
});

watch(technology, () => {
  if (entityType.value && !entityTypeOptions.value.includes(entityType.value)) {
    entityType.value = null;
  }
});

const isEdit = computed(() => !!route.params.id);

const availableStepsForSelect = computed(() =>
    availableSteps.value.filter(
        s => s.flowType === flowType.value && !selectedSteps.value.some(sel => sel.stepId === s.stepId)
    )
);

watch(stepToAdd, (val) => {
  if (val) addStep();
});

watch(flowType, (val) => {
  selectedSteps.value = [];
  stepToAdd.value = null;
  if (val === 'TRUST_MARK_ISSUER') {
    technology.value = null;
    entityType.value = null;
  }
});

const configDialogStep = computed(() =>
    configDialog.value.stepIndex !== null ? selectedSteps.value[configDialog.value.stepIndex] : null
);

function openStepConfig(index) {
  configDialog.value = {open: true, stepIndex: index};
}

const rules = {
  required: (value) => {
    if (typeof value === 'string') return !!value.trim() || 'This field is required.';
    return !!value || 'This field is required.';
  },
};

function addStep() {
  if (!stepToAdd.value) return;
  const step = {
    ...stepToAdd.value,
    config: (stepToAdd.value.config || []).map(c => ({
      ...c,
      value: c.value ?? String(c.defaultValue ?? ''),
    })),
  };
  selectedSteps.value.push(step);
  stepToAdd.value = null;
}

function removeStep(index) {
  selectedSteps.value.splice(index, 1);
}

function moveStep(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= selectedSteps.value.length) return;
  const steps = [...selectedSteps.value];
  [steps[index], steps[target]] = [steps[target], steps[index]];
  selectedSteps.value = steps;
}

async function loadAvailableSteps() {
  const response = await requestGet(registrationFlowStepsPath);
  availableSteps.value = Array.isArray(response) ? response : [];
}

async function loadFlow() {
  errorStore.clearError();
  const response = await requestGet(registrationFlowPath(route.params.id));
  if (response) {
    name.value = response.name || '';
    description.value = response.description || '';
    descriptionSv.value = response.descriptionSv || '';
    technology.value = response.technology || null;
    entityType.value = response.entityType || null;
    flowType.value = response.flowType || 'INTERMEDIATE';
    // Wait for the flowType watcher to fire and clear selectedSteps, then set them.
    await nextTick();
    if (Array.isArray(response.steps) && response.steps.length > 0) {
      selectedSteps.value = response.steps.map(s => ({
        ...s,
        config: (s.config || []).map(c => ({
          ...c,
          value: c.value ?? String(c.defaultValue ?? ''),
        })),
      }));
    }
  }
}

async function submitForm() {
  stepsValidationAttempted.value = true;
  const {valid} = await form.value.validate();
  if (!valid || selectedSteps.value.length === 0) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const payload = {
      name: name.value,
      description: description.value,
      descriptionSv: descriptionSv.value || null,
      technology: technology.value || null,
      entityType: entityType.value || null,
      flowType: flowType.value,
      steps: selectedSteps.value.map(step => ({
        stepId: step.stepId,
        name: step.name,
        description: step.description,
        config: (step.config || []).map(c => ({
          key: c.key,
          value: c.value ?? '',
        })),
      })),
    };

    if (isEdit.value) {
      await requestPut(registrationFlowPath(route.params.id), payload);
    } else {
      await requestPost(registrationFlowCreatePath, payload);
    }

    if (ok.value) {
      router.push({name: 'registration-flows-list'});
    }
  } catch (error) {
    console.error('Error saving registration flow:', error);
  } finally {
    saving.value = false;
  }
}

function cancel() {
  router.push({name: 'registration-flows-list'});
}

onMounted(async () => {
  errorStore.clearError();
  await loadAvailableSteps();
  if (isEdit.value) {
    await loadFlow();
  }
});
</script>