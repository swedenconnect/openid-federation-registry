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
      <h2>Entities</h2>
      <div class="d-flex gap-2">
        <v-btn
            color="primary"
            @click="addFederationEntity"
            class="mr-2"
        >
          Add Federation Entity
        </v-btn>
        <v-btn
            color="primary"
            @click="addHostedEntity"
        >
          Add Hosted Entity
        </v-btn>
      </div>
    </div>

    <v-card v-if="loading">
      <v-card-text>
        <div class="text-center py-12">
          <v-progress-circular
              indeterminate
              color="primary"
              size="64"
          ></v-progress-circular>
          <p class="mt-4 text-grey">Loading entities...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="error">
      <v-card-text>
        <v-alert type="error">
          {{ error }}
        </v-alert>
      </v-card-text>
    </v-card>

    <v-card v-else-if="entities.length > 0">
      <v-table>
        <thead>
        <tr>
          <th class="text-left">Entity Identifier</th>
          <th class="text-left">Modules</th>
          <th class="text-right">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(entity, index) in entities" :key="index">
          <td>{{ getEntityIdentifier(entity) }}</td>
          <td>
            <div v-if="getEntityType(entity) === 'federation'" class="d-flex gap-2 flex-wrap">
              <v-btn
                  v-if="hasModule(entity, 'trustanchor')"
                  color="secondary"
                  variant="outlined"
                  size="small"
                  @click="viewModule(entity, 'trustanchor')"
              >
                Trustanchor
              </v-btn>
              <v-btn
                  v-if="hasModule(entity, 'intermediate')"
                  color="secondary"
                  variant="outlined"
                  size="small"
                  @click="viewModule(entity, 'intermediate')"
              >
                Intermediate
              </v-btn>
              <v-btn
                  v-if="hasModule(entity, 'resolver')"
                  color="secondary"
                  variant="outlined"
                  size="small"
                  @click="viewModule(entity, 'resolver')"
              >
                Resolver
              </v-btn>
              <v-btn
                  v-if="hasModule(entity, 'trustmarkissuer')"
                  color="secondary"
                  variant="outlined"
                  size="small"
                  @click="viewModule(entity, 'trustmarkissuer')"
              >
                Trustmark Issuer
              </v-btn>
            </div>
            <span v-else class="text-grey">-</span>
          </td>
          <td class="text-right">
            <v-btn
                v-if="getEntityType(entity) === 'federation' || getEntityType(entity) === 'hosted'"
                color="primary"
                variant="text"
                size="small"
                @click="editEntity(entity)"
                class="mr-2"
            >
              Edit
            </v-btn>
            <v-btn
                v-if="getEntityType(entity) === 'federation' || getEntityType(entity) === 'hosted'"
                color="error"
                variant="text"
                size="small"
                @click="confirmDelete(entity)"
            >
              Delete
            </v-btn>
          </td>
        </tr>
        </tbody>
      </v-table>
    </v-card>

    <v-card v-else>
      <v-card-text>
        <div class="text-center py-12">
          <p class="text-grey">No entities found.</p>
        </div>
      </v-card-text>
    </v-card>

    <!-- Delete Confirmation Dialog -->
    <v-dialog v-model="deleteDialog" max-width="500">
      <v-card>
        <v-card-title class="text-h5">Confirm Delete</v-card-title>
        <v-card-text>
          Are you sure you want to delete entity "{{ deleteEntityName }}"? This action cannot be undone.
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              color="grey"
              variant="text"
              @click="deleteDialog = false"
              :disabled="deleting"
          >
            Cancel
          </v-btn>
          <v-btn
              color="error"
              @click="deleteEntity"
              :loading="deleting"
              :disabled="deleting"
          >
            Yes, Delete
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useErrorStore} from '@/stores/errorStore';
import {adminPath, federationEntityPath, hostedEntityPath} from '@/config/path';

const router = useRouter();
const {requestGet, requestDelete, loading, error, ok} = useRequest();
const errorStore = useErrorStore();

const entities = ref([]);
const deleteDialog = ref(false);
const deleting = ref(false);
const entityToDelete = ref(null);
const deleteEntityName = computed(() => {
  if (!entityToDelete.value) return '';
  return getEntityIdentifier(entityToDelete.value);
});

function getEntityIdentifier(entity) {
  if (entity.federationEntity) {
    return entity.federationEntity.entityIdentifier || 'N/A';
  }
  if (entity.hostedEntity) {
    return entity.hostedEntity.entityIdentifier || 'N/A';
  }
  if (entity.subordinateEntity) {
    return entity.subordinateEntity.subject || 'N/A';
  }
  return 'N/A';
}

function getEntityType(entity) {
  if (entity.federationEntity) {
    return 'federation';
  }
  if (entity.hostedEntity) {
    return 'hosted';
  }
  if (entity.subordinateEntity) {
    return 'subordinate';
  }
  return null;
}

function getEntityId(entity) {
  if (entity.federationEntity) {
    return entity.federationEntity.entityId || entity.federationEntity.id;
  }
  if (entity.hostedEntity) {
    return entity.hostedEntity.entityId || entity.hostedEntity.id;
  }
  return null;
}

