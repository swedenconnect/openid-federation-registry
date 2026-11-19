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

import {beforeEach, describe, expect, it, vi} from 'vitest';
import {createPinia, setActivePinia} from 'pinia';
import {useReviewCountStore} from '@/stores/reviewCountStore';
import {useUserStore} from '@/stores/userStore';

// The HTTP layer is replaced, everything above it is the real thing: the request composable and the store. The refs
// are plain objects because that is all the composable does with them.
const http = vi.hoisted(() => ({
    fetchData: vi.fn(),
    json: {value: null},
    ok: {value: null},
    status: {value: null},
    traceparent: {value: null},
}));

vi.mock('@/api/composables/fetch', () => ({
    useFetch: () => http,
    csrfToken: () => null,
    csrfHeaders: () => ({}),
}));

const COUNT_PATH = '/registration-admin/v1/Swedenconnect/5520001263/entity-reviews/count';

describe('reviewCountStore', () => {

    beforeEach(() => {
        setActivePinia(createPinia());
        http.fetchData.mockReset();
        http.ok.value = null;
        http.json.value = null;
        http.status.value = null;
        // The request composable logs a failed request.
        vi.spyOn(console, 'log').mockImplementation(() => {
        });
    });

    /** An operator of the Swedenconnect tenant, the only case where the badge is shown. */
    function operator() {
        const userStore = useUserStore();
        userStore.selectedTenant = 'Swedenconnect';
        userStore.orgNumber = '5520001263';
        userStore.isOperator = true;
        return userStore;
    }

    it('takes the count from the server for the selected tenant and organization', async () => {
        operator();
        http.ok.value = true;
        http.fetchData.mockResolvedValue({count: 3});
        const store = useReviewCountStore();

        await store.refresh();

        expect(http.fetchData).toHaveBeenCalledWith(COUNT_PATH, null, 'GET');
        expect(store.count).toBe(3);
    });

    it('asks for nothing and shows nothing when the organization does not operate the tenant', async () => {
        const userStore = operator();
        userStore.isOperator = false;
        const store = useReviewCountStore();

        await store.refresh();

        expect(http.fetchData).not.toHaveBeenCalled();
        expect(store.count).toBe(0);
    });

    it('asks for nothing before a tenant and an organization are selected', async () => {
        const userStore = operator();
        userStore.selectedTenant = '';
        userStore.orgNumber = '';
        const store = useReviewCountStore();

        await store.refresh();

        expect(http.fetchData).not.toHaveBeenCalled();
        expect(store.count).toBe(0);
    });

    it('drops the badge instead of keeping a stale number when the request fails', async () => {
        operator();
        http.ok.value = true;
        http.fetchData.mockResolvedValue({count: 3});
        const store = useReviewCountStore();
        await store.refresh();

        http.ok.value = false;
        http.status.value = 500;
        http.fetchData.mockRejectedValue(new Error('unknown'));
        await store.refresh();

        expect(store.count).toBe(0);
    });

    it('keeps the newest number when two refreshes overlap', async () => {
        operator();
        http.ok.value = true;
        let answerFirst;
        http.fetchData
            .mockImplementationOnce(() => new Promise(resolve => {
                answerFirst = () => resolve({count: 7});
            }))
            .mockResolvedValue({count: 2});
        const store = useReviewCountStore();

        const first = store.refresh();
        const second = store.refresh();
        await second;
        answerFirst();
        await first;

        expect(store.count).toBe(2);
    });
});
