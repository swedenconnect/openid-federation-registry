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

import {defineStore} from 'pinia';
import {ref} from 'vue';
import {useRequest} from '@/api/composables/request';
import {useUserStore} from '@/stores/userStore';
import {entityReviewCountPath} from '@/config/path';

// The number on the Registrations tab: what waits for this operator to act, counted by the server the same way the
// Registrations view counts its rows. It lives in a store so that every view that decides something can refresh it,
// instead of leaving the badge at the number it had when the page was loaded (issue #408).
//
// A failure leaves the number at 0: the badge is then absent rather than wrong, and the view itself shows the error
// when the operator opens it.
export const useReviewCountStore = defineStore('reviewCountStore', () => {
    const count = ref(0);
    const {requestGet, ok} = useRequest(false, false);

    // Refreshes overlap when several decisions are taken quickly. Only the newest one is allowed to set the number.
    let latest = 0;

    async function refresh() {
        const userStore = useUserStore();
        if (!userStore.isOperator || !userStore.selectedTenant || !userStore.orgNumber) {
            count.value = 0;
            return;
        }
        const ticket = ++latest;
        const response = await requestGet(entityReviewCountPath(userStore.selectedTenant, userStore.orgNumber));
        if (ticket !== latest) {
            return;
        }
        count.value = ok.value ? (response?.count ?? 0) : 0;
    }

    return {
        count,
        refresh,
    };
});
