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
          <p class="mt-4 text-grey">Loading trustmark subject...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-title>
        <h2>{{ isEdit ? 'Edit Trustmark Subject' : 'Create Trustmark Subject' }}</h2>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" @submit.prevent="submitForm">
          <v-combobox
              id="trustmark-subject"
              v-model="subject"
              :items="suggestions"
              item-title="value"
              item-value="value"
              :return-object="false"
              label="Subject"
              :rules="[rules.required]"
              :disabled="saving"
              :loading="loadingSuggestions"
              :hide-no-data="true"
              required
              :hint="isEdit
                  ? 'Subject entity identifier (required)'
                  : 'Subject entity identifier (required). Hosted entities are suggested.'"
              persistent-hint
              class="mb-4"
          >
            <template #item="{ props, item }">
              <v-list-item v-bind="props" :subtitle="item.raw.type"></v-list-item>
            </template>
          </v-combobox>

          <v-switch
              v-model="revoked"
              label="Revoked"
              :disabled="saving"
              color="primary"
              class="mb-4"
          ></v-switch>

          <DateTimeField
              id="field-granted"
              v-model="granted"
              label="Granted"
              hint="Date and time granted"
              :disabled="saving"
              class="mb-4"
          ></DateTimeField>

          <DateTimeField
              id="field-expires"
              v-model="expires"
              label="Expires"
              hint="Date and time of expiry. Leave empty if the trustmark should be valid forever."
              :disabled="saving"
              class="mb-4"
          ></DateTimeField>

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
import DateTimeField from '@/components/DateTimeField.vue';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {hostedEntitiesPath, trustmarkSubjectsPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();
const userStore = useUserStore();

const form = ref(null);
const saving = ref(false);

const subjectId = ref(null);
const trustmarkIdValue = ref(null);
const subject = ref('');
const revoked = ref(false);
const granted = ref('');
const expires = ref('');

// Suggestions for the subject when creating: the organization's hosted entities.
const suggestions = ref([]);
const loadingSuggestions = ref(false);

const isEdit = computed(() => !!route.params.id);
const entityId = computed(() => route.params.entityId);
const trustmarkId = computed(() => route.params.trustmarkId);
const trustmarkIssuerId = computed(() => route.query.trustmarkIssuerId || null);

function isoToLocal(isoString) {
  if (!isoString) return '';
  return isoString.replace('Z', '').replace(/[+-]\d{2}:\d{2}$/, '').substring(0, 16);
}

function localToIso(localString) {
  if (!localString) return null;
  return localString + ':00Z';
}

const rules = {
  required: (value) => {
    if (typeof value === 'string') {
      return !!value.trim() || 'This field is required.';
    }
    return !!value || 'This field is required.';
  },
};

async function loadSuggestions() {
  loadingSuggestions.value = true;
  try {
    const response = await requestGet(hostedEntitiesPath(userStore.selectedTenant, userStore.orgNumber));
    if (!Array.isArray(response)) return;

    suggestions.value = [...new Set(response.map((entity) => entity.entityIdentifier).filter(Boolean))]
        .sort((a, b) => a.localeCompare(b))
        .map((value) => ({value, type: 'Hosted entity'}));
  } finally {
    loadingSuggestions.value = false;
  }
}

async function loadSubject() {
  errorStore.clearError();
  subjectId.value = route.params.id;

  const response = await requestGet(`${trustmarkSubjectsPath(userStore.selectedTenant, userStore.orgNumber)}/${subjectId.value}`);
  if (response) {
    trustmarkIdValue.value = response.trustmarkId || trustmarkId.value || null;
    subject.value = response.subject || '';
    revoked.value = response.revoked || false;
    granted.value = isoToLocal(response.granted);
    expires.value = isoToLocal(response.expires);
  }
}

async function submitForm() {
  const {valid} = await form.value.validate();
  if (!valid) return;

  saving.value = true;
  errorStore.clearError();

  try {
    const subjectData = {
      trustmarkId: trustmarkIdValue.value || trustmarkId.value,
      subject: subject.value || '',
      revoked: revoked.value,
      granted: localToIso(granted.value),
      expires: localToIso(expires.value),
    };

    if (isEdit.value) {
      await requestPut(`${trustmarkSubjectsPath(userStore.selectedTenant, userStore.orgNumber)}/${subjectId.value}`, subjectData);
    } else {
      await requestPost(trustmarkSubjectsPath(userStore.selectedTenant, userStore.orgNumber), subjectData);
    }

    if (ok.value) {
      navigateBack();
    }
  } catch (error) {
    console.error('Error saving trustmark subject:', error);
  } finally {
    saving.value = false;
  }
}

function navigateBack() {
  const params = new URLSearchParams();
  if (trustmarkIssuerId.value) params.set('trustmarkIssuerId', trustmarkIssuerId.value);
  router.push(`/entities/${entityId.value}/modules/trustmarkissuer/trustmarks/${trustmarkId.value}/subjects?${params.toString()}`);
}

function cancel() {
  navigateBack();
}

onMounted(() => {
  errorStore.clearError();
  if (isEdit.value) {
    loadSubject();
  } else {
    if (route.query.subject) {
      subject.value = route.query.subject;
    }
    loadSuggestions();
  }
});
</script>
