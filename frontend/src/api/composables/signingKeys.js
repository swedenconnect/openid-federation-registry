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

import {ref} from 'vue'
import {useRequest} from './request'
import {signingKeysPath} from '@/config/path'

export function useSigningKeys() {
    const {requestGet, loading} = useRequest(false)
    const signingKeys = ref([])

    async function fetchSigningKeys(type) {
        const keys = await requestGet(signingKeysPath(type))
        signingKeys.value = (keys || []).map(name => ({
            value: name,
            title: name,
        }))
    }

    return {signingKeys, fetchSigningKeys, loading}
}
