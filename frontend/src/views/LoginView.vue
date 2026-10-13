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
  <div class="d-flex justify-center align-center" style="min-height: 60vh;">
    <v-card class="pa-8 text-center" min-width="400">
      <v-card-title class="text-h4 mb-4">OpenID Federation - Administration</v-card-title>
      <v-card-text class="mb-4">
        <v-alert
            v-if="errorMessage"
            type="error"
            role="alert"
            aria-live="assertive"
            variant="tonal"
            class="mb-4 text-left"
        >
          {{ errorMessage }}
          <div v-if="errorReason" class="text-body-small mt-1">Reason: {{ errorReason }}</div>
        </v-alert>
        <p>Please log in to continue.</p>
      </v-card-text>
      <v-card-actions class="justify-center">
        <v-btn
            id="btn-login"
            color="primary"
            variant="flat"
            size="large"
            @click="login"
        >
          Login
        </v-btn>
      </v-card-actions>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, onUnmounted} from 'vue';
import {useRoute} from 'vue-router';
import {adminAuthenticatePath} from '@/config/path';

const ERROR_MESSAGES = {
  'backend.login.failed': 'Login failed. Please try again or contact support if the problem persists.',
  'backend.login.denied': 'The login was denied or cancelled. Please try again.',
};

const route = useRoute();
const errorMessage = computed(() => {
  const code = route.query.errorcode;
  if (!code) return null;
  return ERROR_MESSAGES[code] ?? `Login error: ${code}`;
});

// The OAuth2 error code from Spring Security or the identity provider, for example invalid_token_response.
const errorReason = computed(() => {
  const reason = route.query.reason;
  return typeof reason === 'string' && /^[a-z][a-z0-9_]{0,63}$/.test(reason) ? reason : null;
});

function login() {
  globalThis.location.href = adminAuthenticatePath;
}

function onKeydown(e) {
  if (e.key === 'Enter') login();
}

onMounted(() => document.addEventListener('keydown', onKeydown));
onUnmounted(() => document.removeEventListener('keydown', onKeydown));
</script>
