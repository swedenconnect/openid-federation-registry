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
          <p class="mt-4 text-grey">Loading policy...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Policy' : 'Create New Policy' }}</h2>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="savePolicy">
          <v-text-field
              v-model="policyName"
              label="Policy Name"
              :rules="[rules.required]"
              :disabled="saving"
              required
              class="mb-4"
          ></v-text-field>

          <v-textarea
              v-model="policyJson"
              label="Policy JSON"
              :rules="[rules.required, rules.json]"
              :disabled="saving"
              :rows="15"
              auto-grow
              required
              class="mb-4"
              style="font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;"
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
import {policiesPath} from '@/config/path';
import {useErrorStore} from '@/stores/errorStore';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);
const policyId = ref(null);
const policyName = ref('');
const policyJson = ref('{}');

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

async function loadPolicy() {
  errorStore.clearError();
  policyId.value = route.params.id;
  const response = await requestGet(`${policiesPath}/${policyId.value}`);
  if (response) {
    policyName.value = response.name || '';
    policyJson.value = JSON.stringify(response.policy || {}, null, 2);
  }
}

async function savePolicy() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    let parsedPolicy;
    try {
      parsedPolicy = JSON.parse(policyJson.value);
    } catch (e) {
      errorStore.setError('Invalid JSON in policy field');
      saving.value = false;
      return;
    }

    const policyData = {
      name: policyName.value,
      policy: parsedPolicy,
    };

    if (isEdit.value) {
      await requestPut(`${policiesPath}/${policyId.value}`, policyData);
    } else {
      await requestPost(policiesPath, policyData);
    }

    if (ok.value) {
      router.push('/policies');
    }
  } catch (error) {
    console.error('Error saving policy:', error);
  } finally {
    saving.value = false;
  }
}

function cancel() {
  router.push('/policies');
}

onMounted(() => {
  errorStore.clearError();
  if (isEdit.value) {
    loadPolicy();
  }
});
</script>
