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
      <h2>Federation</h2>
      <v-btn
          id="btn-add-federation-entity"
          color="primary"
          @click="router.push('/entities/federation/new')"
      >
        Add Federation Entity
      </v-btn>
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

    <v-row v-else-if="entities.length > 0">
      <v-col v-for="(entity, index) in entities" :key="entity.entityId" cols="12" sm="6" lg="4">
        <v-card :id="'entity-card-' + index" class="entity-card h-100" variant="outlined">
          <div class="d-flex align-center pa-2 pb-0">
            <div class="entity-name text-subtitle-1 font-weight-bold text-truncate ml-2 mr-2"
                 :title="entity.entityIdentifier">
              {{ displayName(entity) }}
            </div>
            <div class="d-flex align-center flex-grow-1">
              <v-tooltip v-for="module in activeModules(entity)" :key="module.type" :text="module.title" location="top">
                <template v-slot:activator="{ props }">
                  <span v-bind="props" :id="'icon-module-' + module.type + '-' + index" tabindex="0" role="img"
                        :aria-label="module.title">
                    <v-icon aria-hidden="true" size="small" class="mr-1">{{ module.icon }}</v-icon>
                  </span>
                </template>
              </v-tooltip>
            </div>
            <EntityConfigurationViewer
                :entity-id="entity.entityIdentifier"
                icon-only
            />
            <v-btn
                :id="'btn-edit-entity-' + index"
                icon="mdi-cog-outline"
                variant="text"
                size="small"
                aria-label="Edit entity"
                title="Edit entity"
                @click="router.push(`/entities/federation/${entity.entityId}/edit`)"
            ></v-btn>
          </div>
          <v-card-text class="pt-3">
            <div v-if="roles(entity).length > 0" class="d-flex flex-column ga-2">
              <v-btn
                  v-for="role in roles(entity)"
                  :key="role.type"
                  :id="'btn-module-' + role.type + '-' + index"
                  color="secondary"
                  variant="outlined"
                  block
                  @click="openRole(entity, role.type)"
              >
                {{ role.label }}
              </v-btn>
            </div>
            <span v-else class="text-grey">No roles configured</span>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No federation entities found.</p>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import EntityConfigurationViewer from '@/components/EntityConfigurationViewer.vue';
import {adminPath} from '@/config/path';

const router = useRouter();
const {requestGet, loading, error, ok} = useRequest();
const userStore = useUserStore();

const entities = ref([]);

// The name is optional, the entity identifier is shown when none is set.
function displayName(entity) {
  return entity.name || entity.entityIdentifier || 'N/A';
}

// The buttons open the list view of a role. The resolver has no list view and therefore no button.
function roles(entity) {
  return [
    entity.trustAnchor && {type: 'trustanchor', label: 'Subordinates'},
    entity.intermediate && {type: 'intermediate', label: 'Subordinates'},
    entity.trustmarkIssuer && {type: 'trustmarkissuer', label: 'Trustmarks'},
  ].filter(Boolean);
}

// Icons after the name show which modules are active.
function activeModules(entity) {
  return [
    entity.resolver?.active && {type: 'resolver', title: 'Resolver', icon: 'mdi-magnify-scan'},
    entity.trustAnchor?.active && {type: 'trustanchor', title: 'Trust Anchor', icon: 'mdi-shield-check'},
    entity.intermediate?.active && {type: 'intermediate', title: 'Intermediate', icon: 'mdi-transit-connection-variant'},
    entity.trustmarkIssuer?.active && {type: 'trustmarkissuer', title: 'Trustmark Issuer', icon: 'mdi-certificate-outline'},
  ].filter(Boolean);
}

function openRole(entity, type) {
  const base = `/entities/${entity.entityId}`;
  const queryParams = new URLSearchParams();
  if (type === 'trustmarkissuer') {
    const id = entity.trustmarkIssuer.trustmarkIssuerId || entity.trustmarkIssuer.id;
    if (id) queryParams.set('trustmarkIssuerId', id);
    router.push(`${base}/modules/trustmarkissuer/trustmarks?${queryParams.toString()}`);
  } else if (type === 'trustanchor' || type === 'intermediate') {
    const module = type === 'trustanchor' ? entity.trustAnchor : entity.intermediate;
    const id = type === 'trustanchor' ? module.trustAnchorId || module.id : module.intermediateId || module.id;
    if (id) queryParams.set('taImId', id);
    router.push(`${base}/modules/${type}/subordinates?${queryParams.toString()}`);
  }
}

async function loadEntities() {
  // EntityWithModules: { federationEntity: [...], hostedEntity: [...] }, here narrowed to the federation entities.
  const response = await requestGet(adminPath(userStore.selectedTenant, userStore.orgNumber) + '?type=federation&includemodules=true');
  if (response && ok.value) {
    entities.value = response.federationEntity || [];
  }
}

onMounted(loadEntities);
</script>
