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
  <v-app>
    <v-app-bar color="primary" prominent>
      <v-btn
          to="/"
          :variant="isEntityRoute ? 'flat' : 'text'"
          color="white"
      >
        Entity
      </v-btn>
      <v-btn
          to="/policies"
          :variant="isPolicyRoute ? 'flat' : 'text'"
          color="white"
      >
        Policy
      </v-btn>

      <v-spacer></v-spacer>

      <v-app-bar-title class="text-center">
        {{ userStore.orgName }}
      </v-app-bar-title>

      <v-spacer></v-spacer>

      <v-select
          v-if="userStore.isAuthorized && userStore.organizations.length > 1"
          :model-value="userStore.orgNumber"
          :items="userStore.organizations"
          item-title="orgName"
          item-value="orgNumber"
          density="compact"
          variant="solo-filled"
          hide-details
          style="max-width: 250px;"
          class="mr-2"
          @update:model-value="handleOrgChange"
      ></v-select>

      <v-btn
          v-if="userStore.isAuthorized"
          color="white"
          variant="text"
          @click="logout"
      >
        Logout
      </v-btn>
    </v-app-bar>

    <v-main>
      <v-container fluid>
        <!-- Error Banner -->
        <v-alert
            v-if="errorMessage"
            type="error"
            closable
            @click:close="clearError"
            class="mb-4"
        >
          {{ errorMessage }}
        </v-alert>

        <!-- Main Content -->
        <RouterView/>
      </v-container>
    </v-main>

    <v-footer app class="text-right">
      <v-spacer></v-spacer>
      <span class="text-caption">OpenID Federation Admin Sweden Connect Copyright 2026</span>
    </v-footer>
  </v-app>
</template>

<script setup>
import {computed, onBeforeMount} from 'vue';
import {RouterView, useRoute} from 'vue-router';
import {useErrorStore} from '@/stores/errorStore';
import {useUserStore} from '@/stores/userStore';
import {logoutPath} from '@/config/path';

const route = useRoute();
const errorStore = useErrorStore();
const userStore = useUserStore();

const isEntityRoute = computed(() => route.path === '/');
const isPolicyRoute = computed(() => route.path.startsWith('/policies'));

const errorMessage = computed(() => errorStore.message);

function clearError() {
  errorStore.clearError();
}

function handleOrgChange(orgNumber) {
  userStore.selectOrganization(orgNumber);
}

function logout() {
  globalThis.location.href = logoutPath;
}

onBeforeMount(async () => {
  if (route.name !== 'login') {
    await userStore.fetchUser();
  }
});
</script>
