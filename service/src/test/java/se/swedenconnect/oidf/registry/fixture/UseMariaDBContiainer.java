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
package se.swedenconnect.oidf.registry.fixture;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation to be used on test classes to initialize and start a MariaDB testcontainer instance for the duration of
 * the tests. This annotation ensures that a MariaDB container is available before any tests are executed.
 * <p>
 * This annotation uses the {@link MariaDBInitializer} class to manage the lifecycle of the MariaDB container, ensuring
 * it is started once before all tests and providing necessary configurations for test execution. Testcontainer will
 * manage the stopping of the container after all tests are run.
 * <br>
 * Usage: - Annotate your test class with @UseMariaDBContainer to activate this extension.
 *
 * <pre>{@code
 * @UseMariaDBContainer
 * public class MyDatabaseInvolvedTests {
 *    // Test methods here
 * }
 * }
 * </pre>
 *
 * Note: This should be used only in test classes where interaction with a MariaDB database is required.
 *
 * @author David Goldring
 */

@Target(ElementType.TYPE)
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@ExtendWith(MariaDBInitializer.class)
public @interface UseMariaDBContiainer {
}
