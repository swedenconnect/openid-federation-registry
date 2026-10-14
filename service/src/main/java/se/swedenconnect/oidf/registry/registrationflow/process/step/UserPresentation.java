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
package se.swedenconnect.oidf.registry.registrationflow.process.step;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration values every selectable step has, besides its own: whether the step is shown to the user who
 * registers, and the name and description the user sees for it.
 *
 * @author Felix Hellman
 */
public final class UserPresentation {

  /** Whether the step is shown to the user. */
  public static final String SHOW_FOR_USER = "showforuser";

  /** The name the user sees for the step, in Swedish. */
  public static final String DISPLAY_NAME_SV = "userdisplayname_sv";

  /** The name the user sees for the step, in English. */
  public static final String DISPLAY_NAME_EN = "userdisplayname_en";

  /** The description the user sees for the step, in Swedish. */
  public static final String DESCRIPTION_SV = "userdescription_sv";

  /** The description the user sees for the step, in English. */
  public static final String DESCRIPTION_EN = "userdescription_en";

  /** The texts a step shown for the user must have. */
  public static final List<String> TEXTS = List.of(DISPLAY_NAME_SV, DISPLAY_NAME_EN, DESCRIPTION_SV, DESCRIPTION_EN);

  /** The values, in the order they are shown. */
  public static final List<StepConfigurationValue> CONFIGURATION_VALUES = List.of(
      new StepConfigurationValue(SHOW_FOR_USER, StepConfigurationValue.DATA_TYPE.BOOLEAN,
          "Show this step to the user who registers", false),
      new StepConfigurationValue(DISPLAY_NAME_SV, StepConfigurationValue.DATA_TYPE.STRING,
          "Name the user sees for this step, in Swedish; required when shown for user", null),
      new StepConfigurationValue(DISPLAY_NAME_EN, StepConfigurationValue.DATA_TYPE.STRING,
          "Name the user sees for this step, in English; required when shown for user", null),
      new StepConfigurationValue(DESCRIPTION_SV, StepConfigurationValue.DATA_TYPE.STRING,
          "Description the user sees for this step, in Swedish; required when shown for user", null),
      new StepConfigurationValue(DESCRIPTION_EN, StepConfigurationValue.DATA_TYPE.STRING,
          "Description the user sees for this step, in English; required when shown for user", null));

  private UserPresentation() {
  }

  /**
   * A step as the user sees it.
   *
   * @param name the name shown for the step, in English
   * @param nameSv the name shown for the step, in Swedish
   * @param description what the step does, for the user, in English
   * @param descriptionSv what the step does, for the user, in Swedish
   */
  public record UserStep(String name, String nameSv, String description, String descriptionSv) {
  }

  /**
   * Returns how a configured step is shown to the user.
   *
   * @param config the step's configuration in the flow
   * @return the step as the user sees it; empty if the step is not shown for the user
   */
  public static Optional<UserStep> of(final StepConfig config) {
    if (config == null || !config.getBoolean(SHOW_FOR_USER)) {
      return Optional.empty();
    }
    return Optional.of(new UserStep(config.getString(DISPLAY_NAME_EN), config.getString(DISPLAY_NAME_SV),
        config.getString(DESCRIPTION_EN), config.getString(DESCRIPTION_SV)));
  }

  /**
   * Checks that a step shown for the user has a name and a description to show, in Swedish and English.
   *
   * @param values the step's configuration values, keyed by name
   * @return what is wrong, one message each; empty if the values can be used
   */
  public static List<String> validate(final Map<String, String> values) {
    if (!Boolean.parseBoolean(values.get(SHOW_FOR_USER))) {
      return List.of();
    }
    final List<String> problems = new ArrayList<>();
    for (final String text : TEXTS) {
      if (isBlank(values.get(text))) {
        problems.add(text + " is required when " + SHOW_FOR_USER + " is enabled");
      }
    }
    return problems;
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
