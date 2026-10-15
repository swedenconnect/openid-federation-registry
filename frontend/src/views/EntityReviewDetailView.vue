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

<!-- One entity and everything it waits for, in review order: domain, trust marks, its own registration. -->
<template>
  <div>
    <v-breadcrumbs :items="breadcrumbs" class="px-0"></v-breadcrumbs>

    <v-card v-if="loading && !review">
      <v-card-text>
        <div role="status" aria-live="polite" class="text-center py-12">
          <v-progress-circular indeterminate color="primary" size="64" aria-hidden="true"></v-progress-circular>
          <p class="mt-4 text-grey">Loading entity...</p>
        </div>
      </v-card-text>
    </v-card>

    <v-card v-else-if="review">
      <v-card-text class="d-flex justify-space-between align-start flex-wrap ga-4">
        <div>
          <h2 class="mono entity-id">{{ review.entityId }}</h2>
          <div class="text-grey">
            {{ review.organizationName }} · {{ review.organizationNumber }} · submitted {{ formatDate(review.submittedAt) }}
            · {{ review.hosted ? 'hosted by the registry' : 'self-hosted Entity Configuration' }}
          </div>
          <div v-if="review.update" class="text-caption">
            Update of an entity in the federation: the published subordinate statement is kept until this registration
            is approved.
          </div>
          <div v-if="review.review && review.reviewState === 'IN_REVIEW'" class="text-caption text-grey">
            Review started {{ formatDate(review.review.startedAt) }} by {{ review.review.startedBy }}
          </div>
        </div>
        <div class="d-flex ga-2 align-center">
          <v-btn
              id="btn-registration-details"
              variant="text"
              @click="router.push({name: 'registration-detail', params: {id: review.registrationId}})"
          >
            Registration details
          </v-btn>
          <v-chip v-if="review.reviewState === 'COMPLETED'" color="success" label>Review completed</v-chip>
          <v-btn
              v-else
              id="btn-start-review"
              color="primary"
              @click="router.push({name: 'entity-review-session', params: {registrationId: review.registrationId}})"
          >
            {{ review.reviewState === 'IN_REVIEW' ? 'Continue review' : 'Start review' }}
          </v-btn>
        </div>
      </v-card-text>

      <template v-for="(group, groupIndex) in groups" :key="group.type">
        <v-divider></v-divider>
        <v-card-text class="pb-1">
          <div class="d-flex align-baseline ga-2">
            <span class="text-caption mono text-grey">{{ groupIndex + 1 }}</span>
            <h3 class="text-subtitle-1 font-weight-medium">{{ group.label }}</h3>
          </div>
        </v-card-text>
        <div v-for="item in group.items" :key="itemKey(item)" class="item-row">
          <div>
            <div :class="{ mono: item.type !== 'ENTITY' }">{{ item.title }}</div>
            <div class="text-caption text-grey">{{ item.subtitle }}</div>
          </div>
          <div class="text-caption text-grey">
            <span v-if="item.decidedInReviewOf">
              Decided in the review of <span class="mono">{{ item.decidedInReviewOf }}</span>
            </span>
            <span v-else-if="item.state === 'WAITING'">{{ item.waitingFor }}</span>
            <span v-else-if="item.state === 'UPCOMING'">Comes up after the previous step is approved</span>
            <span v-else-if="item.rejectionReason">{{ item.rejectionReason }}</span>
            <span v-else-if="item.decidedBy">{{ item.decidedBy }} · {{ formatDate(item.decidedAt) }}</span>
          </div>
          <div class="d-flex ga-1 justify-end">
            <v-chip v-if="item.deferred" size="small" label variant="outlined">Deferred</v-chip>
            <v-chip :color="stateColor(item.state)" size="small" label>{{ stateLabel(item.state) }}</v-chip>
          </div>
        </div>
      </template>
      <v-card-text v-if="!review.items.length" class="text-grey">Nothing to review for this entity.</v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {entityReviewPath} from '@/config/path';
import {GROUP_LABELS, itemKey, stateColor, stateLabel, formatDate} from '@/views/entityReview';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const {requestGet, loading} = useRequest();

const review = ref(null);

const breadcrumbs = computed(() => [
  {title: 'Registrations', to: {name: 'registrations-list'}},
  {title: review.value?.entityId ?? '…', disabled: true},
]);

const groups = computed(() => ['DOMAIN', 'TRUST_MARK', 'ENTITY']
    .map(type => ({type, label: GROUP_LABELS[type], items: (review.value?.items ?? []).filter(i => i.type === type)}))
    .filter(group => group.items.length > 0));

onMounted(async () => {
  review.value = await requestGet(
      entityReviewPath(userStore.selectedTenant, userStore.orgNumber, route.params.registrationId));
});
</script>

<style scoped>
.mono {
  font-family: monospace;
  overflow-wrap: anywhere;
}
.entity-id {
  font-size: 1.15rem;
  font-weight: 500;
}
.item-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) auto;
  gap: 8px 16px;
  align-items: center;
  padding: 10px 16px;
  border-top: 1px dashed rgba(0, 0, 0, 0.12);
}
@media (max-width: 700px) {
  .item-row {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
