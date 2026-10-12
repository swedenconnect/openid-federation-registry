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
  Renders the preview a step stores when it pauses for manual review: a list of typed sections. A step needs no
  frontend change to be reviewable, as long as it describes itself with these section types.
-->
<template>
  <div class="step-preview">
    <p v-if="!sections.length" class="text-grey text-body-2">This step has no preview.</p>
    <section
        v-for="(section, index) in sections"
        :key="index"
        class="preview-section"
        :aria-label="section.label"
    >
      <div class="d-flex align-baseline justify-space-between mb-2">
        <h4 class="section-label">{{ section.label }}</h4>
        <span v-if="showTypes" class="section-type">{{ section.type }}</span>
      </div>

      <dl v-if="section.type === 'FIELDS'" class="fields">
        <template v-for="(field, i) in section.fields ?? []" :key="i">
          <dt :class="{ 'mono': isParameter(field.name) }">{{ field.name }}</dt>
          <dd>{{ field.value }}</dd>
        </template>
      </dl>

      <ul v-else-if="section.type === 'CHECKS'" class="checks">
        <li v-for="(check, i) in section.checks ?? []" :key="i" class="d-flex align-baseline ga-2">
          <v-chip :color="checkColor(check.status)" size="x-small" label variant="outlined">
            {{ checkLabel(check.status) }}
          </v-chip>
          <span>{{ check.message }}</span>
        </li>
      </ul>

      <pre v-else-if="section.type === 'JSON'" class="json">{{ pretty(section.json) }}</pre>

      <ul v-else-if="section.type === 'LIST'" class="list">
        <li v-for="(item, i) in section.items ?? []" :key="i" class="mono">{{ item }}</li>
      </ul>

      <p v-else-if="section.type === 'TEXT'" class="mb-0">{{ section.text }}</p>

      <div v-else-if="section.type === 'IMAGE'" class="d-flex align-center ga-4 flex-wrap">
        <div class="image-box">
          <img v-if="section.image?.dataUri" :src="section.image.dataUri" :alt="`${section.label}: ${section.image.url}`">
          <span v-else class="text-caption text-grey">No image</span>
        </div>
        <div>
          <div class="mono">{{ section.image?.url }}</div>
          <div class="text-caption text-grey">{{ section.image?.note }}</div>
        </div>
      </div>

      <div v-else-if="section.type === 'DIFF'" class="overflow-x-auto">
        <p v-if="!(section.diff ?? []).length" class="text-grey text-body-2 mb-0">No changes.</p>
        <v-table v-else density="compact">
          <thead>
            <tr><th>Change</th><th>Parameter</th><th>Before</th><th>After</th></tr>
          </thead>
          <tbody>
            <tr v-for="(entry, i) in section.diff" :key="i">
              <td>
                <v-chip :color="diffColor(entry.changeType)" size="x-small" label variant="outlined">
                  {{ entry.changeType.toLowerCase() }}
                </v-chip>
              </td>
              <td class="mono">{{ entry.key }}</td>
              <td class="mono text-grey">{{ entry.before ?? '—' }}</td>
              <td class="mono">{{ entry.after ?? '—' }}</td>
            </tr>
          </tbody>
        </v-table>
      </div>

      <p v-else class="text-grey text-body-2 mb-0">Unknown section type {{ section.type }}.</p>
    </section>
  </div>
</template>

<script setup>
import {computed} from 'vue';

const props = defineProps({
  preview: {type: Object, default: null},
  // Shows each section's type next to its label, for operators setting up new steps.
  showTypes: {type: Boolean, default: false},
});

const sections = computed(() => props.preview?.sections ?? []);

function isParameter(name) {
  return /^[a-z_]+(#[a-z-]+)?$/i.test(name ?? '') && (name ?? '').includes('_');
}

function pretty(value) {
  try {
    return JSON.stringify(value, null, 2);
  } catch {
    return String(value);
  }
}

function checkColor(status) {
  return {OK: 'success', FAIL: 'error', INFO: 'info'}[status] ?? 'default';
}

function checkLabel(status) {
  return {OK: 'ok', FAIL: 'fail', INFO: 'info'}[status] ?? status;
}

function diffColor(changeType) {
  return {ADDED: 'success', REMOVED: 'error', CHANGED: 'info'}[changeType] ?? 'default';
}
</script>

<style scoped>
.preview-section {
  padding: 12px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}
.preview-section:first-child {
  border-top: none;
}
.section-label {
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.6);
}
.section-type {
  font-family: monospace;
  font-size: 0.7rem;
  color: rgb(var(--v-theme-info));
}
.fields {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: 4px 18px;
  margin: 0;
}
.fields dt {
  color: rgba(0, 0, 0, 0.6);
}
.fields dd {
  margin: 0;
  overflow-wrap: anywhere;
}
.checks, .list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.mono {
  font-family: monospace;
  overflow-wrap: anywhere;
}
.json {
  margin: 0;
  padding: 12px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 4px;
  overflow-x: auto;
  font-size: 0.8rem;
}
.image-box {
  width: 96px;
  height: 96px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 6px;
  display: grid;
  place-items: center;
  overflow: hidden;
}
.image-box img {
  max-width: 100%;
  max-height: 100%;
}
@media (max-width: 600px) {
  .fields {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
