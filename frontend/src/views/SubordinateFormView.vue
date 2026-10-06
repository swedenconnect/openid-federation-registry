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
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
              aria-hidden="true"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading subordinate...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Subordinate' : 'Create Subordinate' }}</h2>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="submitForm">
          <div class="d-flex align-start gap-2 mb-4">
            <v-text-field
                id="entity-identifier"
                v-model="entityIdentifier"
                label="Entity Identifier (Subject)"
                :rules="[rules.required]"
                :disabled="saving"
                required
                hint="Subject entity identifier (required, URL)"
                persistent-hint
                class="flex-grow-1"
            ></v-text-field>
            <EntityConfigurationViewer
                v-if="isEdit && entityIdentifier"
                :entity-id="entityIdentifier"
                class="mt-1"
            />
          </div>

          <v-textarea
              id="subordinate-jwks"
              v-model="jwks"
              label="JWKS (Public Keys)"
              :rules="[rules.required, rules.json]"
              :disabled="saving"
              :rows="5"
              auto-grow
              required
              hint="Public keys in JWKS format (required, JSON)"
              persistent-hint
              class="mb-4"
              style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
          ></v-textarea>
          <div v-if="jwksLoadedFrom" class="text-body-2 text-medium-emphasis mb-4">
            JWKS was loaded from url: {{ jwksLoadedFrom }}
          </div>
          <v-btn
              id="btn-load-jwks"
              color="secondary"
              variant="outlined"
              :disabled="!entityIdentifier || !entityIdentifier.trim() || loadingJwks || saving"
              :loading="loadingJwks"
              @click="loadJwks"
              class="mb-2"
          >
            Load JWKS
          </v-btn>


          <ListField
              v-model="metadataPolicyCrit"
              label="Metadata Policy Crit"
              hint="MetadataPolicyCrit (list)"
              :disabled="saving"
          />

          <v-textarea
              v-model="metadataPolicy"
              label="Metadata Policy"
              :rules="[rules.json]"
              :disabled="saving"
              :rows="5"
              auto-grow
              hint="Metadata policy for this subordinate statement (optional, JSON)"
              persistent-hint
              class="mb-4"
              style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
          ></v-textarea>

          <v-textarea
              v-model="metadata"
              label="Metadata"
              :rules="[rules.json]"
              :disabled="saving"
              :rows="5"
              auto-grow
              hint="Metadata for this subordinate statement (optional, JSON)"
              persistent-hint
              class="mb-4"
              style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
          ></v-textarea>

          <v-card variant="outlined" class="mb-4 pa-4" role="group" aria-labelledby="constraints-title">
            <h3 id="constraints-title" class="text-subtitle-1 mb-1">Constraints</h3>
            <p class="text-caption mb-4">
              Optional constraints on the subordinate statement, applied to the entities below the subordinate.
              Leave empty for no constraints.
            </p>

            <v-text-field
                id="constraints-max-path-length"
                v-model="maxPathLength"
                label="Max Path Length"
                type="number"
                min="0"
                step="1"
                :rules="[rules.nonNegativeInteger]"
                :disabled="saving"
                hint="Maximum number of intermediates below this subordinate (0 or more)"
                persistent-hint
                class="mb-4"
            ></v-text-field>

            <ListField
                id="constraints-permitted"
                v-model="namingPermitted"
                label="Naming Constraints - Permitted"
                hint="Permitted name constraints, e.g. .example.com"
                :disabled="saving"
            />

            <ListField
                id="constraints-excluded"
                v-model="namingExcluded"
                label="Naming Constraints - Excluded"
                hint="Excluded name constraints, e.g. east.example.com"
                :disabled="saving"
            />

            <v-combobox
                id="constraints-allowed-entity-types"
                v-model="allowedEntityTypes"
                :items="entityTypeSuggestions"
                label="Allowed Entity Types"
                multiple
                chips
                closable-chips
                :disabled="saving"
                hint="Entity types that are allowed below this subordinate. Pick from the list or type your own and press Enter."
                persistent-hint
            ></v-combobox>
          </v-card>

          <ListField
              v-model="crit"
              label="Crit"
              hint="Crit (list)"
              :disabled="saving"
          />

          <v-text-field
              v-model="ecLocation"
              label="EC Location"
              :disabled="saving"
              hint="Ec Location, expressed as an url"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-switch
              v-model="ecLocationAutomaticResolve"
              label="EC Location Automatic Resolve"
              :disabled="saving"
              hint="System will try to find hosted entity with same subject name"
              persistent-hint
              class="mb-4"
          ></v-switch>

          <v-text-field
              v-if="effectiveEcLocation"
              :model-value="effectiveEcLocation"
              label="Effective EC Location"
              disabled
              hint="Calculated server-side"
              persistent-hint
              class="mb-4"
          ></v-text-field>

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

    <v-dialog v-model="jwksPickerDialog" max-width="640" scrollable aria-labelledby="jwks-picker-title">
      <v-card>
        <v-card-title id="jwks-picker-title">Select Entity</v-card-title>
        <v-card-text>
          <v-list lines="two">
            <v-list-item
                v-for="item in jwksPickerItems"
                :key="item.entityId"
                :title="item.entityId"
                :subtitle="item.ecLocation"
                tabindex="0"
                style="cursor: pointer"
                @click="applyJwksResult(item)"
                @keydown.enter.prevent="applyJwksResult(item)"
            ></v-list-item>
          </v-list>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn variant="text" @click="jwksPickerDialog = false">Cancel</v-btn>
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
import {subordinatePath, subordinatesPath} from '@/config/path';
import {useUserStore} from '@/stores/userStore';
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import ListField from '@/components/ListField.vue';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();
const {loadJwks: loadJwksFromApi, loading: loadingJwks} = useLoadJwks();

