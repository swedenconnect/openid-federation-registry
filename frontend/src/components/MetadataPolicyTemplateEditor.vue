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
  Edits the Metadata Policy Creation step's template: per entity type, the rules that decide what goes into the
  Subordinate Statement's metadata and metadata_policy claims, and what is checked. The value is the template as a
  JSON string; the server checks it again when the flow is saved. See docs/metadata_policy.md.
-->
<template>
  <div class="template-editor">
    <div class="d-flex justify-end ga-2 mb-2">
      <v-btn v-if="preset" id="btn-template-preset" size="small" variant="text" @click="loadPreset">
        Load default template
      </v-btn>
      <v-menu v-if="configuredTemplates.length > 0">
        <template #activator="{ props: menuProps }">
          <v-btn id="btn-template-configured" size="small" variant="text" v-bind="menuProps">
            Load template
            <v-icon end>mdi-menu-down</v-icon>
          </v-btn>
        </template>
        <v-list density="compact">
          <v-list-item
              v-for="configured in configuredTemplates"
              :key="configured.name"
              :title="configured.name"
              :subtitle="[configured.description, configured.omitted?.length
                ? `${configured.omitted.length} left out, not supported yet` : null].filter(Boolean).join(' · ')"
              @click="loadConfigured(configured)"
          ></v-list-item>
        </v-list>
      </v-menu>
      <v-btn id="btn-template-json" size="small" variant="text" @click="toggleRaw">
        {{ raw ? 'Edit as rules' : 'Edit as JSON' }}
      </v-btn>
    </div>

    <v-alert
        v-if="leftOut.length > 0"
        id="template-left-out"
        type="warning"
        variant="tonal"
        density="compact"
        class="mb-2"
    >
      Left out of {{ leftOutFrom }}, not supported by the registry yet:
      <ul class="pl-4">
        <li v-for="entry in leftOut" :key="entry" class="mono">{{ entry }}</li>
      </ul>
    </v-alert>

    <template v-if="raw || !template">
      <v-textarea
          id="textarea-template-json"
          :model-value="rawText"
          label="Template (JSON)"
          rows="16"
          class="mono"
          :error-messages="parseError"
          @update:model-value="onRawInput"
      ></v-textarea>
    </template>

    <template v-else>
      <section aria-labelledby="template-general">
        <h4 id="template-general" class="section-label">General</h4>
        <div class="d-flex flex-wrap ga-4">
          <v-text-field
              v-model="template.default_language"
              label="Default language"
              hint="Copied to the parameter without language tag"
              persistent-hint
              density="compact"
              style="max-width: 180px"
          ></v-text-field>
          <v-text-field
              :model-value="(template.languages ?? []).join(', ')"
              label="Required languages"
              hint="For multilingual parameters, comma separated"
              persistent-hint
              density="compact"
              style="max-width: 240px"
              @update:model-value="value => template.languages = splitList(value, ',')"
          ></v-text-field>
          <v-select
              v-model="template.on_failure"
              :items="ON_FAILURE"
              label="If a check fails or a required value is missing"
              density="compact"
              style="max-width: 320px"
          ></v-select>
        </div>
      </section>

      <section aria-labelledby="template-rules" class="mt-4">
        <div class="d-flex align-center justify-space-between">
          <h4 id="template-rules" class="section-label">Rules per entity type</h4>
          <v-menu>
            <template #activator="{ props: menuProps }">
              <v-btn v-bind="menuProps" size="small" variant="text" :disabled="!addableEntityTypes.length">
                + Entity type
              </v-btn>
            </template>
            <v-list density="compact">
              <v-list-item v-for="type in addableEntityTypes" :key="type" @click="addEntityType(type)">
                <v-list-item-title class="mono">{{ type }}</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </div>

        <v-tabs v-model="activeType" density="compact" color="primary" show-arrows>
          <v-tab v-for="type in entityTypes" :key="type" :value="type" class="mono text-none">{{ type }}</v-tab>
        </v-tabs>

        <div v-if="activeType" class="overflow-x-auto">
          <v-table density="compact" class="rules-table">
            <thead>
              <tr>
                <th>Parameter</th><th>Claim</th><th>Operator</th><th>Value source</th><th>Languages</th>
                <th>Checks</th><th><span class="sr-only">Remove</span></th>
              </tr>
            </thead>
            <tbody>
              <tr
                  v-for="(rule, index) in activeRules"
                  :key="index"
                  class="rule-row"
                  :class="{ selected: index === selectedRule }"
                  tabindex="0"
                  @click="selectedRule = index"
                  @keydown.enter.prevent="selectedRule = index"
              >
                <td class="mono">{{ rule.parameter }}<span v-if="rule.required" title="required"> *</span></td>
                <td>
                  <v-chip size="x-small" label :color="rule.claim === 'metadata' ? 'primary' : 'secondary'">
                    {{ rule.claim }}
                  </v-chip>
                </td>
                <td class="mono">{{ rule.claim === 'metadata' ? '—' : (rule.operator ?? 'value') }}</td>
                <td>{{ sourceLabel(rule) }}</td>
                <td>{{ rule.multilingual ? languagesLabel(rule) : '—' }}</td>
                <td>
                  <div class="d-flex flex-wrap ga-1">
                    <v-chip v-for="(check, i) in rule.checks ?? []" :key="i" size="x-small" variant="outlined" label>
                      {{ checkLabel(check) }}
                    </v-chip>
                  </div>
                </td>
                <td>
                  <v-btn
                      icon="mdi-close"
                      size="x-small"
                      variant="text"
                      :aria-label="`Remove rule ${rule.parameter}`"
                      @click.stop="removeRule(index)"
                  ></v-btn>
                </td>
              </tr>
              <tr v-if="!activeRules.length">
                <td colspan="7" class="text-grey">No rules for this entity type.</td>
              </tr>
            </tbody>
          </v-table>
          <div class="d-flex justify-space-between">
            <v-btn size="small" variant="text" @click="addRule">+ Add rule</v-btn>
            <v-btn size="small" variant="text" color="error" @click="removeEntityType">Remove entity type</v-btn>
          </div>
        </div>
        <p v-else class="text-grey text-body-2">Add an entity type, or load the preset.</p>

        <!-- Selected rule -->
        <div v-if="rule" class="rule-editor">
          <v-text-field v-model="rule.parameter" label="Parameter" density="compact" class="mono"
                        hint="Without language tag" persistent-hint></v-text-field>
          <v-select v-model="rule.claim" :items="CLAIMS" label="Claim" density="compact"
                    @update:model-value="onClaimChange"></v-select>
          <v-select v-if="rule.claim === 'metadata_policy'" v-model="rule.operator" :items="OPERATORS"
                    label="Policy operator" density="compact"></v-select>
          <v-select v-model="rule.source" :items="SOURCES" label="Value source" density="compact"
                    @update:model-value="onSourceChange"></v-select>
          <v-select v-if="rule.source === 'organization'" v-model="rule.organization_field" :items="ORG_FIELDS"
                    label="Organization field" density="compact"></v-select>
          <v-text-field v-if="rule.source === 'organization' && rule.organization_field === 'organization_identifier'"
                        v-model="rule.icd" label="ISO 6523 ICD" placeholder="0007" density="compact"></v-text-field>
          <v-text-field v-if="rule.source === 'entity'" :model-value="rule.fallback ?? ''"
                        label="Fallback parameter if absent" placeholder="e.g. client_name" density="compact"
                        class="mono" @update:model-value="value => setOptional(rule, 'fallback', value)"></v-text-field>
          <v-text-field v-if="rule.source === 'static'" :model-value="staticText(rule)" class="full mono"
                        label="Static value" hint="Text, or JSON for lists and true/false" persistent-hint
                        density="compact" @update:model-value="value => rule.static_value = parseStatic(value)"></v-text-field>
          <v-checkbox v-model="rule.required" label="Required" density="compact" hide-details></v-checkbox>
          <v-checkbox v-model="rule.multilingual" label="Multilingual" density="compact" hide-details
                      :disabled="rule.source !== 'entity'"></v-checkbox>
          <v-select v-if="rule.multilingual" v-model="rule.untagged" :items="UNTAGGED" label="Untagged value"
                    density="compact"></v-select>
          <v-select v-if="rule.multilingual && rule.untagged === 'organization'" v-model="rule.untagged_organization_field"
                    :items="ORG_FIELDS" label="Untagged organization field" density="compact"></v-select>

          <div class="full">
            <div class="text-caption text-grey mb-1">Checks before registration</div>
            <div class="d-flex flex-wrap ga-4">
              <v-checkbox
                  v-for="check in CHECKS"
                  :key="check.value"
                  :model-value="hasCheck(rule, check.value)"
                  :label="check.title"
                  density="compact"
                  hide-details
                  @update:model-value="on => toggleCheck(rule, check.value, on)"
              ></v-checkbox>
            </div>
            <div v-for="check in checksWithOptions(rule)" :key="check.type" class="check-options">
              <span class="font-weight-medium">{{ checkTitle(check.type) }}</span>
              <template v-if="check.type === 'matches_organization'">
                <v-select v-model="check.organization_field" :items="ORG_FIELDS" label="Organization field"
                          density="compact" hide-details style="max-width: 280px"></v-select>
                <v-text-field v-if="check.organization_field === 'organization_identifier'" v-model="check.icd"
                              label="ISO 6523 ICD" placeholder="0007" density="compact" hide-details
                              style="max-width: 140px"></v-text-field>
              </template>
              <v-textarea v-else-if="check.type === 'allowed_values'" :model-value="(check.values ?? []).join('\n')"
                          label="Allowed values (one per line)" rows="2" auto-grow density="compact" hide-details
                          class="mono flex-grow-1"
                          @update:model-value="value => check.values = splitList(value, '\n')"></v-textarea>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import {computed, onMounted, ref, watch} from 'vue';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {templatesPath} from '@/config/path';

