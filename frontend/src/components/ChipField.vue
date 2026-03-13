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
  <v-combobox
      :model-value="modelValue"
      @update:model-value="onUpdate"
      :label="label"
      :hint="hint"
      :disabled="disabled"
      :rules="rules"
      multiple
      chips
      closable-chips
      persistent-hint
      class="mb-4"
  ></v-combobox>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  label: {
    type: String,
    required: true,
  },
  hint: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  rules: {
    type: Array,
    default: () => [],
  },
});

const emit = defineEmits(['update:modelValue']);

function onUpdate(value) {
  const cleaned = (value || []).filter(v => v && (typeof v === 'string' ? v.trim() !== '' : true));
  emit('update:modelValue', cleaned);
}
</script>
