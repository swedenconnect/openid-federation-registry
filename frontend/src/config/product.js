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

// The name of the application, shown in the header, the browser tab, the footer and the login page.
export const PRODUCT_NAME = 'OpenID Federation Registry';

// The version of the service, set by the Maven build. A build outside Maven shows 'dev'.
export const PRODUCT_VERSION = import.meta.env.VITE_APP_VERSION || 'dev';