const form = ref(null);
const saving = ref(false);

const subordinateId = ref(null);
const taImIdValue = ref(null);
const entityIdentifier = ref('');
const jwks = ref('');
const metadataPolicyCrit = ref([]);
const crit = ref([]);
const metadataPolicy = ref('');
const metadata = ref('');
const maxPathLength = ref('');
const namingPermitted = ref([]);
const namingExcluded = ref([]);
const allowedEntityTypes = ref([]);
const entityTypeSuggestions = [
  'openid_provider',
  'openid_relying_party',
  'oauth_authorization_server',
  'oauth_client',
  'oauth_resource',
];
const ecLocation = ref('');
const ecLocationAutomaticResolve = ref(false);
const effectiveEcLocation = ref('');
const jwksLoadedFrom = ref('');
const jwksPickerDialog = ref(false);
const jwksPickerItems = ref([]);

const isEdit = computed(() => !!route.params.id);
const entityId = computed(() => route.params.entityId);
const moduleType = computed(() => route.params.moduleType);
const taImId = computed(() => route.query.taImId || null);

const rules = {
  required: (value) => {
    if (typeof value === 'string') {
      return !!value.trim() || 'This field is required.';
    }
    return !!value || 'This field is required.';
  },
  nonNegativeInteger: (value) => {
    if (value === '' || value === null || value === undefined) return true;
    return /^\d+$/.test(String(value)) || 'Must be a whole number, 0 or more.';
  },
  json: (value) => {
    if (!value || !value.trim()) return true;
    try {
      JSON.parse(value);
      return true;
    } catch (e) {
      return 'Invalid JSON format';
    }
  },
};

function applyJwksResult(item) {
  jwks.value = item.jwks ? JSON.stringify(item.jwks, null, 2) : '';
  jwksLoadedFrom.value = item.ecLocation || '';
  jwksPickerDialog.value = false;
}

async function loadJwks() {
  const result = await loadJwksFromApi(entityIdentifier.value);
  if (!result || !Array.isArray(result) || result.length === 0) return;
  if (result.length === 1) {
    applyJwksResult(result[0]);
  } else {
    jwksPickerItems.value = result;
    jwksPickerDialog.value = true;
  }
}

