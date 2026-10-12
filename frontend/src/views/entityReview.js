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

// Shared by the entity page and the review view.

export const GROUP_LABELS = {DOMAIN: 'Domain', TRUST_MARK: 'Trust marks', ENTITY: 'Entity'};
export const ITEM_LABELS = {DOMAIN: 'Domain', TRUST_MARK: 'Trust mark', ENTITY: 'Entity'};

export function itemKey(item) {
  return `${item.type}:${item.itemId}:${item.type === 'DOMAIN' ? '' : item.stepIndex}`;
}

export function stateColor(state) {
  return {PENDING: 'warning', WAITING: 'default', UPCOMING: 'default', APPROVED: 'success', REJECTED: 'error'}[state]
      ?? 'default';
}

export function stateLabel(state) {
  return {
    PENDING: 'Awaiting review',
    WAITING: 'Waiting',
    UPCOMING: 'Not reached',
    APPROVED: 'Approved',
    REJECTED: 'Rejected',
  }[state] ?? state;
}

export function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}
