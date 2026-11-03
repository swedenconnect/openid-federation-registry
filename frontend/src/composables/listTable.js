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

import {computed, ref, unref, watch} from 'vue';

/**
 * Search, paging and row handling shared by the list views that use a v-data-table with TablePager.
 *
 * The search matches a case-insensitive substring of one of the filter keys of a row. v-data-table on its own only
 * searches the values of its columns, so bind filterRow to :custom-filter and filterKeys to :filter-keys, which also
 * lets the search look in values that are not shown as a column. Bind filteredCount to the total of TablePager.
 *
 * @param items the rows, a ref or a computed
 * @param filterKeys the keys of a row that the search looks in
 * @param options.open called with the row when it is opened with Enter or Space, rows are not focusable without it
 * @param options.rowLabel gives the aria-label of a row
 * @param options.rowId gives the id of a row
 * @param options.search the search text, for tables that share one search field
 */
export function useListTable(items, filterKeys, {open, rowLabel, rowId, search = ref('')} = {}) {
  const page = ref(1);
  const itemsPerPage = ref(50);

  function matches(row, query) {
    const term = (query || '').toLowerCase();
    return !term || filterKeys.some((key) => String(row[key] ?? '').toLowerCase().includes(term));
  }

  // v-data-table calls this once per filter key with the value of that column, the row is read from the item instead.
  function filterRow(_value, query, item) {
    return matches(item?.raw ?? item, query);
  }

  const filteredCount = computed(() => (unref(items) ?? []).filter((row) => matches(row, search.value)).length);

  watch(search, () => {
    page.value = 1;
  });

  // A list that shrinks, after a delete or a reload, must not leave the page past its end.
  watch([filteredCount, itemsPerPage], ([count, size]) => {
    page.value = Math.min(page.value, Math.max(1, Math.ceil(count / size)));
  });

  // @click:row of v-data-table only handles the mouse, the keyboard opens the focused row here. Keys pressed on a
  // button inside the row are left to the button.
  function rowProps({item}) {
    const id = rowId?.(item);
    if (!open) return id ? {id} : {};
    return {
      id,
      tabindex: 0,
      'aria-label': rowLabel?.(item),
      onKeydown: (event) => {
        if (event.target !== event.currentTarget || (event.key !== 'Enter' && event.key !== ' ')) return;
        event.preventDefault();
        open(item);
      },
    };
  }

  return {search, page, itemsPerPage, filteredCount, filterKeys, filterRow, rowProps};
}
