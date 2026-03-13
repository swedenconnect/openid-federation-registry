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
          <v-text-field
              v-model="subject"
              label="Subject"
              :rules="[rules.required]"
              :disabled="saving"
              required
              hint="Subject entity identifier (required)"
              persistent-hint
              class="mb-4"
          ></v-text-field>

          <v-switch
              v-model="revoked"
              label="Revoked"
              :disabled="saving"
              color="primary"
              class="mb-4"
          ></v-switch>

          <v-text-field
              v-model="granted"
              label="Granted"
              type="datetime-local"
              :disabled="saving"
              hint="Date and time granted"
              persistent-hint
              clearable
              class="mb-4"
          ></v-text-field>

          <v-text-field
              v-model="expires"
              label="Expires"
              type="datetime-local"
              :disabled="saving"
              hint="Date and time of expiry"
              persistent-hint
              clearable
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
import {trustmarkSubjectsPath} from '@/config/path';

const route = useRoute();
const router = useRouter();
const {requestGet, requestPost, requestPut, loading, ok} = useRequest();
const errorStore = useErrorStore();

const form = ref(null);
const saving = ref(false);

const subjectId = ref(null);
const trustmarkIdValue = ref(null);
const subject = ref('');
const revoked = ref(false);
const granted = ref('');
const expires = ref('');

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

async function loadSubject() {
  errorStore.clearError();
  subjectId.value = route.params.id;

  const response = await requestGet(`${trustmarkSubjectsPath}/${subjectId.value}`);
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
      await requestPut(`${trustmarkSubjectsPath}/${subjectId.value}`, subjectData);
    } else {
      await requestPost(trustmarkSubjectsPath, subjectData);
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
  }
});
</script>
