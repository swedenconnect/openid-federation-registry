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
export const userInfoPath = getAbsolutePath('userinfo');
export const tenantsPath = getAbsolutePath('tenants');
export const logoutPath = getAbsolutePath('logout');
export const swaggerUiPath = getAbsolutePath('swagger-ui.html');

// Endpoints below require the tenant and organization currently selected in the
// Tenant/Organization dropdowns (see userStore's selectedTenant/orgNumber).
export const adminPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entities`);
export const trustmarksPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/trustmarks`);
export const trustmarksListingPath = (tenant, orgNumber, trustmarkIssuerID) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules/trustmark-issuer/${trustmarkIssuerID}/trustmarks`);
export const trustmarkSubjectsPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/trustmarks/subjects`);

export const federationEntitiesPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entities/federation`);
export const federationEntityPath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entities/federation/${id}`);
export const hostedEntitiesPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entities/hosted`);
export const hostedEntityPath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entities/hosted/${id}`);

export const trustAnchorModulePath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules/trust-anchor/${id}`);
export const intermediateModulePath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules/intermediate/${id}`);
export const resolverModulePath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules/resolver/${id}`);
export const trustmarkIssuerModulePath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules/trustmark-issuer/${id}`);

export const subordinatesPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/subordinates/`);
export const subordinatePath = (tenant, orgNumber, id) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/subordinates/${id}`);

export const jwksSupportPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entityconfiguration/jwks`);
export const entityConfigurationViewPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entityconfiguration/view`);

export const registrationFlowsPath = (tenant, orgNumber) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/flows`);
export const registrationFlowPath = (tenant, orgNumber, id) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/flow/${id}`);
export const registrationFlowCreatePath = (tenant, orgNumber) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/flow`);
export const registrationFlowStepsPath = (tenant, orgNumber) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/steps`);

export const intermediateFlowAssignmentsPath = (tenant, orgNumber, taImId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/intermediate/${taImId}/assignments`);
export const intermediateFlowAssignPath = (tenant, orgNumber, taImId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/intermediate/${taImId}/assign`);
export const intermediateFlowUnassignPath = (tenant, orgNumber, taImId, assignId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/intermediate/${taImId}/assign/${assignId}`);

export const tmIssuerTrustmarkAssignmentsPath = (tenant, orgNumber, tmIssuerId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/trustmark-issuer/${tmIssuerId}/trustmark-assignments`);
export const tmFlowAssignPath = (tenant, orgNumber, trustmarkId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/trustmark/${trustmarkId}/assign`);
export const tmFlowUnassignPath = (tenant, orgNumber, trustmarkId, assignId) => getAbsolutePath(`registration-flow/v1/${tenant}/${orgNumber}/trustmark/${trustmarkId}/assign/${assignId}`);

export const modulesPath = (tenant, orgNumber) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/modules`);
export const signingKeysPath = (tenant, orgNumber, type) => getAbsolutePath(`registry/v1/${tenant}/${orgNumber}/entityconfiguration/signing-keys?type=${type}`);

export const registrationAdminPath = (tenant, orgNumber) => getAbsolutePath(`registration-admin/v1/${tenant}/${orgNumber}`);
export const registrationAdminItemPath = (tenant, orgNumber, id) => getAbsolutePath(`registration-admin/v1/${tenant}/${orgNumber}/${id}`);
export const registrationAdminRejectPath = (tenant, orgNumber, id) => getAbsolutePath(`registration-admin/v1/${tenant}/${orgNumber}/${id}/reject`);
export const registrationAdminApproveStepPath = (tenant, orgNumber, id, stepIndex) => getAbsolutePath(`registration-admin/v1/${tenant}/${orgNumber}/${id}/steps/${stepIndex}/approve`);

export const registrationPublicFlowsPath = getAbsolutePath('registration/v1/flows');
export const registrationTriggerPath = (tenant, orgNumber, joinId) => getAbsolutePath(`registration/v1/${tenant}/${orgNumber}/${joinId}`);
