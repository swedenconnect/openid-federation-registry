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
  <div class="d-flex align-center justify-end ga-2 pa-2">
    <span class="text-body-2">Items per page:</span>
    <v-select
        v-model="itemsPerPage"
        :items="itemsPerPageOptions"
        density="compact"
        variant="outlined"
        hide-details
        single-line
        aria-label="Items per page"
        :menu-props="{maxWidth: 88, minWidth: 0}"
        class="pager-size"
    ></v-select>
    <span class="text-body-2 mx-2">{{ rangeText }}</span>
    <v-btn
        icon="mdi-chevron-left"
        variant="text"
        size="small"
        aria-label="Previous page"
        :disabled="page <= 1"
        @click="page--"
    ></v-btn>
    <v-btn
        icon="mdi-chevron-right"
        variant="text"
        size="small"
        aria-label="Next page"
        :disabled="page >= pageCount"
        @click="page++"
    ></v-btn>
  </div>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({
  total: {type: Number, required: true},
  itemsPerPageOptions: {type: Array, default: () => [50, 100]},
});

const page = defineModel('page', {type: Number, default: 1});
const itemsPerPage = defineModel('itemsPerPage', {type: Number, default: 50});

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / itemsPerPage.value)));

const rangeText = computed(() => {
  if (props.total === 0) return '0 of 0';
  const start = (page.value - 1) * itemsPerPage.value + 1;
  const end = Math.min(page.value * itemsPerPage.value, props.total);
  return `${start}-${end} of ${props.total}`;
});
</script>

<style scoped>
.pager-size {
  flex: 0 0 88px;
  width: 88px;
}
</style>
