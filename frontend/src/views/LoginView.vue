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
        <p>Please log in to continue.</p>
      </v-card-text>
      <v-card-actions class="justify-center">
        <v-btn
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
import {adminAuthenticatePath} from '@/config/path';

function login() {
  globalThis.location.href = adminAuthenticatePath;
}
</script>
