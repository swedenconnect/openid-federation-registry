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
  <div role="status" aria-live="polite" class="text-center py-12">
    <v-progress-circular indeterminate color="primary" aria-hidden="true"></v-progress-circular>
  </div>
</template>

<script setup>
import {onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {useUserStore} from '@/stores/userStore';

// The start page is the first list the user may use: operators manage the federation entities, everyone else the
// hosted entities. The layout only renders this view once /tenants has loaded, so isOperator is known here.
const router = useRouter();
const userStore = useUserStore();

onMounted(() => {
  router.replace({name: userStore.isOperator ? 'federation-entities' : 'hosted-entities'});
});
</script>