const props = defineProps({
  modelValue: {type: String, default: ''},
  // The step's default template, offered as "Load default template".
  preset: {type: String, default: ''},
});
const emit = defineEmits(['update:modelValue']);

const ENTITY_TYPES = ['openid_relying_party', 'openid_provider', 'federation_entity', 'oauth_client',
  'oauth_authorization_server', 'oauth_resource'];
const CLAIMS = [{title: 'metadata (set the value)', value: 'metadata'},
  {title: 'metadata_policy (constrain with an operator)', value: 'metadata_policy'}];
const OPERATORS = ['value', 'add', 'default', 'one_of', 'subset_of', 'superset_of', 'essential'];
const SOURCES = [{title: "Entity's metadata", value: 'entity'},
  {title: 'Organization data (registry)', value: 'organization'}, {title: 'Static value', value: 'static'}];
const ORG_FIELDS = [{title: 'Legal name', value: 'legal_name'},
  {title: 'Organization number', value: 'organization_number'},
  {title: 'Organization identifier (GLUE-URI)', value: 'organization_identifier'}];
const UNTAGGED = [{title: 'No untagged value', value: 'none'},
  {title: 'Copy from default language', value: 'default_language'},
  {title: "Entity's own, else default language", value: 'entity_or_default'},
  {title: 'Organization data (registry)', value: 'organization'}];
