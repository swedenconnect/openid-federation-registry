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
  Reviews one entity item by item: domain, then trust marks, then the steps of its registration. Opening the view
  starts the review, or continues the open one. "Defer decision" leaves the current item undecided and goes back to
  the list, where the entity shows "Continue review".
-->
<template>
  <div>
    <v-breadcrumbs :items="breadcrumbs" class="px-0"></v-breadcrumbs>

    <v-card v-if="!review">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular v-if="loading" indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">{{ loading ? 'Opening review...' : 'The review could not be opened.' }}</p>
        </div>
      </v-card-text>
    </v-card>

    <template v-else>
      <div class="d-flex justify-space-between align-center flex-wrap ga-2 mb-3">
        <h2>Review <span class="mono entity-id">{{ review.entityId }}</span></h2>
        <v-switch
            id="switch-section-types"
            v-model="showTypes"
            label="Show preview section types"
            density="compact"
            hide-details
            color="primary"
            class="flex-grow-0"
        ></v-switch>
      </div>

      <!-- Done -->
      <v-card v-if="!current">
        <v-card-title>{{ endedByRejection ? 'Review ended' : 'Review complete' }}</v-card-title>
        <v-card-text>
          <p class="text-grey">
            {{ endedByRejection
              ? 'The registration was rejected; nothing more can be decided for this entity.'
              : 'Every item is decided. Approved registrations continue to the publish step.' }}
          </p>
          <ul class="summary">
            <li v-for="item in review.items" :key="itemKey(item)" class="d-flex ga-2 align-baseline">
              <v-chip :color="stateColor(item.state)" size="x-small" label>{{ stateLabel(item.state) }}</v-chip>
              <span :class="{ mono: item.type !== 'ENTITY' }">{{ item.title }}</span>
            </li>
          </ul>
        </v-card-text>
        <v-card-actions>
          <v-btn color="primary" variant="flat" @click="router.push({name: 'registrations-list'})">
            Back to registrations
          </v-btn>
        </v-card-actions>
      </v-card>

      <div v-else class="review-grid">
        <v-card class="stepper" tag="nav" aria-label="Review items">
          <template v-for="group in groups" :key="group.type">
            <div class="group-label">{{ group.label }}</div>
            <div
                v-for="item in group.items"
                :key="itemKey(item)"
                class="stepper-item"
                :class="{ current: itemKey(item) === itemKey(current), upcoming: item.state === 'UPCOMING' }"
                :aria-current="itemKey(item) === itemKey(current) ? 'step' : undefined"
            >
              <v-icon :color="dotColor(item)" size="small" class="mt-1">{{ dotIcon(item) }}</v-icon>
              <div class="min-w-0">
                <div :class="{ mono: item.type !== 'ENTITY' }" class="item-title">{{ item.title }}</div>
                <div class="text-caption text-grey">{{ stepperNote(item) }}</div>
              </div>
            </div>
          </template>
        </v-card>

        <v-card class="min-w-0">
          <v-card-text class="pb-0">
            <div class="d-flex align-center ga-2 mb-1">
              <v-chip size="small" label>{{ ITEM_LABELS[current.type] }}</v-chip>
              <span class="text-caption text-grey">{{ current.subtitle }}</span>
              <v-chip v-if="current.deferred" size="x-small" label variant="outlined">Deferred earlier</v-chip>
            </div>
            <h3 :class="{ mono: current.type !== 'ENTITY' }" class="current-title">{{ current.title }}</h3>
            <p v-if="current.type === 'ENTITY'" class="text-caption text-grey mb-0">
              Stored when the step paused: what will be written if you approve.
            </p>
            <v-alert v-if="current.state === 'WAITING'" type="info" variant="tonal" density="compact" class="mt-3">
              {{ current.waitingFor }}
            </v-alert>
            <v-alert v-if="hasFailedChecks" type="warning" variant="tonal" density="compact" class="mt-3">
              Some checks failed. They were accepted for review; reject if they matter.
            </v-alert>
          </v-card-text>

          <v-card-text>
            <StepPreview :preview="current.preview" :show-types="showTypes"></StepPreview>
          </v-card-text>

          <v-card-text v-if="rejecting" class="reject-box">
            <v-textarea
                id="textarea-reject-reason"
                v-model="reason"
                label="Reason for rejecting (sent to the organization)"
                rows="3"
                auto-grow
                autofocus
                hide-details="auto"
                :error-messages="reasonError"
            ></v-textarea>
            <v-alert type="warning" variant="tonal" density="compact" class="mt-3">{{ rejectConsequence }}</v-alert>
            <div class="d-flex ga-2 mt-3">
              <v-btn id="btn-confirm-reject" color="error" variant="flat" :loading="rejectingBusy" @click="decide('REJECTED')">
                Reject
              </v-btn>
              <v-btn variant="text" :disabled="deciding" @click="cancelReject">Cancel</v-btn>
            </div>
          </v-card-text>

          <v-divider></v-divider>
          <v-card-actions class="actions">
            <v-btn
                id="btn-reject"
                color="error"
                variant="outlined"
                :disabled="rejecting || deciding"
                @click="rejecting = true"
            >
              Reject…
            </v-btn>
            <v-btn id="btn-defer" variant="outlined" :disabled="deciding" :loading="deferring" @click="defer">
              Defer decision
            </v-btn>
            <v-spacer></v-spacer>
            <v-btn
                id="btn-approve"
                color="primary"
                variant="flat"
                :disabled="current.state !== 'PENDING' || deciding"
                :loading="approving"
                @click="decide('APPROVED')"
            >
              Approve and continue
            </v-btn>
          </v-card-actions>
        </v-card>
      </div>
    </template>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {useErrorStore} from '@/stores/errorStore';