async function loadSubordinate() {
  errorStore.clearError();
  subordinateId.value = route.params.id;

  const response = await requestGet(subordinatePath(userStore.selectedTenant, userStore.orgNumber, subordinateId.value));
  if (response) {
    taImIdValue.value = response.taImId || taImId.value || null;
    entityIdentifier.value = response.entityIdentifier || '';
    jwks.value = response.jwks ? JSON.stringify(response.jwks, null, 2) : '';
    metadataPolicyCrit.value = response.metadataPolicyCrit || [];
    crit.value = response.crit || [];
    metadataPolicy.value = response.metadataPolicy
        ? JSON.stringify(response.metadataPolicy, null, 2)
        : '';
    metadata.value = response.metadata
        ? JSON.stringify(response.metadata, null, 2)
        : '';
    loadConstraints(response.constraints);
    ecLocation.value = response.ecLocation || '';
    ecLocationAutomaticResolve.value = response.ecLocationAutomaticResolve || false;
    effectiveEcLocation.value = response.effectiveEcLocation || '';
  }
}

// The constraints are stored in the format of the OpenID Federation specification, section 6.2.
function loadConstraints(constraints) {
  maxPathLength.value = constraints?.max_path_length ?? '';
  namingPermitted.value = constraints?.naming_constraints?.permitted || [];
  namingExcluded.value = constraints?.naming_constraints?.excluded || [];
  allowedEntityTypes.value = constraints?.allowed_entity_types || [];
}

function cleanList(list) {
  return Array.isArray(list) ? list.map(item => String(item).trim()).filter(item => item !== '') : [];
}

// Only what is set is sent, and null when nothing is set.
function buildConstraints() {
  const constraints = {};
  if (maxPathLength.value !== '' && maxPathLength.value !== null) {
    constraints.max_path_length = Number(maxPathLength.value);
  }
  const naming = {};
  const permitted = cleanList(namingPermitted.value);
  const excluded = cleanList(namingExcluded.value);
  if (permitted.length > 0) naming.permitted = permitted;
  if (excluded.length > 0) naming.excluded = excluded;
  if (Object.keys(naming).length > 0) constraints.naming_constraints = naming;
  const entityTypes = cleanList(allowedEntityTypes.value);
  if (entityTypes.length > 0) constraints.allowed_entity_types = entityTypes;
  return Object.keys(constraints).length > 0 ? constraints : null;
}

async function submitForm() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const subordinateData = {
      taImId: taImIdValue.value || taImId.value || null,
      jwks: jwks.value && jwks.value.trim() ? JSON.parse(jwks.value) : null,
      entityIdentifier: entityIdentifier.value || '',
      crit: Array.isArray(crit.value)
          ? crit.value.filter(c => c && (typeof c === 'string' ? c.trim() !== '' : true))
          : [],
      metadataPolicyCrit: Array.isArray(metadataPolicyCrit.value)
          ? metadataPolicyCrit.value.filter(c => c && (typeof c === 'string' ? c.trim() !== '' : true))
          : [],
      ecLocation: ecLocation.value || null,
      ecLocationAutomaticResolve: ecLocationAutomaticResolve.value || false,
      metadataPolicy: metadataPolicy.value && metadataPolicy.value.trim()
          ? JSON.parse(metadataPolicy.value)
          : null,
      metadata: metadata.value && metadata.value.trim()
          ? JSON.parse(metadata.value)
          : null,
      constraints: buildConstraints(),
    };

    if (isEdit.value) {
      await requestPut(subordinatePath(userStore.selectedTenant, userStore.orgNumber, subordinateId.value), subordinateData);
    } else {
      await requestPost(subordinatesPath(userStore.selectedTenant, userStore.orgNumber), subordinateData);
    }

    if (ok.value) {
      navigateBack();
    }
  } catch (error) {
    console.error('Error saving subordinate:', error);
  } finally {
    saving.value = false;
  }
}

function navigateBack() {
  const params = new URLSearchParams();
  if (taImId.value) params.set('taImId', taImId.value);
  router.push(`/entities/${entityId.value}/modules/${moduleType.value}/subordinates?${params.toString()}`);
}

function cancel() {
  navigateBack();
}

onMounted(() => {
  errorStore.clearError();
  if (isEdit.value) {
    loadSubordinate();
  }
});
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
</style>
