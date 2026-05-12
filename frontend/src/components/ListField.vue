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
  <div class="list-field">
    <div class="list-field-label mb-2">
      <label class="text-body-2">{{ label }}</label>
    </div>

    <div v-if="items.length > 0" class="list-items mb-2">
      <div
          v-for="(item, index) in items"
          :key="index"
          class="list-item d-flex align-center mb-2"
      >
        <v-text-field
            :id="id ? `${id}-item-${index}` : undefined"
            :model-value="item"
            @update:model-value="(value) => updateItem(index, value)"
            :disabled="disabled"
            :rules="rules"
            density="compact"
            variant="outlined"
            hide-details="auto"
            class="flex-grow-1 mr-2"
        ></v-text-field>
        <v-btn
            :id="id ? `${id}-delete-${index}` : undefined"
            icon
            size="small"
            color="error"
            variant="text"
            @click="removeItem(index)"
            :disabled="disabled"
        >
          <v-icon>mdi-delete</v-icon>
        </v-btn>
      </div>
    </div>

    <v-btn
        :id="id ? `${id}-add` : undefined"
        color="primary"
        variant="outlined"
        size="small"
        @click="addItem"
        :disabled="disabled"
        class="mt-2"
    >
      <v-icon start>mdi-plus</v-icon>
      Add
    </v-btn>

    <div v-if="hint" class="text-caption text-medium-emphasis mt-1">
      {{ hint }}
    </div>
  </div>
</template>

<script setup>
import {ref, watch} from 'vue';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  id: {
    type: String,
    default: '',
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

// Create local copy of items to work with
const items = ref([...props.modelValue]);

// Watch for external changes to modelValue
watch(
    () => props.modelValue,
    (newValue) => {
      if (newValue && Array.isArray(newValue)) {
        items.value = [...newValue];
      } else {
        items.value = [];
      }
    },
    {deep: true}
);

function updateItem(index, value) {
  items.value[index] = value;
  emit('update:modelValue', [...items.value]);
}

function removeItem(index) {
  items.value.splice(index, 1);
  emit('update:modelValue', [...items.value]);
}

function addItem() {
  items.value.push('');
  emit('update:modelValue', [...items.value]);
}
</script>

<style scoped>
.list-field {
  margin-bottom: 16px;
}

.list-item {
  min-height: 40px;
}
</style>
