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

// Read base path from the same element that the router uses (#base-href-id),
// falling back to import.meta.env.BASE_URL and then '/'.
function resolveBase() {
    const el = document.getElementById('base-href-id');
    if (el) {
        const href = el.getAttribute('href');
        if (href) {
            return href.endsWith('/') ? href.slice(0, -1) : href;
        }
    }
    const envBase = import.meta.env.BASE_URL;
    if (envBase && envBase !== '/') {
        return envBase.endsWith('/') ? envBase.slice(0, -1) : envBase;
    }
    return '';
}

const base = resolveBase();

function getAbsolutePath(path) {
    const normalized = path.startsWith('/') ? path : '/' + path;
    return base + normalized;
}

export const adminAuthenticatePath = getAbsolutePath('authenticate?reg=oidf-admin&continue=/');
export const adminPath = getAbsolutePath('registry/v1/entities');
export const userInfoPath = getAbsolutePath('userinfo');
export const trustmarksPath = getAbsolutePath('registry/v1/trustmarks');
export const trustmarksListingPath = (trustmarkIssuerID) => getAbsolutePath(`registry/v1/modules/trustmark-issuer/${trustmarkIssuerID}/trustmarks`);
export const trustmarkSubjectsPath = getAbsolutePath('registry/v1/trustmarks/subjects');
export const logoutPath = getAbsolutePath('logout');

export const federationEntitiesPath = getAbsolutePath('registry/v1/entities/federation');
export const federationEntityPath = (id) => getAbsolutePath(`registry/v1/entities/federation/${id}`);
export const hostedEntitiesPath = getAbsolutePath('registry/v1/entities/hosted');
export const hostedEntityPath = (id) => getAbsolutePath(`registry/v1/entities/hosted/${id}`);

export const trustAnchorModulePath = (id) => getAbsolutePath(`registry/v1/modules/trust-anchor/${id}`);
export const intermediateModulePath = (id) => getAbsolutePath(`registry/v1/modules/intermediate/${id}`);
export const resolverModulePath = (id) => getAbsolutePath(`registry/v1/modules/resolver/${id}`);
export const trustmarkIssuerModulePath = (id) => getAbsolutePath(`registry/v1/modules/trustmark-issuer/${id}`);

export const subordinatesPath = getAbsolutePath('registry/v1/subordinates/');
export const subordinatePath = (id) => getAbsolutePath(`registry/v1/subordinates/${id}`);

export const jwksSupportPath = getAbsolutePath('registry/v1/entityconfiguration/jwks');
export const entityConfigurationViewPath = getAbsolutePath('registry/v1/entityconfiguration/view');

export const swaggerUiPath = getAbsolutePath('swagger-ui.html');

export const registrationFlowsPath = getAbsolutePath('registration-flow/v1/flows');
export const registrationFlowPath = (id) => getAbsolutePath(`registration-flow/v1/flow/${id}`);
export const registrationFlowCreatePath = getAbsolutePath('registration-flow/v1/flow');
export const registrationFlowStepsPath = getAbsolutePath('registration-flow/v1/steps');

export const intermediateFlowAssignmentsPath = (taImId) => getAbsolutePath(`registration-flow/v1/intermediate/${taImId}/assignments`);
export const intermediateFlowAssignPath = (taImId) => getAbsolutePath(`registration-flow/v1/intermediate/${taImId}/assign`);
export const intermediateFlowUnassignPath = (taImId, assignId) => getAbsolutePath(`registration-flow/v1/intermediate/${taImId}/assign/${assignId}`);

export const tmIssuerTrustmarkAssignmentsPath = (tmIssuerId) => getAbsolutePath(`registration-flow/v1/trustmark-issuer/${tmIssuerId}/trustmark-assignments`);
export const tmFlowAssignPath = (trustmarkId) => getAbsolutePath(`registration-flow/v1/trustmark/${trustmarkId}/assign`);
export const tmFlowUnassignPath = (trustmarkId, assignId) => getAbsolutePath(`registration-flow/v1/trustmark/${trustmarkId}/assign/${assignId}`);

export const registrationAdminPath = getAbsolutePath('registration-admin/v1');
export const registrationAdminItemPath = (id) => getAbsolutePath(`registration-admin/v1/${id}`);
export const registrationAdminRejectPath = (id) => getAbsolutePath(`registration-admin/v1/${id}/reject`);
export const registrationAdminApproveStepPath = (id, stepIndex) => getAbsolutePath(`registration-admin/v1/${id}/steps/${stepIndex}/approve`);

export const registrationPublicFlowsPath = getAbsolutePath('registration/v1/flows');
export const registrationTriggerPath = (joinId) => getAbsolutePath(`registration/v1/${joinId}`);

export const modulesPath = getAbsolutePath('registry/v1/modules');
