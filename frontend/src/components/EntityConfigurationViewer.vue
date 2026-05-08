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
  <div>
    <v-btn
        id="btn-view-ec"
        color="secondary"
        variant="outlined"
        size="small"
        :loading="loading"
        @click="open"
    >
      View EC
    </v-btn>

    <v-dialog v-model="dialog" max-width="860" scrollable>
      <v-card>
        <v-card-title class="d-flex align-center">
          <span>Entity Configuration</span>
          <v-spacer></v-spacer>
          <span class="text-body-2 text-grey text-truncate" style="max-width: 420px;">
            {{ entityId }}
          </span>
        </v-card-title>

        <v-divider></v-divider>

        <v-card-text style="max-height: 72vh; overflow-y: auto;">
          <div v-if="loading" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
            <p class="mt-4 text-grey">Fetching entity configuration…</p>
          </div>

          <v-alert
              v-else-if="errorMessage"
              type="error"
              variant="tonal"
              class="mt-2"
          >
            {{ errorMessage }}
          </v-alert>

          <template v-else-if="ecData">
            <p class="text-overline text-grey mb-1 mt-2">Header</p>
            <pre class="json-block mb-6">{{ JSON.stringify(ecData.header, null, 2) }}</pre>

            <p class="text-overline text-grey mb-1">Payload</p>
            <pre class="json-block">{{ JSON.stringify(ecData.payload, null, 2) }}</pre>
          </template>
        </v-card-text>

        <v-divider></v-divider>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn id="btn-ec-close" variant="text" @click="dialog = false">Close</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {ref} from 'vue';
import {useErrorStore} from '@/stores/errorStore';
import {entityConfigurationViewPath} from '@/config/path';

const props = defineProps({
  entityId: {
    type: String,
    required: true,
  },
});

const errorStore = useErrorStore();

const dialog = ref(false);
const loading = ref(false);
const ecData = ref(null);
const errorMessage = ref(null);

async function open() {
  dialog.value = true;
  ecData.value = null;
  errorMessage.value = null;
  loading.value = true;

  try {
    const response = await fetch(entityConfigurationViewPath, {
      method: 'POST',
      credentials: 'include',
      headers: {'Content-Type': 'text/plain'},
      body: props.entityId,
    });

    if (response.ok) {
      ecData.value = await response.json();
    } else {
      errorMessage.value = 'Could not fetch entity configuration. The entity may be unreachable or the response is invalid.';
    }
  } catch (e) {
    errorMessage.value = 'Could not fetch entity configuration. The entity may be unreachable or the response is invalid.';
    errorStore.setError(e.message);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.json-block {
  background: #f5f5f5;
  border-radius: 4px;
  padding: 16px;
  font-family: monospace;
  font-size: 0.82rem;
  white-space: pre-wrap;
  word-break: break-all;
  overflow: auto;
}
</style>
