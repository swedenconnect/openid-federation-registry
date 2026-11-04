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

import {nextTick, onBeforeUnmount, onMounted} from 'vue';
import {onBeforeRouteLeave} from 'vue-router';
import {useLeaveStore} from '@/stores/leaveStore';

// Asks before leaving a form whose state differs from the last saved state. state returns the values of the form.
// Nothing is guarded until markSaved has been called, which the form does when it has loaded and after each save.
export function useUnsavedChanges(state) {
    const leaveStore = useLeaveStore();
    let saved = null;

    // Taken again after the next tick, since watchers in the form can still adjust the values that were just set.
    function markSaved() {
        saved = JSON.stringify(state());
        nextTick(() => {
            saved = JSON.stringify(state());
        });
    }

    function isDirty() {
        return saved !== null && JSON.stringify(state()) !== saved;
    }

    onBeforeRouteLeave(async () => !isDirty() || await leaveStore.confirm());

    // Closing or reloading the tab. The browser shows its own question.
    function onBeforeUnload(event) {
        if (isDirty()) {
            event.preventDefault();
        }
    }

    onMounted(() => globalThis.addEventListener('beforeunload', onBeforeUnload));
    onBeforeUnmount(() => globalThis.removeEventListener('beforeunload', onBeforeUnload));

    return {markSaved, isDirty};
}
