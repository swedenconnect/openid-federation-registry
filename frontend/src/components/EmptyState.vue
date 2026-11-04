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


<!-- The text of an empty list: what is missing and how to add it, or that a search matched nothing. -->
<template>
  <p class="text-grey py-8">
    <template v-if="search">No results for "{{ search }}".</template>
    <template v-else>No {{ what }} yet.<template v-if="action"> Use {{ action }} to add one.</template></template>
  </p>
</template>

<script setup>
defineProps({
  // What the list holds, in plural, e.g. "subordinates"
  what: {
    type: String,
    required: true,
  },
  // The button that adds an item, e.g. "Add Subordinate"; leave out when the list has none
  action: {
    type: String,
    default: '',
  },
  search: {
    type: String,
    default: '',
  },
});
</script>
