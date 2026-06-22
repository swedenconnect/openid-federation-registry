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
    <v-card>
      <v-card-title>
        <h2>Create Federation Entity</h2>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="saveEntity">
          <v-text-field
              id="entity-identifier"
              v-model="entityIdentifier"
              label="Entity Identifier"
              :rules="[rules.required]"
              :disabled="saving"
              required
              hint="The entity identifier (entity ID)"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-select
              v-model="signingKeyId"
              :items="signingKeys"
              label="Signing Key"
              hint="Select signing key (kid) from oidf-service federation keys"
              persistent-hint
              clearable
              class="mb-4"
          ></v-select>

          <ListField
              id="crit"
              v-model="crit"
              label="Crit"
              hint="Crit list"
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
                id="btn-create"
                color="primary"
                type="submit"
                :loading="saving"
                :disabled="saving"
            >
              Create
            </v-btn>
          </v-card-actions>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {useSigningKeys} from '@/api/composables/signingKeys';
import ListField from '@/components/ListField.vue';
import {federationEntitiesPath} from '@/config/path';

const router = useRouter();
const {requestPost, ok} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const {signingKeys, fetchSigningKeys} = useSigningKeys();

const form = ref(null);
const saving = ref(false);
const entityIdentifier = ref('');
const crit = ref([]);
const signingKeyId = ref(null);

const rules = {
  required: (value) => !!value || 'This field is required.',
};

async function saveEntity() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const entityData = {
      entityIdentifier: entityIdentifier.value,
      crit: crit.value.filter(c => c && c.trim() !== ''),
      signingKeyId: signingKeyId.value ? [signingKeyId.value] : [],
    };

    const response = await requestPost(federationEntitiesPath, entityData);

    if (ok.value && response?.entityId) {
      router.push({name: 'federation-entity-edit', params: {id: response.entityId}});
    }
  } catch (error) {
    // Error is already handled by useRequest and set in errorStore
    console.error('Error creating federation entity:', error);
  } finally {
    saving.value = false;
  }
}

function cancel() {
  router.push('/');
}

onMounted(async () => {
  errorStore.clearError();
  if (userStore.entityPrefix) {
    entityIdentifier.value = userStore.entityPrefix;
  }
  await fetchSigningKeys('FEDERATION_ENTITY');
});
</script>

