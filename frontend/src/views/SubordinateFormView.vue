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
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import ListField from '@/components/ListField.vue';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();
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

  const response = await requestGet(subordinatePath(subordinateId.value));
  if (response) {
    taImIdValue.value = response.taImId || taImId.value || null;
    entityIdentifier.value = response.entityIdentifier || '';
    jwks.value = response.jwks ? JSON.stringify(response.jwks, null, 2) : '';
    metadataPolicyCrit.value = response.metadataPolicyCrit || [];
    crit.value = response.crit || [];
    metadataPolicy.value = response.metadataPolicy
        ? JSON.stringify(response.metadataPolicy, null, 2)
        : '';
    ecLocation.value = response.ecLocation || '';
    ecLocationAutomaticResolve.value = response.ecLocationAutomaticResolve || false;
    effectiveEcLocation.value = response.effectiveEcLocation || '';
  }
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
    };

    if (isEdit.value) {
      await requestPut(subordinatePath(subordinateId.value), subordinateData);
    } else {
      await requestPost(subordinatesPath, subordinateData);
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
