/*
 * Copyright 2025 Sweden Connect
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

package se.swedenconnect.oidf.registry.validation;

import se.swedenconnect.oidf.registry.auth.OrganizationRecord;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple template engine that replace ${variableName} with the result from Function valueResolver
 *
 * @author Per Fredrik Plars
 */
public class VariabelValueResolver {

  final Function<String, String> valueResolver;
  final String patternString = "@\\{([^}]*)}";
  final Pattern pattern = Pattern.compile(this.patternString);

  /**
   * Constructs a new instance of VariabelValueResolver with the provided value resolver function. The provided function
   * is used to resolve variable names to their corresponding values.
   *
   * @param valueResolver a function that takes a variable name as input and returns its resolved value as a string
   */
  public VariabelValueResolver(final Function<String, String> valueResolver) {
    this.valueResolver = valueResolver;
  }

  /**
   * Inserts values into a template string by replacing placeholders of the format ${variableName} with values resolved
   * by a provided function.
   *
   * @param template the template string containing placeholders in the format ${variableName}. If the template is
   *     null, null is returned.
   * @return the resulting string with placeholders replaced by their resolved values, or null if the input template is
   *     null.
   */
  public String insertTemplateValues(final String template) {
    if (template == null) {
      return null;
    }
    final Matcher matcher = this.pattern.matcher(template);
    String result = template;
    while (matcher.find()) {
      result = result.replace(matcher.group(), this.valueResolver.apply(matcher.group(1)));
    }
    return result;
  }

  /**
   * Creates a default instance of a VariabelValueResolver with a value resolver function that returns an empty string
   * for any variable.
   *
   * @return a VariabelValueResolver instance that resolves all variables to an empty string
   */
  public static VariabelValueResolver defaultResolver() {
    return new VariabelValueResolver(s -> "");
  }

  /**
   * Creates a VariabelValueResolver instance that resolves variables using the provided key-value map. For any
   * variable, if the key exists in the map, the associated value will be returned; otherwise, an empty string is
   * returned as the default value.
   *
   * @param keyValue a map containing key-value pairs used to resolve variables. Keys represent variable names, and
   *     values represent the corresponding resolved values for those variables.
   * @return a VariabelValueResolver instance that resolves variables based on the provided map.
   */
  public static VariabelValueResolver defaultResolver(final Map<String, String> keyValue) {
    return new VariabelValueResolver(s -> keyValue.getOrDefault(s, ""));
  }

  /**
   * Creates a {@code VariabelValueResolver} that resolves specific organization-related variables to their
   * corresponding values based on the provided {@code OrganizationRecord}. The following variable names are supported:
   * - "orgid" resolves to {@code organizationRecord.orgNumber()}. - "entityprefix" resolves to
   * {@code organizationRecord.entityPrefix()}. - "orgname" resolves to {@code organizationRecord.orgName()}. Any
   * unsupported variable name will resolve to an empty string.
   *
   * @param organizationRecord an instance of {@code OrganizationRecord} containing the data to resolve
   *     organization-related variables.
   * @return a {@code VariabelValueResolver} capable of resolving organization-related variables based on the provided
   *     {@code OrganizationRecord}.
   */
  public static VariabelValueResolver orgResolver(final OrganizationRecord organizationRecord) {
    return new VariabelValueResolver(s -> switch (s) {
      case "orgid" -> organizationRecord.orgNumber();
      case "entityprefix" -> organizationRecord.entityPrefix();
      case "orgname" -> organizationRecord.orgName();
      default -> "";
    });
  }

}
