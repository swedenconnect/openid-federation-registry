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
          <v-text-field
              v-model="entityIdentifier"
              label="Entity Identifier (Subject)"
              :rules="[rules.required]"
              :disabled="saving"
              required
              hint="Subject entity identifier (required, URL)"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-select
              v-model="policyId"
              :items="policyOptions"
              item-title="name"
              item-value="policyId"
              label="Policy"
              :disabled="saving"
              hint="Policy (UUID)"
              persistent-hint
              clearable
              class="mb-4"
          ></v-select>

          <v-textarea
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

          <v-btn
              color="secondary"
              variant="outlined"
              :disabled="!entityIdentifier || !entityIdentifier.trim() || loadingJwks || saving"
              :loading="loadingJwks"
              @click="loadJwks"
              class="mb-4"
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
                color="grey"
                variant="text"
                @click="cancel"
                :disabled="saving"
            >
              Cancel
            </v-btn>
            <v-btn
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
import {useLoadJwks} from '@/api/composables/jwks';
import {policiesPath, subordinatePath, subordinatesPath} from '@/config/path';
import ListField from '@/components/ListField.vue';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();
const {loadJwks: loadJwksFromApi, loading: loadingJwks} = useLoadJwks();

const form = ref(null);
const saving = ref(false);
const policies = ref([]);

const subordinateId = ref(null);
const taImIdValue = ref(null);
const entityIdentifier = ref('');
const policyId = ref(null);
const jwks = ref('');
const metadataPolicyCrit = ref([]);
const crit = ref([]);
const metadataPolicy = ref('');
const ecLocation = ref('');
const ecLocationAutomaticResolve = ref(false);
const effectiveEcLocation = ref('');

const isEdit = computed(() => !!route.params.id);
const entityId = computed(() => route.params.entityId);
const moduleType = computed(() => route.params.moduleType);
const taImId = computed(() => route.query.taImId || null);

const policyOptions = computed(() => {
  return policies.value.map(p => ({
    name: p.name || 'Unnamed Policy',
    policyId: p.policyId,
  }));
});

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

async function loadPolicies() {
  const response = await requestGet(policiesPath);
  if (response && Array.isArray(response)) {
    policies.value = response;
  }
}

async function loadJwks() {
  const jwksData = await loadJwksFromApi(entityIdentifier.value);
  if (jwksData) {
    jwks.value = JSON.stringify(jwksData, null, 2);
  }
}

async function loadSubordinate() {
  errorStore.clearError();
  subordinateId.value = route.params.id;

  const response = await requestGet(subordinatePath(subordinateId.value));
  if (response) {
    taImIdValue.value = response.taImId || taImId.value || null;
    entityIdentifier.value = response.entityIdentifier || '';
    policyId.value = response.policyId || null;
    jwks.value = typeof response.jwks === 'string'
        ? response.jwks
        : (response.jwks ? JSON.stringify(response.jwks, null, 2) : '');
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
      jwks: typeof jwks.value === 'string'
          ? jwks.value
          : (jwks.value ? JSON.stringify(jwks.value) : ''),
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

    if (policyId.value) {
      subordinateData.policyId = policyId.value;
    }

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
  loadPolicies();
  if (isEdit.value) {
    loadSubordinate();
  }
});
</script>
