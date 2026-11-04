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
        <h2>{{ isEdit ? 'Edit Hosted Entity' : 'Add Hosted Entity' }}</h2>
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
              v-if="signingKeysError"
              id="alert-signing-keys"
              type="warning"
              variant="tonal"
              density="compact"
              class="mb-4"
          >
            {{ signingKeysError }}
          </v-alert>

          <v-alert
              v-if="signingKeyChanged"
              type="warning"
              variant="tonal"
              class="mb-4"
          >
            Warning: Changing the signing key will affect how this hosted entity's statements
            are signed. Ensure the new key is correctly configured and distributed before saving.
          </v-alert>

          <ListField
              id="authority-hints"
              v-model="authorityhints"
              label="Authority Hints"
              hint="Entity identifiers of superiors to add to authority_hints. Superiors with a subordinate pointing to this entity are added automatically."
              :rules="[rules.url]"
              :disabled="saving"
          />

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
              suggest
              :subject="entityIdentifier"
              :disabled="saving"
          />

          <v-card-actions>
            <v-btn
                v-if="isEdit"
                id="btn-delete-entity"
                color="error"
                variant="text"
                :disabled="saving"
                @click="deleteDialog = true"
            >
              Delete Hosted Entity
            </v-btn>
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
              {{ isEdit ? 'Save' : 'Add' }}
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500" aria-labelledby="delete-entity-dialog-title">
      <v-card>
        <v-card-title id="delete-entity-dialog-title" class="text-h5">Delete Hosted Entity</v-card-title>
        <v-card-text>
          Are you sure you want to delete entity "{{ entityIdentifier }}"? This action cannot be undone.
          <v-checkbox
              id="chk-delete-subordinates"
              v-model="deleteSubordinates"
              :disabled="deleting"
              density="compact"
              hide-details
              class="mt-2"
              label="Also delete the subordinate statements of the entity"
          ></v-checkbox>
          <v-checkbox
              id="chk-delete-trustmark-subjects"
              v-model="deleteTrustmarkSubjects"
              :disabled="deleting"
              density="compact"
              hide-details
              label="Also remove the entity as subject of its Trust Marks"
          ></v-checkbox>
          <div class="text-caption text-medium-emphasis ml-8">
            Subordinate statements of this organization are removed. Trust Mark Subjects are removed in the whole
            federation, also on Trust Marks of other organizations.
          </div>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              id="btn-delete-entity-cancel"
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deleting"
          >
            Cancel
          </v-btn>
          <v-btn
              id="btn-delete-entity-confirm"
              color="error"
              @click="deleteEntity"
              :loading="deleting"
              :disabled="deleting"
          >
            Delete Hosted Entity
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
import {useUnsavedChanges} from '@/api/composables/unsavedChanges';
import {useErrorStore} from '@/stores/errorStore';
import {useNoticeStore} from '@/stores/noticeStore';
import {useUserStore} from '@/stores/userStore';
import {useSigningKeys} from '@/api/composables/signingKeys';
import TrustmarkSourcesField from '@/components/TrustmarkSourcesField.vue';
import ListField from '@/components/ListField.vue';
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import {hostedEntitiesPath, hostedEntityPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, requestDelete, loading, ok} = useRequest();
const errorStore = useErrorStore();
const noticeStore = useNoticeStore();
const userStore = useUserStore();

const {signingKeys, signingKeysError, fetchSigningKeys} = useSigningKeys();

const form = ref(null);
const saving = ref(false);
const deleteDialog = ref(false);
const deleting = ref(false);
const deleteTrustmarkSubjects = ref(true);
const deleteSubordinates = ref(true);
const entityId = ref(null);
const entityIdentifier = ref('');
const metadata = ref('{}');
const trustmarkSources = ref([]);
const authorityhints = ref([]);
const signingKeyId = ref(null);
const originalSigningKeyId = ref(null);

const {markSaved} = useUnsavedChanges(() => ({
  entityIdentifier: entityIdentifier.value,
  metadata: metadata.value,
  trustmarkSources: trustmarkSources.value,
  signingKeyId: signingKeyId.value,
  authorityhints: authorityhints.value,
}));
const signingKeyChanged = computed(() =>
    isEdit.value &&
    originalSigningKeyId.value !== null &&
    signingKeyId.value !== originalSigningKeyId.value
);

const isEdit = computed(() => !!route.params.id);

const rules = {
  required: (value) => !!value || 'This field is required.',
  url: (value) => {
    if (!value) return true;
    try {
      new URL(value);
      return true;
    } catch (e) {
      return 'Invalid URL format.';
    }
  },
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
  entityId.value = route.params.id;
  const response = await requestGet(hostedEntityPath(userStore.selectedTenant, userStore.orgNumber, entityId.value));
  if (response) {
    entityIdentifier.value = response.entityIdentifier || '';
    metadata.value = JSON.stringify(response.metadata || {}, null, 2);
    trustmarkSources.value = response.trustMarkSources || response.trustmarkSources || [];
    authorityhints.value = response.authorityhints || [];
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
      authorityhints: authorityhints.value.map(h => h?.trim()).filter(h => h),
      signingKeyId: signingKeyId.value ? [signingKeyId.value] : [],
    };

    if (isEdit.value) {
      await requestPut(hostedEntityPath(userStore.selectedTenant, userStore.orgNumber, entityId.value), entityData);
    } else {
      await requestPost(hostedEntitiesPath(userStore.selectedTenant, userStore.orgNumber), entityData);
    }

    if (ok.value) {
      noticeStore.show(isEdit.value ? 'Hosted Entity saved.' : 'Hosted Entity added.');
      markSaved();
      router.push({name: 'hosted-entities'});
    }
  } catch (error) {
    console.error('Error saving hosted entity:', error);
  } finally {
    saving.value = false;
  }
}

async function deleteEntity() {
  deleting.value = true;
  errorStore.clearError();
  try {
    const path = hostedEntityPath(userStore.selectedTenant, userStore.orgNumber, entityId.value);
    const query = new URLSearchParams({
      deleteTrustmarkSubjects: String(deleteTrustmarkSubjects.value),
      deleteSubordinates: String(deleteSubordinates.value),
    });
    await requestDelete(`${path}?${query}`);
    if (ok.value) {
      deleteDialog.value = false;
      noticeStore.show('Hosted Entity deleted.');
      markSaved();
      router.push({name: 'hosted-entities'});
    }
  } catch (error) {
    console.error('Error deleting hosted entity:', error);
  } finally {
    deleting.value = false;
  }
}

function cancel() {
  router.push({name: 'hosted-entities'});
}

onMounted(async () => {
  await fetchSigningKeys('HOSTED_ENTITY');
  if (isEdit.value) {
    await loadEntity();
  }
  markSaved();
});
</script>

<style scoped>
.gap-2 {
  gap: 8px;
}
</style>
