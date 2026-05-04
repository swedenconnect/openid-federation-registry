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
              :rows="4"
              auto-grow
              hint="Human-readable description of this flow's purpose"
              persistent-hint
              class="mb-4"
          ></v-textarea>

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
import {registrationFlowPath, registrationFlowCreatePath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);
const name = ref('');
const description = ref('');

const isEdit = computed(() => !!route.params.id);

const rules = {
  required: (value) => {
    if (typeof value === 'string') return !!value.trim() || 'This field is required.';
    return !!value || 'This field is required.';
  },
};

async function loadFlow() {
  errorStore.clearError();
  const response = await requestGet(registrationFlowPath(route.params.id));
  if (response) {
    name.value = response.name || '';
    description.value = response.description || '';
  }
}

async function submitForm() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const payload = {
      name: name.value,
      description: description.value,
      steps: [],
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

onMounted(() => {
  errorStore.clearError();
  if (isEdit.value) {
    loadFlow();
  }
});
</script>