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
        <v-combobox
            :model-value="group.issuer"
            @update:model-value="(val) => updateIssuer(gi, val)"
            :items="issuerItems"
            item-title="value"
            item-value="value"
            :return-object="false"
            :hide-no-data="true"
            label="TrustMark Issuer"
            :hint="suggest
                ? 'Entity identifier URL of the issuer. Trustmark issuers in this organization are suggested.'
                : 'Entity identifier URL of the issuer'"
            persistent-hint
            density="compact"
            :disabled="disabled"
            class="flex-grow-1 mr-2"
        >
          <template #item="{ props: itemProps, item }">
            <v-list-item v-bind="itemProps" subtitle="Trustmark issuer in this organization"></v-list-item>
          </template>
        </v-combobox>
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
          <v-combobox
              :model-value="tm"
              @update:model-value="(val) => updateTrustmark(gi, ti, val)"
              :items="trustmarkItems(group.issuer)"
              :return-object="false"
              :hide-no-data="true"
              hint="Trustmark identifier URL"
              density="compact"
              variant="outlined"
              hide-details="auto"
              :disabled="disabled"
              class="flex-grow-1 mr-2"
          >
            <template v-if="status(group.issuer, tm)" #append-inner>
              <v-tooltip :text="status(group.issuer, tm).text" location="top">
                <template #activator="{ props: tipProps }">
                  <v-icon
                      v-bind="tipProps"
                      :color="status(group.issuer, tm).ok ? 'success' : 'error'"
                      tabindex="0"
                      :aria-label="status(group.issuer, tm).text"
                  >{{ status(group.issuer, tm).ok ? 'mdi-check-circle' : 'mdi-close-circle' }}</v-icon>
                </template>
              </v-tooltip>
            </template>
          </v-combobox>
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
import {computed, onMounted, ref, watch} from 'vue';
import {useUserStore} from '@/stores/userStore';
import {adminPath, trustmarksListingPath, trustmarksPath} from '@/config/path';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  disabled: {
    type: Boolean,
    default: false,
  },
  // Suggest the organization's own trustmark issuers and their trustmarks, and mark whether a trustmark has been
  // assigned to `subject` (the entity identifier of the entity the sources belong to).
  suggest: {
    type: Boolean,
    default: false,
  },
  subject: {
    type: String,
    default: '',
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


// --- Suggestions and assignment status (only when `suggest` is set) ---------------------------------------------

const userStore = useUserStore();

// Entity identifier of a trustmark issuer in this organization -> its id.
const issuers = ref(new Map());
// Issuer entity identifier -> [{trustmarkId, trustmarkType}]
const trustmarksByIssuer = ref(new Map());
// trustmarkId -> subjects of that trustmark
const subjectsByTrustmark = ref(new Map());
const requested = new Set();

const issuerItems = computed(() => props.suggest
    ? [...issuers.value.keys()].sort((a, b) => a.localeCompare(b)).map((value) => ({value}))
    : []);

function trustmarkItems(issuer) {
  return (trustmarksByIssuer.value.get(issuer) || []).map((t) => t.trustmarkType).sort((a, b) => a.localeCompare(b));
}

// Silent on purpose: a failed lookup only means that no suggestion or status is shown.
async function getJson(path) {
  try {
    const response = await fetch(path, {credentials: 'include'});
    return response.ok ? await response.json() : null;
  } catch (e) {
    return null;
  }
}

async function loadIssuers() {
  const response = await getJson(adminPath(userStore.selectedTenant, userStore.orgNumber)
      + '?type=federation&includemodules=true');
  const found = new Map();
  for (const entity of response?.federationEntity || []) {
    const id = entity.trustmarkIssuer?.trustmarkIssuerId || entity.trustmarkIssuer?.id;
    if (id && entity.entityIdentifier) found.set(entity.entityIdentifier, id);
  }
  issuers.value = found;
}

async function ensureTrustmarks(issuer) {
  const issuerId = issuers.value.get(issuer);
  const key = 'tm:' + issuer;
  if (!issuerId || requested.has(key)) return;
  requested.add(key);
  const response = await getJson(trustmarksListingPath(userStore.selectedTenant, userStore.orgNumber, issuerId));
  trustmarksByIssuer.value = new Map(trustmarksByIssuer.value)
      .set(issuer, Array.isArray(response) ? response : []);
}

async function ensureSubjects(trustmarkId) {
  const key = 'sub:' + trustmarkId;
  if (requested.has(key)) return;
  requested.add(key);
  const response = await getJson(`${trustmarksPath(userStore.selectedTenant, userStore.orgNumber)}/${trustmarkId}/subjects`);
  subjectsByTrustmark.value = new Map(subjectsByTrustmark.value)
      .set(trustmarkId, Array.isArray(response) ? response : []);
}

function findTrustmark(issuer, type) {
  return (trustmarksByIssuer.value.get(issuer) || []).find((t) => t.trustmarkType === type);
}

function isValidSubject(entry, now) {
  if (entry.revoked) return false;
  if (entry.granted && new Date(entry.granted) > now) return false;
  return !(entry.expires && new Date(entry.expires) <= now);
}

// null: nothing to say (not suggesting, an issuer outside this organization, or nothing entered yet / still loading).
function status(issuer, type) {
  if (!props.suggest || !props.subject || !type || !issuers.value.has(issuer)) return null;
  const trustmarks = trustmarksByIssuer.value.get(issuer);
  if (!trustmarks) return null;
  const trustmark = findTrustmark(issuer, type);
  if (!trustmark) return {ok: false, text: 'This issuer has no trustmark with this identifier'};
  const subjects = subjectsByTrustmark.value.get(trustmark.trustmarkId);
  if (!subjects) return null;
  const entries = subjects.filter((entry) => entry.subject === props.subject);
  if (entries.length === 0) return {ok: false, text: 'The trustmark is not assigned to this entity'};
  if (entries.some((entry) => isValidSubject(entry, new Date()))) {
    return {ok: true, text: 'The trustmark is assigned to this entity'};
  }
  return {ok: false, text: 'The trustmark is assigned to this entity but is revoked, expired or not yet granted'};
}

// Load what the entered values need: the trustmarks of a chosen issuer (also the suggestions for its trustmark
// fields) and the subjects of each trustmark that is entered.
watch([groups, issuers, trustmarksByIssuer], () => {
  if (!props.suggest) return;
  for (const group of groups.value) {
    if (!issuers.value.has(group.issuer)) continue;
    ensureTrustmarks(group.issuer).then(() => {
      for (const type of group.trustmarks) {
        const trustmark = findTrustmark(group.issuer, type);
        if (trustmark) ensureSubjects(trustmark.trustmarkId);
      }
    });
  }
}, {deep: true});

onMounted(() => {
  if (props.suggest) loadIssuers();
});
</script>
