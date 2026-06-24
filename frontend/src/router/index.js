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

import {createRouter, createWebHistory} from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import HostedEntityFormView from '../views/HostedEntityFormView.vue';
import FederationEntityNewView from '../views/FederationEntityNewView.vue';
import FederationEntityEditView from '../views/FederationEntityEditView.vue';
import SubordinatesListView from '../views/SubordinatesListView.vue';
import SubordinateFormView from '../views/SubordinateFormView.vue';
import TrustmarksListView from '../views/TrustmarksListView.vue';
import TrustmarkFormView from '../views/TrustmarkFormView.vue';
import TrustmarkSubjectsListView from '../views/TrustmarkSubjectsListView.vue';
import TrustmarkSubjectFormView from '../views/TrustmarkSubjectFormView.vue';
import RegistrationFlowsListView from '../views/RegistrationFlowsListView.vue';
import RegistrationFlowFormView from '../views/RegistrationFlowFormView.vue';
import RegistrationsListView from '../views/RegistrationsListView.vue';
import RegistrationDetailView from '../views/RegistrationDetailView.vue';
import TriggerRegistrationView from '../views/TriggerRegistrationView.vue';
import {useAuthorizationStatusStore} from '@/authorization/stores/authorizationStatusStore';

let base = import.meta.env.BASE_URL;
const baseHref = document.getElementById('base-href-id');
if (baseHref !== undefined && baseHref !== null) {
    const attr = baseHref.attributes.getNamedItem('href');
    if (attr !== null) {
        base = attr.value;
    }
}

const router = createRouter({
    history: createWebHistory(base),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: LoginView,
            meta: {public: true, title: 'Login'},
        },
        {
            path: '/',
            name: 'home',
            component: HomeView,
            meta: {title: 'Entities'},
        },
        {
            path: '/entities/federation/new',
            name: 'federation-entity-new',
            component: FederationEntityNewView,
            meta: {title: 'New Federation Entity'},
        },
        {
            path: '/entities/federation/:id/edit',
            name: 'federation-entity-edit',
            component: FederationEntityEditView,
            meta: {title: 'Edit Federation Entity'},
        },
        {
            path: '/entities/hosted/new',
            name: 'hosted-entity-new',
            component: HostedEntityFormView,
            meta: {title: 'New Hosted Entity'},
        },
        {
            path: '/entities/hosted/:id/edit',
            name: 'hosted-entity-edit',
            component: HostedEntityFormView,
            meta: {title: 'Edit Hosted Entity'},
        },
        {
            path: '/entities/:entityId/modules/:moduleType/subordinates',
            name: 'subordinates-list',
            component: SubordinatesListView,
            meta: {title: 'Subordinates'},
        },
        {
            path: '/entities/:entityId/modules/:moduleType/subordinates/new',
            name: 'subordinate-new',
            component: SubordinateFormView,
            meta: {title: 'New Subordinate'},
        },
        {
            path: '/entities/:entityId/modules/:moduleType/subordinates/:id/edit',
            name: 'subordinate-edit',
            component: SubordinateFormView,
            meta: {title: 'Edit Subordinate'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks',
            name: 'trustmarks-list',
            component: TrustmarksListView,
            meta: {title: 'Trustmarks'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks/new',
            name: 'trustmark-new',
            component: TrustmarkFormView,
            meta: {title: 'New Trustmark'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks/:id/edit',
            name: 'trustmark-edit',
            component: TrustmarkFormView,
            meta: {title: 'Edit Trustmark'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks/:trustmarkId/subjects',
            name: 'trustmark-subjects-list',
            component: TrustmarkSubjectsListView,
            meta: {title: 'Trustmark Subjects'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks/:trustmarkId/subjects/new',
            name: 'trustmark-subject-new',
            component: TrustmarkSubjectFormView,
            meta: {title: 'New Trustmark Subject'},
        },
        {
            path: '/entities/:entityId/modules/trustmarkissuer/trustmarks/:trustmarkId/subjects/:id/edit',
            name: 'trustmark-subject-edit',
            component: TrustmarkSubjectFormView,
            meta: {title: 'Edit Trustmark Subject'},
        },
        {
            path: '/registration-flows',
            name: 'registration-flows-list',
            component: RegistrationFlowsListView,
            meta: {title: 'Registration Flows'},
        },
        {
            path: '/registration-flows/new',
            name: 'registration-flow-new',
            component: RegistrationFlowFormView,
            meta: {title: 'New Registration Flow'},
        },
        {
            path: '/registration-flows/:id/edit',
            name: 'registration-flow-edit',
            component: RegistrationFlowFormView,
            meta: {title: 'Edit Registration Flow'},
        },
        {
            path: '/registrations',
            name: 'registrations-list',
            component: RegistrationsListView,
            meta: {title: 'Registrations'},
        },
        {
            path: '/registrations/trigger',
            name: 'registration-trigger',
            component: TriggerRegistrationView,
            meta: {title: 'Trigger Registration'},
        },
        {
            path: '/registrations/:id',
            name: 'registration-detail',
            component: RegistrationDetailView,
            meta: {title: 'Registration Detail'},
        },
    ],
});

router.afterEach((to) => {
    document.title = `${to.meta.title ?? to.name} — OpenID Federation Admin`;
});

router.beforeEach((to) => {
    if (to.meta.public) return true;

    const authStore = useAuthorizationStatusStore();
    if (authStore.isAuthorized === false) {
        return {name: 'login'};
    }
    return true;
});

export default router;
