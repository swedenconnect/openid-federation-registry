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

<!--
  Edits the metadata policy a Trust Anchor or Intermediate applies to every subordinate statement it issues, as JSON
  with the root key metadata_policy. A policy template configured in the registry can be loaded as a starting point;
  the text is then the module's own, so a later change to the template does not change it.
-->
<template>
  <div>
    <div class="text-subtitle-2 mb-1">Metadata policy for subordinates</div>
    <div class="text-caption text-medium-emphasis mb-2">
      Combined with each subordinate's own policy in every subordinate statement. Leave empty for none.
    </div>
    <div v-if="templates.length > 0" class="d-flex align-center ga-2 mb-2">
      <v-select
          :id="`${idPrefix}-policy-template`"
          v-model="selected"
          :items="templates"
          item-title="name"
          item-value="name"
          return-object
          label="Policy template"
          density="compact"
          hide-details
          :disabled="disabled"
          class="flex-grow-1"
      >
        <template #item="{ props: itemProps, item }">
          <v-list-item v-bind="itemProps" :subtitle="templateSubtitle(item.raw)"></v-list-item>
        </template>
      </v-select>
      <v-btn :id="`btn-${idPrefix}-load-policy-template`" variant="outlined" size="small"
             :disabled="!selected || disabled" @click="loadTemplate">
        Load template
      </v-btn>
    </div>
    <v-alert
        v-if="leftOut.length > 0"
        :id="`${idPrefix}-policy-template-left-out`"
        type="warning"
        variant="tonal"
        density="compact"
        class="mb-2"
    >
      Left out of {{ leftOutFrom }}, not supported by the registry yet:
      <ul class="pl-4">
        <li v-for="entry in leftOut" :key="entry" class="mono-text">{{ entry }}</li>
      </ul>
    </v-alert>
    <v-textarea
        :id="`${idPrefix}-metadata-policy`"
        :model-value="modelValue"
        label="Metadata policy (JSON)"
        rows="10"
        class="mono"
        :disabled="disabled"
        :error-messages="parseError"
        @update:model-value="value => emit('update:modelValue', value)"
    ></v-textarea>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {templatesPath} from '@/config/path';

const props = defineProps({
  // The policy as JSON text; empty for none.
  modelValue: {type: String, default: ''},
  disabled: {type: Boolean, default: false},
  idPrefix: {type: String, required: true},
});
const emit = defineEmits(['update:modelValue']);

const {requestGet} = useRequest();
const userStore = useUserStore();
const templates = ref([]);
const selected = ref(null);
// What the registry left out of the template loaded last, shown until another one is loaded.
const leftOut = ref([]);
const leftOutFrom = ref('');

function templateSubtitle(template) {
  const omitted = template.omitted?.length ?? 0;
  return [template.description, omitted > 0 ? `${omitted} left out, not supported yet` : null]
      .filter(Boolean).join(' · ');
}

const parseError = computed(() => {
  if (!props.modelValue || !props.modelValue.trim()) return [];
  try {
    const value = JSON.parse(props.modelValue);
    return value && typeof value === 'object' && value.metadata_policy && typeof value.metadata_policy === 'object'
        ? [] : ['Needs the root key metadata_policy holding a JSON object.'];
  } catch (e) {
    return ['Not valid JSON: ' + e.message];
  }
});

function loadTemplate() {
  if (!selected.value) return;
  leftOut.value = selected.value.omitted ?? [];
  leftOutFrom.value = selected.value.name;
  emit('update:modelValue', selected.value.content);
}

onMounted(async () => {
  const response = await requestGet(templatesPath(userStore.selectedTenant, userStore.orgNumber, 'POLICY'));
  templates.value = Array.isArray(response) ? response : [];
});
</script>

<style scoped>
.mono-text {
  font-family: monospace;
}

.mono :deep(textarea) {
  font-family: monospace;
  font-size: 0.85rem;
}
</style>