const ON_FAILURE = [{title: 'Fail registration', value: 'fail'}, {title: 'Show reviewer as a warning', value: 'review'}];
const CHECKS = [{title: 'Not empty', value: 'non_empty'}, {title: 'HTTPS URL', value: 'https_url'},
  {title: 'Resolves to an image', value: 'image'}, {title: 'Matches organization data', value: 'matches_organization'},
  {title: 'Within allowed values', value: 'allowed_values'}];

const template = ref(null);
const raw = ref(false);
const rawText = ref(props.modelValue);
const parseError = ref('');
const activeType = ref(null);
const selectedRule = ref(0);

function parse(text) {
  if (!text || !text.trim()) {
    return {default_language: 'sv', languages: ['sv', 'en'], on_failure: 'fail', entity_types: {}};
  }
  return JSON.parse(text);
}

function load(text) {
  rawText.value = text;
  try {
    template.value = parse(text);
    template.value.entity_types ??= {};
    parseError.value = '';
    activeType.value = Object.keys(template.value.entity_types)[0] ?? null;
    selectedRule.value = 0;
  } catch (e) {
    template.value = null;
    parseError.value = `Not valid JSON: ${e.message}`;
  }
}

load(props.modelValue);

// Emit on every change made through the rules editor.
watch(template, value => {
  if (value && !raw.value) {
    const text = JSON.stringify(value, null, 2);
    rawText.value = text;
    emit('update:modelValue', text);
  }
}, {deep: true});

function onRawInput(text) {
  rawText.value = text;
  emit('update:modelValue', text);
  try {
    JSON.parse(text);
    parseError.value = '';
  } catch (e) {
    parseError.value = `Not valid JSON: ${e.message}`;
  }
}

function toggleRaw() {
  if (raw.value) {
    load(rawText.value);
    if (template.value) raw.value = false;
  } else {
    raw.value = true;
  }
}

// Step templates configured in the registry, offered under "Load template".
const configuredTemplates = ref([]);
// What the registry left out of the configured template loaded last.
const leftOut = ref([]);
const leftOutFrom = ref('');
const {requestGet} = useRequest();
const userStore = useUserStore();

onMounted(async () => {
  const response = await requestGet(templatesPath(userStore.selectedTenant, userStore.orgNumber, 'STEP'));
  configuredTemplates.value = Array.isArray(response) ? response : [];
});

function loadConfigured(configured) {
  leftOut.value = configured.omitted ?? [];
  leftOutFrom.value = configured.name;
  raw.value = false;
  load(configured.content);
  emit('update:modelValue', configured.content);
}

