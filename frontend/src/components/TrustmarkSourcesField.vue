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
  <div class="trustmark-sources-field mb-4">
    <label class="text-body-2 d-block mb-2">TrustMark Sources</label>

    <div v-if="groups.length === 0" class="text-caption text-medium-emphasis mb-2">
      No trustmark sources added.
    </div>

    <v-card
        v-for="(group, gi) in groups"
        :key="gi"
        variant="outlined"
        class="mb-3 pa-3"
    >
      <div class="d-flex align-center mb-2">
        <v-text-field
            :model-value="group.issuer"
            @update:model-value="(val) => updateIssuer(gi, val)"
            label="TrustMark Issuer"
            hint="Entity identifier URL of the issuer"
            persistent-hint
            density="compact"
            :disabled="disabled"
            class="flex-grow-1 mr-2"
        ></v-text-field>
        <v-btn
            icon
            size="small"
            color="error"
            variant="text"
            :aria-label="`Remove issuer ${gi + 1}`"
            @click="removeIssuer(gi)"
            :disabled="disabled"
        >
          <v-icon aria-hidden="true">mdi-delete</v-icon>
        </v-btn>
      </div>

      <div class="ml-2">
        <label class="text-caption text-medium-emphasis">Trustmarks</label>
        <div
            v-for="(tm, ti) in group.trustmarks"
            :key="ti"
            class="d-flex align-center mb-1"
        >
          <v-text-field
              :model-value="tm"
              @update:model-value="(val) => updateTrustmark(gi, ti, val)"
              hint="Trustmark identifier URL"
              density="compact"
              variant="outlined"
              hide-details="auto"
              :disabled="disabled"
              class="flex-grow-1 mr-2"
          ></v-text-field>
          <v-btn
              icon
              size="x-small"
              color="error"
              variant="text"
              :aria-label="`Remove trustmark ${ti + 1} from issuer ${gi + 1}`"
              @click="removeTrustmark(gi, ti)"
              :disabled="disabled"
          >
            <v-icon aria-hidden="true">mdi-close</v-icon>
          </v-btn>
        </div>
        <v-btn
            color="primary"
            variant="text"
            size="small"
            @click="addTrustmark(gi)"
            :disabled="disabled"
            class="mt-1"
        >
          <v-icon start>mdi-plus</v-icon>
          Add Trustmark
        </v-btn>
      </div>
    </v-card>

    <v-btn
        color="primary"
        variant="outlined"
        size="small"
        @click="addIssuer"
        :disabled="disabled"
    >
      <v-icon start>mdi-plus</v-icon>
      Add Issuer
    </v-btn>

    <div class="text-caption text-medium-emphasis mt-1">
      TrustMark sources for the trustmarks to be fetched and included
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
  disabled: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:modelValue']);

const groups = ref([]);
let internalUpdate = false;

function flatToGroups(flat) {
  const map = new Map();
  for (const item of flat) {
    const issuer = item.trustMarkIssuer || '';
    if (!map.has(issuer)) {
      map.set(issuer, []);
    }
    if (item.trustmarkId) {
      map.get(issuer).push(item.trustmarkId);
    }
  }
  return Array.from(map.entries()).map(([issuer, trustmarks]) => ({
    issuer,
    trustmarks,
  }));
}

function groupsToFlat(grouped) {
  const result = [];
  for (const group of grouped) {
    const issuer = group.issuer || '';
    if (group.trustmarks.length === 0) {
      result.push({trustMarkIssuer: issuer, trustmarkId: ''});
    } else {
      for (const tm of group.trustmarks) {
        result.push({trustMarkIssuer: issuer, trustmarkId: tm});
      }
    }
  }
  return result;
}

function emitUpdate() {
  internalUpdate = true;
  emit('update:modelValue', groupsToFlat(groups.value));
}

function updateIssuer(gi, value) {
  groups.value[gi].issuer = value;
  emitUpdate();
}

function removeIssuer(gi) {
  groups.value.splice(gi, 1);
  emitUpdate();
}

function addIssuer() {
  groups.value.push({issuer: '', trustmarks: ['']});
  emitUpdate();
}

function updateTrustmark(gi, ti, value) {
  groups.value[gi].trustmarks[ti] = value;
  emitUpdate();
}

function removeTrustmark(gi, ti) {
  groups.value[gi].trustmarks.splice(ti, 1);
  emitUpdate();
}

function addTrustmark(gi) {
  groups.value[gi].trustmarks.push('');
  emitUpdate();
}

// Initialize from modelValue
groups.value = flatToGroups(props.modelValue);

watch(
    () => props.modelValue,
    (newValue) => {
      if (internalUpdate) {
        internalUpdate = false;
        return;
      }
      groups.value = flatToGroups(newValue || []);
    },
    {deep: true}
);
</script>
