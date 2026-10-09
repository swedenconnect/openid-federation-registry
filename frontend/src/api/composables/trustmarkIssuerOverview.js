/*
 * Copyright 2026 Sweden Connect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import {computed, ref} from 'vue';
import {adminPath, trustmarksListingPath} from '@/config/path';
import {useUserStore} from '@/stores/userStore';

// Silent on purpose: a failed lookup only means that no suggestion or status is shown.
async function getJson(path) {
    try {
        const response = await fetch(path, {credentials: 'include'});
        return response.ok ? await response.json() : null;
    } catch (e) {
        return null;
    }
}

/**
 * The trust mark issuers of the selected organization: the federation entities that have a trust mark issuer module,
 * whether it is active, and the trust mark types of their trust marks.
 *
 * A trust anchor lists issuers as entries {issuer, auto, trustMarkTypes}. With auto set the entry includes every trust
 * mark of the issuer, which has to be a trust mark issuer of the same organization, and the types are taken from the
 * issuer when the configuration is fetched. Without auto the trust mark types are listed in the entry and the issuer can
 * be any entity. This mirrors OidfApiService.toTrustMarkIssuers.
 */
export function useTrustmarkIssuerOverview() {
    const userStore = useUserStore();

    // Entity identifier of the issuer -> {id, active, types}
    const issuers = ref(new Map());
    const loaded = ref(false);

    async function load() {
        const response = await getJson(adminPath(userStore.selectedTenant, userStore.orgNumber)
            + '?type=federation&includemodules=true');
        const found = new Map();
        for (const entity of response?.federationEntity || []) {
            const module = entity.trustmarkIssuer;
            const id = module?.trustmarkIssuerId || module?.id;
            if (id && entity.entityIdentifier) {
                found.set(entity.entityIdentifier, {id, active: module.active === true, types: []});
            }
        }
        await Promise.all([...found.values()].map(async (issuer) => {
            const trustmarks = await getJson(trustmarksListingPath(userStore.selectedTenant, userStore.orgNumber, issuer.id));
            issuer.types = [...new Set((Array.isArray(trustmarks) ? trustmarks : [])
                .map((trustmark) => trustmark.trustmarkType)
                .filter(Boolean))].sort();
        }));
        issuers.value = found;
        loaded.value = true;
    }

    // The trust mark issuers of the organization, for suggestions.
    const ownIssuers = computed(() => [...issuers.value.keys()].sort());

    function isOwn(entityIdentifier) {
        return issuers.value.has(entityIdentifier);
    }

    // The trust mark types of one of the organization's issuers.
    function typesOf(entityIdentifier) {
        return issuers.value.get(entityIdentifier)?.types || [];
    }

    function isBlank(entry) {
        return !entry?.issuer || !entry.issuer.trim();
    }

    // The trust mark types an entry contributes to the claim, empty when it contributes nothing.
    function typesOfEntry(entry) {
        if (isBlank(entry)) return [];
        const issuer = entry.issuer.trim();
        if (entry.auto) {
            const own = issuers.value.get(issuer);
            return own && own.active ? own.types : [];
        }
        return [...new Set((entry.trustMarkTypes || []).filter((type) => type && type.trim()))];
    }

    /**
     * Explains what happens to an entry: {level, text} with level ok, warning or error, where error is something the
     * registry rejects when saving. Null when there is nothing to say yet.
     */
    function statusOf(entry) {
        if (isBlank(entry)) return null;
        const issuer = entry.issuer.trim();
        if (entry.auto) {
            if (!loaded.value) return null;
            const own = issuers.value.get(issuer);
            if (!own) {
                return {
                    level: 'error',
                    text: 'Auto needs a trust mark issuer of this organization. Turn auto off and list the trust mark types instead.',
                };
            }
            if (!own.active) {
                return {level: 'warning', text: 'The trust mark issuer is not active, so nothing is exported for it.'};
            }
            if (own.types.length === 0) {
                return {level: 'warning', text: 'The trust mark issuer has no trust marks yet. Its trust marks are included as soon as it has some.'};
            }
            return {
                level: 'ok',
                text: `Includes all ${own.types.length} trust mark type${own.types.length === 1 ? '' : 's'} of the issuer, and follows it when it changes.`,
            };
        }
        const types = typesOfEntry(entry);
        if (types.length === 0) {
            return {level: 'error', text: 'Add at least one trust mark type, or turn auto on.'};
        }
        return {level: 'ok', text: `Trusted for the ${types.length} listed trust mark type${types.length === 1 ? '' : 's'}.`};
    }

    /**
     * The trust_mark_issuers that the entries give: trust mark type -> issuers.
     */
    function trustedTypes(entries) {
        const byType = new Map();
        for (const entry of entries || []) {
            for (const type of typesOfEntry(entry)) {
                const issuer = entry.issuer.trim();
                const current = byType.get(type) || [];
                if (!current.includes(issuer)) byType.set(type, [...current, issuer]);
            }
        }
        return [...byType.entries()].sort(([a], [b]) => a.localeCompare(b));
    }

    return {issuers, loaded, load, ownIssuers, isOwn, typesOf, statusOf, trustedTypes};
}
