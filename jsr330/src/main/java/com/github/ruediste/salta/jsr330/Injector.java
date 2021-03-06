/**
 * Copyright (C) 2014 Ruedi Steinmann
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

package com.github.ruediste.salta.jsr330;

import java.util.Optional;

import javax.inject.Provider;

import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.jsr330.binder.Binder;
import com.github.ruediste.salta.standard.StandardInjector;
import com.github.ruediste.salta.standard.config.MembersInjectionToken;
import com.google.common.reflect.TypeToken;

public interface Injector {

    /**
     * Injects dependencies into the fields and methods of {@code instance}.
     * Ignores the presence or absence of an injectable constructor.
     *
     * <p>
     * Whenever Salta creates an instance, it performs this injection
     * automatically (after first performing constructor injection), so if
     * you're able to let Salta create all your objects for you, you'll never
     * need to use this method.
     *
     * @param instance
     *            to inject members on
     *
     * @see Binder#getMembersInjector(Class) for a preferred alternative that
     *      supports checks before run time
     */
    void injectMembers(Object instance);

    <T> void injectMembers(TypeToken<T> type, T instance);

    /**
     * Create a token to access the value which makes sure that the members of
     * the value are injected when {@link MembersInjectionToken#getValue()}
     * returns. Only a single token for a single value (compared by identity) is
     * ever created.
     */
    <T> MembersInjectionToken<T> getMembersInjectionToken(T value);

    /**
     * Create a token to access the value which makes sure that the members of
     * the value are injected when {@link MembersInjectionToken#getValue()}
     * returns. Only a single token for a single value (compared by identity) is
     * ever created.
     */
    <T> MembersInjectionToken<T> getMembersInjectionToken(T value, TypeToken<T> type);

    /**
     * Returns the members injector used to inject dependencies into methods and
     * fields on instances of the given type {@code T}.
     *
     * @param typeLiteral
     *            type to get members injector for
     * @see Binder#getMembersInjector(TypeToken) for an alternative that offers
     *      up front error detection
     * @since 2.0
     */
    <T> MembersInjector<T> getMembersInjector(TypeToken<T> typeLiteral);

    /**
     * Returns the members injector used to inject dependencies into methods and
     * fields on instances of the given type {@code T}. When feasible, use
     * {@link Binder#getMembersInjector(TypeToken)} instead to get increased up
     * front error detection.
     *
     * @param type
     *            type to get members injector for
     * @see Binder#getMembersInjector(Class) for an alternative that offers up
     *      front error detection
     * @since 2.0
     */
    <T> MembersInjector<T> getMembersInjector(Class<T> type);

    /**
     * Returns the provider used to obtain instances for the given injection
     * key. When feasible, avoid using this method, in favor of having Salta
     * inject your dependencies ahead of time.
     *
     * @see Binder#getProvider(CoreDependencyKey) for an alternative that offers
     *      up front error detection
     */
    <T> Provider<T> getProvider(CoreDependencyKey<T> key);

    /**
     * Returns the provider used to obtain instances for the given type. When
     * feasible, avoid using this method, in favor of having Salta inject your
     * dependencies ahead of time.
     *
     * @see Binder#getProvider(Class) for an alternative that offers up front
     *      error detection
     */
    <T> Provider<T> getProvider(Class<T> type);

    /**
     * Returns the appropriate instance for the given injection key; equivalent
     * to {@code getProvider(key).get()}. When feasible, avoid using this
     * method, in favor of having Salta inject your dependencies ahead of time.
     *
     * @throws SaltaException
     *             if there was a runtime failure while providing an instance.
     */
    <T> T getInstance(CoreDependencyKey<T> key);

    <T> Optional<T> tryGetInstance(CoreDependencyKey<T> key);

    /**
     * Returns the appropriate instance for the given injection type; equivalent
     * to {@code getProvider(type).get()}. When feasible, avoid using this
     * method, in favor of having Salta inject your dependencies ahead of time.
     *
     * @throws SaltaException
     *             if there was a runtime failure while providing an instance.
     */
    <T> T getInstance(Class<T> type);

    <T> Optional<T> tryGetInstance(Class<T> type);

    StandardInjector getDelegate();
}