import {entityReviewDecisionsPath, entityReviewStartPath} from '@/config/path';
import StepPreview from '@/components/StepPreview.vue';
import {GROUP_LABELS, ITEM_LABELS, itemKey, stateColor, stateLabel} from '@/views/entityReview';

const SHOW_TYPES_STORAGE_KEY = 'oidf.review.showSectionTypes';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const errorStore = useErrorStore();
const {requestPost, loading, ok} = useRequest();

const review = ref(null);
const rejecting = ref(false);
const reason = ref('');
const reasonError = ref('');
const approving = ref(false);
const deferring = ref(false);
const rejectingBusy = ref(false);
const deciding = computed(() => approving.value || deferring.value || rejectingBusy.value);
const showTypes = ref(readShowTypes());

const registrationId = computed(() => route.params.registrationId);

const breadcrumbs = computed(() => [
  {title: 'Registrations', to: {name: 'registrations-list'}},
  {title: review.value?.entityId ?? '…', to: {name: 'entity-review-detail', params: {registrationId: registrationId.value}}},
  {title: 'Review', disabled: true},
]);

const groups = computed(() => ['DOMAIN', 'TRUST_MARK', 'ENTITY']
    .map(type => ({type, label: GROUP_LABELS[type], items: (review.value?.items ?? []).filter(i => i.type === type)}))
    .filter(group => group.items.length > 0));

// The first item that can be decided now; failing that, the first that waits (it shows why).
const current = computed(() => {
  const items = review.value?.items ?? [];
  return items.find(item => item.state === 'PENDING') ?? items.find(item => item.state === 'WAITING') ?? null;
});

const endedByRejection = computed(() => review.value?.registrationStatus === 'REJECTED');

const hasFailedChecks = computed(() => (current.value?.preview?.sections ?? [])
    .some(section => section.type === 'CHECKS' && (section.checks ?? []).some(check => check.status === 'FAIL')));

const rejectConsequence = computed(() => {
  switch (current.value?.type) {
    case 'DOMAIN':
      return `Rejecting ${current.value.title} also rejects every pending registration under it that no other `
          + 'domain of the organization covers, with reason "Not Accepted Domain". This review ends.';
    case 'TRUST_MARK':
      return 'Only this trust mark request is rejected. The review continues with the next item.';
    default:
      return 'The entity registration is rejected. This review ends.';
  }
});

