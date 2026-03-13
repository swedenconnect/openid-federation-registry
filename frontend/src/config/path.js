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

// Helper function to ensure absolute paths
function getAbsolutePath(path) {
    // If path already starts with /, return as is
    if (path.startsWith('/')) {
        return path;
    }
    // Otherwise, ensure it starts with /
    return '/' + path;
}

export const adminAuthenticatePath = getAbsolutePath('authenticate?reg=entity-registry&continue=/');
export const adminPath = getAbsolutePath('api/v1/entities');
export const policiesPath = getAbsolutePath('api/v1/policies');
export const adminUserApiPath = getAbsolutePath('');
export const userInfoPath = getAbsolutePath('userinfo');
export const trustmarksPath = getAbsolutePath('api/v1/trustmarks');
export const trustmarksListingPath = (trustmarkIssuerID) => getAbsolutePath(`api/v1/modules/trustmark-issuer/${trustmarkIssuerID}/trustmarks`);
export const trustmarkSubjectsPath = getAbsolutePath('api/v1/trustmarks/subjects');
export const logoutPath = getAbsolutePath('logout');
