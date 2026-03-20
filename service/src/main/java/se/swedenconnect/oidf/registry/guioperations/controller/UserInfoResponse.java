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
package se.swedenconnect.oidf.registry.guioperations.controller;

import lombok.Builder;
import se.swedenconnect.oidf.registry.infrastructure.auth.OrganizationInformation;

/**
 * Represents a response containing details about a user.
 *
 * @param userName the users name
 * @param givenName the users given name
 * @param familyName the users family name
 * @param fullName the users full name
 * @param orgNumber the users organization number
 * @param orgName the users organization name
 * @param orgInfo the users organization information
 * @param entityPrefix entityPrefix ex https://www.ppm.nu/oidf
 * @author David Goldring
 */
@Builder
public record UserInfoResponse(
    String userName,
    String givenName,
    String familyName,
    String fullName,
    String orgNumber,
    String orgName,
    String entityPrefix,
    OrganizationInformation orgInfo) {}