function readShowTypes() {
  try {
    return globalThis.localStorage?.getItem(SHOW_TYPES_STORAGE_KEY) === 'true';
  } catch {
    return false;
  }
}

function dotIcon(item) {
  if (item.state === 'APPROVED') return 'mdi-check-circle';
  if (item.state === 'REJECTED') return 'mdi-close-circle';
  if (current.value && itemKey(item) === itemKey(current.value)) return 'mdi-record-circle';
  return 'mdi-circle-outline';
}

function dotColor(item) {
  if (item.state === 'APPROVED') return 'success';
  if (item.state === 'REJECTED') return 'error';
  if (current.value && itemKey(item) === itemKey(current.value)) return 'primary';
  return 'grey';
}

function stepperNote(item) {
  if (item.decidedInReviewOf) return `${stateLabel(item.state)} in the review of ${item.decidedInReviewOf}`;
  if (item.state === 'APPROVED' || item.state === 'REJECTED') return stateLabel(item.state);
  if (item.state === 'UPCOMING') return 'Runs after the previous step';
  if (current.value && itemKey(item) === itemKey(current.value)) return 'Reviewing now';
  if (item.deferred) return 'Deferred';
  return item.subtitle;
}

function cancelReject() {
  rejecting.value = false;
  reason.value = '';
  reasonError.value = '';
}

async function post(decision) {
  const item = current.value;
  const response = await requestPost(
      entityReviewDecisionsPath(userStore.selectedTenant, userStore.orgNumber, registrationId.value), {
        itemType: item.type,
        itemId: item.itemId,
        stepIndex: item.stepIndex ?? null,
        decision,
        reason: decision === 'REJECTED' ? reason.value.trim() : null,
      });
  return ok.value ? response : null;
}

async function decide(decision) {
  if (decision === 'REJECTED' && !reason.value.trim()) {
    reasonError.value = 'Give a reason. It is sent to the organization.';
    return;
  }
  approving.value = decision === 'APPROVED';
  rejectingBusy.value = decision === 'REJECTED';
  try {
    const updated = await post(decision);
    if (updated) {
      review.value = updated;
      cancelReject();
    }
  } finally {
    approving.value = false;
    rejectingBusy.value = false;
  }
}

async function defer() {
  deferring.value = true;
  try {
    if (await post('DEFERRED')) {
      router.push({name: 'registrations-list'});
    }
  } finally {
    deferring.value = false;
  }
}

onMounted(async () => {
  errorStore.clearError();
  const started = await requestPost(
      entityReviewStartPath(userStore.selectedTenant, userStore.orgNumber, registrationId.value));
  review.value = ok.value ? started : null;
});

watch(showTypes, value => {
  try {
    globalThis.localStorage?.setItem(SHOW_TYPES_STORAGE_KEY, String(value));
  } catch {
    // storage unavailable — the choice does not survive the reload
  }
});
</script>

<style scoped>
.mono {
  font-family: monospace;
  overflow-wrap: anywhere;
}
.min-w-0 {
  min-width: 0;
}
.entity-id {
  font-size: 1.05rem;
}
.review-grid {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}
@media (max-width: 960px) {
  .review-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
.stepper {
  padding: 8px 0;
}
.group-label {
  font-size: 0.7rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.6);
  padding: 12px 16px 4px;
}
.stepper-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 2px 10px;
  padding: 8px 16px;
}
.stepper-item.current {
  background: rgba(0, 0, 0, 0.04);
  box-shadow: inset 3px 0 0 rgb(var(--v-theme-primary));
}
.stepper-item.upcoming {
  opacity: 0.55;
}
.item-title {
  font-size: 0.85rem;
  overflow-wrap: anywhere;
}
.current-title {
  font-size: 1.1rem;
  font-weight: 500;
  overflow-wrap: anywhere;
}
.reject-box {
  background: rgba(0, 0, 0, 0.03);
}
.actions {
  position: sticky;
  bottom: 0;
  background: rgb(var(--v-theme-surface));
  flex-wrap: wrap;
}
.summary {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
</style>
