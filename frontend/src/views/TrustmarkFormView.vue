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
import {trustmarksPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);

const trustmarkId = ref(null);
const trustmarkissuerId = ref(null);
const trustmarkType = ref('');
const logoUri = ref('');
const refUri = ref('');
const delegation = ref('');

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
