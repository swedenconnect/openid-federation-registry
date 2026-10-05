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
    <v-card class="pa-8 text-center" max-width="560">
      <v-icon color="warning" size="48" class="mb-4" aria-hidden="true">mdi-lock-outline</v-icon>
      <v-card-title class="text-h5 mb-2 text-wrap">You do not have access to this service</v-card-title>
      <v-card-text>
        <p class="mb-2">
          <template v-if="userStore.fullName">You are logged in as {{ userStore.fullName }}, but your</template>
          <template v-else>Your</template>
          account has no permissions in the OpenID Federation Registry.
        </p>
        <p>Contact your administrator if you believe you should have access.</p>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {useUserStore} from '@/stores/userStore';

const userStore = useUserStore();
</script>