function loadPreset() {
  leftOut.value = [];
  raw.value = false;
  load(props.preset);
  emit('update:modelValue', props.preset);
}

const entityTypes = computed(() => Object.keys(template.value?.entity_types ?? {}));
const addableEntityTypes = computed(() => ENTITY_TYPES.filter(type => !entityTypes.value.includes(type)));
const activeRules = computed(() => template.value?.entity_types?.[activeType.value] ?? []);
const rule = computed(() => activeRules.value[selectedRule.value] ?? null);

watch(activeType, () => {
  selectedRule.value = 0;
});

function addEntityType(type) {
  template.value.entity_types[type] = [];
  activeType.value = type;
}

function removeEntityType() {
  delete template.value.entity_types[activeType.value];
  activeType.value = entityTypes.value[0] ?? null;
}

function addRule() {
  activeRules.value.push({parameter: '', claim: 'metadata_policy', operator: 'value', source: 'entity', checks: []});
  selectedRule.value = activeRules.value.length - 1;
}

function removeRule(index) {
  activeRules.value.splice(index, 1);
  selectedRule.value = Math.max(0, Math.min(selectedRule.value, activeRules.value.length - 1));
}

function onClaimChange(claim) {
  if (claim === 'metadata') delete rule.value.operator;
  else rule.value.operator ??= 'value';
}

function onSourceChange(source) {
  if (source !== 'entity') {
    delete rule.value.fallback;
    rule.value.multilingual = false;
  }
  if (source === 'organization') rule.value.organization_field ??= 'legal_name';
  if (source !== 'organization') {
    delete rule.value.organization_field;
    delete rule.value.icd;
  }
  if (source !== 'static') delete rule.value.static_value;
}

function setOptional(target, key, value) {
  if (value) target[key] = value;
  else delete target[key];
}

function staticText(target) {
  const value = target.static_value;
  return value === undefined || value === null ? '' : typeof value === 'string' ? value : JSON.stringify(value);
}

function parseStatic(text) {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function hasCheck(target, type) {
  return (target.checks ?? []).some(check => check.type === type);
}

function toggleCheck(target, type, on) {
  target.checks = (target.checks ?? []).filter(check => check.type !== type);
  if (on) {
    const check = {type};
    if (type === 'matches_organization') check.organization_field = 'organization_identifier';
    if (type === 'allowed_values') check.values = [];
    target.checks.push(check);
  }
}

function checksWithOptions(target) {
  return (target.checks ?? []).filter(check => check.type === 'matches_organization' || check.type === 'allowed_values');
}

function checkTitle(type) {
  return CHECKS.find(check => check.value === type)?.title ?? type;
}

function checkLabel(check) {
  if (check.type === 'matches_organization') {
    const field = ORG_FIELDS.find(f => f.value === check.organization_field)?.title ?? check.organization_field;
    return `Matches org ${field?.toLowerCase()}${check.organization_field === 'organization_identifier'
        ? ` (ICD ${check.icd || '0007'})` : ''}`;
  }
  if (check.type === 'allowed_values') return `Allowed values (${(check.values ?? []).length})`;
  return checkTitle(check.type);
}

function sourceLabel(target) {
  if (target.source === 'organization') {
    return `Organization: ${ORG_FIELDS.find(f => f.value === target.organization_field)?.title ?? '?'}`;
  }
  if (target.source === 'static') return `Static: ${staticText(target)}`;
  return target.fallback ? `Entity's metadata, else ${target.fallback}` : "Entity's metadata";
}

function languagesLabel(target) {
  const tags = (template.value.languages?.length ? template.value.languages : ['sv', 'en']).map(l => `#${l}`).join(' ');
  return target.untagged === 'none' ? tags : `${tags} + untagged`;
}

function splitList(text, separator) {
  return (text ?? '').split(separator).map(part => part.trim()).filter(Boolean);
}
</script>

<style scoped>
.mono {
  font-family: monospace;
}
.section-label {
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(0, 0, 0, 0.6);
  margin-bottom: 8px;
}
.rules-table {
  min-width: 720px;
}
.rule-row {
  cursor: pointer;
}
.rule-row.selected td {
  background: rgba(0, 0, 0, 0.04);
}
.rule-row.selected td:first-child {
  box-shadow: inset 3px 0 0 rgb(var(--v-theme-primary));
}
.rule-editor {
  margin-top: 12px;
  padding: 14px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: 4px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 4px 18px;
}
.rule-editor .full {
  grid-column: 1 / -1;
}
.check-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  align-items: center;
  margin-top: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 4px;
}
</style>
