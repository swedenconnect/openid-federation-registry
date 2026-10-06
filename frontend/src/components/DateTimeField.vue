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
  <v-menu v-model="open" :close-on-content-click="false" location="bottom start">
    <template #activator="{ props: menuProps }">
      <v-text-field
          v-bind="menuProps"
          :id="id"
          :model-value="display"
          :label="label"
          :hint="hint"
          :disabled="disabled"
          persistent-hint
          readonly
          clearable
          prepend-inner-icon="mdi-calendar"
          @click:clear="clear"
          @keydown.enter.prevent="open = true"
          @keydown.space.prevent="open = true"
      ></v-text-field>
    </template>

    <v-card min-width="320">
      <v-date-picker
          v-model="pickedDate"
          hide-header
          show-adjacent-months
      ></v-date-picker>
      <div class="px-4">
        <v-text-field
            v-model="pickedTime"
            type="time"
            label="Time"
            density="compact"
            variant="outlined"
            hide-details
        ></v-text-field>
      </div>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn variant="text" @click="clear">Clear</v-btn>
        <v-btn color="primary" variant="flat" :disabled="!pickedDate" @click="apply">OK</v-btn>
      </v-card-actions>
    </v-card>
  </v-menu>
</template>

<script setup>
import {computed, ref, watch} from 'vue';

// Date and time picker for an optional value. The value is a local date and time as 'YYYY-MM-DDTHH:mm', the same
// format as an <input type="datetime-local">, and an empty string means that nothing is set.
const props = defineProps({
  modelValue: {type: String, default: ''},
  label: {type: String, default: ''},
  hint: {type: String, default: ''},
  id: {type: String, default: undefined},
  disabled: {type: Boolean, default: false},
});
const emit = defineEmits(['update:modelValue']);

const open = ref(false);
const pickedDate = ref(null);
const pickedTime = ref('00:00');

const display = computed(() => (props.modelValue ? props.modelValue.replace('T', ' ') : ''));

function pad(value) {
  return String(value).padStart(2, '0');
}

function loadFromModel() {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}:\d{2})/.exec(props.modelValue || '');
  if (match) {
    pickedDate.value = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
    pickedTime.value = match[4];
  } else {
    pickedDate.value = null;
    pickedTime.value = '00:00';
  }
}

// Start from the current value each time the picker is opened.
watch(open, (isOpen) => {
  if (isOpen) loadFromModel();
});

function apply() {
  const date = pickedDate.value;
  if (!date) return;
  const day = `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
  emit('update:modelValue', `${day}T${pickedTime.value || '00:00'}`);
  open.value = false;
}

function clear() {
  emit('update:modelValue', '');
  open.value = false;
}
</script>