async function loadEntities() {
  const response = await requestGet(adminPath + '?includemodules=true');
  if (response && ok.value) {
    // API returns EntityWithModules: { federationEntity: [...], hostedEntity: [...] }
    const allEntities = [];
    if (response.federationEntity && Array.isArray(response.federationEntity)) {
      allEntities.push(...response.federationEntity.map(e => ({federationEntity: e})));
    }
    if (response.hostedEntity && Array.isArray(response.hostedEntity)) {
      allEntities.push(...response.hostedEntity.map(e => ({hostedEntity: e})));
    }
    entities.value = allEntities;
  }
}

function hasModule(entity, moduleType) {
  if (getEntityType(entity) !== 'federation') return false;
  // Check if entity has modules in the response
  // When includemodules=true, the response structure may be different
  const fedEntity = entity.federationEntity;
  if (!fedEntity) return false;

  // Check direct properties first
  if (fedEntity.trustAnchor && moduleType === 'trustanchor') return true;
  if (fedEntity.intermediate && moduleType === 'intermediate') return true;
  if (fedEntity.resolver && moduleType === 'resolver') return true;
  if (fedEntity.trustmarkIssuer && moduleType === 'trustmarkissuer') return true;

  // Also check if modules are in the entity object directly
  if (entity.trustAnchor && moduleType === 'trustanchor') return true;
  if (entity.intermediate && moduleType === 'intermediate') return true;
  if (entity.resolver && moduleType === 'resolver') return true;
  if (entity.trustmarkIssuer && moduleType === 'trustmarkissuer') return true;

  return false;
}

function viewModule(entity, moduleType) {
  const entityId = getEntityId(entity);
  const entityIdentifier = getEntityIdentifier(entity);

  // For trustmarkissuer, show trustmarks list
  if (moduleType === 'trustmarkissuer') {
    const fedEntity = entity.federationEntity || entity;
    let trustmarkIssuerId = null;

    if (fedEntity.trustmarkIssuer) {
      trustmarkIssuerId = fedEntity.trustmarkIssuer.trustmarkIssuerId || fedEntity.trustmarkIssuer.id;
    }
    if (!trustmarkIssuerId && entity.trustmarkIssuer) {
      trustmarkIssuerId = entity.trustmarkIssuer.trustmarkIssuerId || entity.trustmarkIssuer.id;
    }

    const queryParams = new URLSearchParams();
    if (trustmarkIssuerId) {
      queryParams.set('trustmarkIssuerId', trustmarkIssuerId);
    }

    router.push(`/entities/${entityId}/modules/trustmarkissuer/trustmarks?${queryParams.toString()}`);
    return;
  }

  // For trustanchor and intermediate, show subordinates
  if (moduleType === 'trustanchor' || moduleType === 'intermediate') {
    // Get module ID (taImId) from entity
    let taImId = null;
    const fedEntity = entity.federationEntity || entity;

    if (moduleType === 'trustanchor' && fedEntity.trustAnchor) {
      taImId = fedEntity.trustAnchor.trustAnchorId || fedEntity.trustAnchor.id;
    } else if (moduleType === 'intermediate' && fedEntity.intermediate) {
      taImId = fedEntity.intermediate.intermediateId || fedEntity.intermediate.id;
    }

    // Also check direct properties on entity
    if (!taImId) {
      if (moduleType === 'trustanchor' && entity.trustAnchor) {
        taImId = entity.trustAnchor.trustAnchorId || entity.trustAnchor.id;
      } else if (moduleType === 'intermediate' && entity.intermediate) {
        taImId = entity.intermediate.intermediateId || entity.intermediate.id;
      }
    }

    const queryParams = new URLSearchParams({
      issuer: entityIdentifier,
    });
    if (taImId) {
      queryParams.set('taImId', taImId);
    }

    router.push(`/entities/${entityId}/modules/${moduleType}/subordinates?${queryParams.toString()}`);
  } else {
    // For other modules, could navigate to edit view or show details
    // For now, just show a message or navigate to edit
    router.push(`/entities/federation/${entityId}/edit`);
  }
}

function addFederationEntity() {
  router.push('/entities/federation/new');
}

function addHostedEntity() {
  router.push('/entities/hosted/new');
}

function editEntity(entity) {
  const entityType = getEntityType(entity);
  const entityId = getEntityId(entity);
  if (entityType === 'federation' && entityId) {
    router.push(`/entities/federation/${entityId}/edit`);
  } else if (entityType === 'hosted' && entityId) {
    router.push(`/entities/hosted/${entityId}/edit`);
  }
}

function confirmDelete(entity) {
  entityToDelete.value = entity;
  deleteDialog.value = true;
}

async function deleteEntity() {
  if (!entityToDelete.value) return;

  deleting.value = true;
  errorStore.clearError();

  try {
    const entityType = getEntityType(entityToDelete.value);
    const entityId = getEntityId(entityToDelete.value);

    if (!entityId) {
      errorStore.setError('Entity ID not found');
      deleting.value = false;
      return;
    }

    const endpoint = entityType === 'federation'
        ? federationEntityPath(entityId)
        : hostedEntityPath(entityId);

    await requestDelete(endpoint);

    if (ok.value) {
      deleteDialog.value = false;
      entityToDelete.value = null;
      await loadEntities();
    }
  } catch (error) {
    console.error('Error deleting entity:', error);
  } finally {
    deleting.value = false;
  }
}

onMounted(() => {
  loadEntities();
});
</script>
