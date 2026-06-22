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
          <p class="mt-4 text-grey">Loading entity...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Hosted Entity' : 'Create Hosted Entity' }}</h2>
      </v-card-title>
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
                v-if="isEdit && entityIdentifier"
                :entity-id="entityIdentifier"
                class="mt-1"
            />
          </div>

          <v-select
              v-model="signingKeyId"
              :items="signingKeys"
              label="Signing Key"
              hint="Select signing key (kid) from oidf-service hosted keys"
              persistent-hint
              clearable
              class="mb-4"
          ></v-select>

          <v-alert
              v-if="signingKeyChanged"
              type="warning"
              variant="tonal"
              class="mb-4"
          >
            Warning: Changing the signing key will affect how this hosted entity's statements
            are signed. Ensure the new key is correctly configured and distributed before saving.
          </v-alert>

          <v-textarea
              v-model="metadata"
              label="Metadata"
              :rules="[rules.required, rules.json]"
              :disabled="saving"
              :rows="10"
              auto-grow
              required
              hint="Metadata as JSON"
              persistent-hint
              class="mb-4"
              style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
          ></v-textarea>

          <TrustmarkSourcesField
              v-model="trustmarkSources"
              :disabled="saving"
          />

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
import {useSigningKeys} from '@/api/composables/signingKeys';
import TrustmarkSourcesField from '@/components/TrustmarkSourcesField.vue';
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import {hostedEntitiesPath, hostedEntityPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const {signingKeys, fetchSigningKeys} = useSigningKeys();

const form = ref(null);
const saving = ref(false);
const entityId = ref(null);
const entityIdentifier = ref('');
const metadata = ref('{}');
const trustmarkSources = ref([]);
const signingKeyId = ref(null);
const originalSigningKeyId = ref(null);
const signingKeyChanged = computed(() =>
    isEdit.value &&
    originalSigningKeyId.value !== null &&
    signingKeyId.value !== originalSigningKeyId.value
);

const isEdit = computed(() => !!route.params.id);

const rules = {
  required: (value) => !!value || 'This field is required.',
  json: (value) => {
    if (!value) return true;
    try {
      JSON.parse(value);
      return true;
    } catch (e) {
      return 'Invalid JSON format.';
    }
  },
};

async function loadEntity() {
  errorStore.clearError();
  entityId.value = route.params.id;
  const response = await requestGet(hostedEntityPath(entityId.value));
  if (response) {
    entityIdentifier.value = response.entityIdentifier || '';
    metadata.value = JSON.stringify(response.metadata || {}, null, 2);
    trustmarkSources.value = response.trustMarkSources || response.trustmarkSources || [];
    signingKeyId.value = response.signingKeyId?.[0] || null;
    originalSigningKeyId.value = signingKeyId.value;
  }
}

async function saveEntity() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    let parsedMetadata;
    try {
      parsedMetadata = JSON.parse(metadata.value);
    } catch (e) {
      errorStore.setError('Invalid JSON in metadata field');
      saving.value = false;
      return;
    }

    const entityData = {
      entityIdentifier: entityIdentifier.value,
      metadata: parsedMetadata,
      trustMarkSources: trustmarkSources.value.filter(s => s.trustMarkIssuer || s.trustmarkId),
      signingKeyId: signingKeyId.value ? [signingKeyId.value] : [],
    };

    if (isEdit.value) {
      await requestPut(hostedEntityPath(entityId.value), entityData);
    } else {
      await requestPost(hostedEntitiesPath, entityData);
    }

    if (ok.value) {
      router.push('/');
    }
  } catch (error) {
    console.error('Error saving hosted entity:', error);
  } finally {
    saving.value = false;
  }
}

function cancel() {
  router.push('/');
}

onMounted(async () => {
  errorStore.clearError();
  await fetchSigningKeys('HOSTED_ENTITY');
  if (isEdit.value) {
    loadEntity();
  }
});
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
</style>
