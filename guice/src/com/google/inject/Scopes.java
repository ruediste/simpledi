/**
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.inject;

import com.github.ruediste.salta.core.CoreDependencyKey;
import com.google.inject.internal.SingletonScope;

/**
 * Built-in scope implementations.
 *
 * @author crazybob@google.com (Bob Lee)
 */
public class Scopes {

    private Scopes() {
    }

    /**
     * One instance per {@link Injector}. Also see {@code @}{@link Singleton}.
     */
    public static final Scope SINGLETON = new SingletonScope();

    /**
     * No scope; the same as not applying any scope at all. Each time the
     * Injector obtains an instance of an object with "no scope", it injects
     * this instance then immediately forgets it. When the next request for the
     * same binding arrives it will need to obtain the instance over again.
     *
     * <p>
     * This exists only in case a class has been annotated with a scope
     * annotation such as {@link Singleton @Singleton}, and you need to override
     * this to "no scope" in your binding.
     *
     * @since 2.0
     */
    public static final Scope NO_SCOPE = new Scope() {

        @Override
        public String toString() {
            return "Scopes.NO_SCOPE";
        }

        @Override
        public <T> Provider<T> scope(
                com.github.ruediste.salta.core.Binding binding,
                CoreDependencyKey<T> requestedKey, Provider<T> unscoped) {
            return unscoped;
        }
    };
}
