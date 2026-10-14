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
    <div class="d-flex justify-space-between align-center mb-4">
      <h2>Hosted Entities</h2>
      <v-btn
          id="btn-add-hosted-entity"
          color="primary"
          @click="router.push('/entities/hosted/new')"
      >
        Add Hosted Entity
      </v-btn>
    </div>

    <div class="d-flex align-center mb-4">
      <v-text-field
          id="hosted-entity-search"
          v-model="search"
          prepend-inner-icon="mdi-magnify"
          label="Search by entity identifier"
          single-line
          hide-details
          clearable
          variant="outlined"
          density="compact"
          class="flex-grow-1"
      ></v-text-field>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">Loading entities...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="error">
      <v-card-text>
        <v-alert type="error">{{ error }}</v-alert>
      </v-card-text>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <v-data-table
            :headers="headers"
            :items="entities"
            :search="search"
            :filter-keys="['entityIdentifier']"
            v-model:page="page"
            v-model:items-per-page="itemsPerPage"
            item-value="entityId"
            hover
            @click:row="openEntity"
        >
          <template #no-data>
            <p class="text-grey py-8">No hosted entities found.</p>
          </template>
          <template #bottom>
            <TablePager v-model:page="page" v-model:items-per-page="itemsPerPage" :total="filteredCount"/>
          </template>
        </v-data-table>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRouter} from 'vue-router';
import TablePager from '@/components/TablePager.vue';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {hostedEntitiesPath} from '@/config/path';

const router = useRouter();
const {requestGet, loading, error, ok} = useRequest();
const userStore = useUserStore();

const entities = ref([]);
const search = ref('');
const page = ref(1);
const itemsPerPage = ref(50);

const filteredCount = computed(() => {
  const term = (search.value || '').toLowerCase();
  if (!term) return entities.value.length;
  return entities.value.filter((entity) => (entity.entityIdentifier || '').toLowerCase().includes(term)).length;
});

watch(search, () => {
  page.value = 1;
});

const headers = [
  {title: 'Entity Identifier', key: 'entityIdentifier', sortable: true},
  {title: 'Signing Key', key: 'signingKeyText', sortable: true},
];

function openEntity(_event, {item}) {
  router.push(`/entities/hosted/${item.entityId}/edit`);
}

async function loadEntities() {
  const response = await requestGet(hostedEntitiesPath(userStore.selectedTenant, userStore.orgNumber));
  if (response && ok.value) {
    entities.value = (Array.isArray(response) ? response : []).map((entity) => ({
      ...entity,
      signingKeyText: (entity.signingKeyId || []).join(', '),
    }));
  }
}

onMounted(loadEntities);
</script>
