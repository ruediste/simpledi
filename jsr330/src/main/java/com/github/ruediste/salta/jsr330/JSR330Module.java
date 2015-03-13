package com.github.ruediste.salta.jsr330;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.github.ruediste.salta.AbstractModule;
import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.standard.MembersInjector;
import com.github.ruediste.salta.standard.ProviderMethodBinder;
import com.github.ruediste.salta.standard.StandardModule;
import com.github.ruediste.salta.standard.config.StandardInjectorConfiguration;
import com.github.ruediste.salta.standard.recipe.FixedConstructorRecipeInstantiator;
import com.github.ruediste.salta.standard.util.MembersInjectorCreationRuleBase;
import com.github.ruediste.salta.standard.util.MethodOverrideIndex;
import com.github.ruediste.salta.standard.util.ProviderCreationRule;
import com.github.ruediste.salta.standard.util.RecipeInitializerFactoryBase;
import com.google.common.reflect.TypeToken;

public class JSR330Module extends AbstractModule {

	@Override
	protected void configure() {
		StandardInjectorConfiguration config = binder().getConfiguration();

		// add rule for ProvidedBy and ImplementedBy
		config.constructionRules.add(new JSR330ProvidedByInstantiatorRule());
		config.constructionRules.add(new JSR330ImplementedByConstructionRule());

		// default instantiator rule
		config.instantiatorRules.add(new JSR330ConstructorInstantiatorRule(
				config));

		config.fixedConstructorInstantiatorFactory = (type, ctx, cstr) -> FixedConstructorRecipeInstantiator
				.of(type, ctx, cstr, config.config.injectionStrategy);

		config.noInstantiatorFoundErrorProducers
				.add(typeToken -> {
					Class<?> clazz = typeToken.getRawType();
					Class<?> enclosingClass = clazz.getEnclosingClass();
					if (enclosingClass != null) {
						for (Constructor<?> c : clazz.getDeclaredConstructors()) {
							if (c.getParameterCount() == 1
									&& enclosingClass.equals(c
											.getParameterTypes()[0])) {
								throw new SaltaException(
										"No suitable constructor found for inner non-static class "
												+ typeToken
												+ ".\nCannot instantiate non-static inner classes. Forgotten to make class static?");
							}
						}
					}
				});
		config.defaultMembersInjectorFactories
				.add(new JSR330MembersInjectorFactory(config));

		config.initializerFactories.add(new RecipeInitializerFactoryBase(
				config.config) {

			@Override
			protected boolean isInitializer(TypeToken<?> declaringType,
					Method method, MethodOverrideIndex overrideIndex) {

				if (method.isAnnotationPresent(PostConstruct.class)) {
					if (method.getTypeParameters().length > 0) {
						throw new SaltaException(
								"@PostConstruct methods may not declare generic type parameters");
					}
					return true;
				}

				return false;
			}
		});

		config.config.creationRules.add(new ProviderCreationRule(
				key -> {
					return key.getType().getRawType().equals(Provider.class);
				}, (type, supplier) -> (Provider<?>) supplier::get,
				Provider.class));

		config.config.creationRules.add(new MembersInjectorCreationRuleBase(
				config) {

			@Override
			protected Object wrapInjector(Consumer<Object> saltaMembersInjector) {
				return new MembersInjector<Object>() {

					@Override
					public void injectMembers(Object instance) {
						saltaMembersInjector.accept(instance);
					}

					@Override
					public String toString() {
						return saltaMembersInjector.toString();
					};
				};
			}

			@Override
			protected Class<?> getWrappedInjectorType() {
				return MembersInjector.class;
			}

			@Override
			protected TypeToken<?> getDependency(CoreDependencyKey<?> key) {
				if (!MembersInjector.class.equals(key.getRawType()))
					return null;

				if (key.getType().getType() instanceof Class) {
					throw new SaltaException(
							"Cannot inject a MembersInjector that has no type parameter");
				}
				TypeToken<?> dependency = key.getType().resolveType(
						MembersInjector.class.getTypeParameters()[0]);
				return dependency;
			}
		});
		config.requiredQualifierExtractors.add(annotatedElement -> Arrays
				.stream(annotatedElement.getAnnotations()).filter(
						a -> a.annotationType().isAnnotationPresent(
								Qualifier.class)));

		config.availableQualifierExtractors.add(annotated -> Arrays.stream(
				annotated.getAnnotations()).filter(
				a -> a.annotationType().isAnnotationPresent(Qualifier.class)));

		// register initializer for requested static injections
		config.dynamicInitializers.add(i -> new JSR330StaticMemberInjector()
				.injectStaticMembers(config, i));

		// register scanner for provides methods
		{
			ProviderMethodBinder b = new ProviderMethodBinder(config) {

				@Override
				protected boolean isProviderMethod(Method m) {
					if (!m.isAnnotationPresent(Provides.class)) {
						return false;
					}
					if (void.class.equals(m.getReturnType())) {
						throw new SaltaException(
								"@Provides method returns void: " + m);
					}
					return true;
				}
			};
			config.modulePostProcessors.add(b::bindProviderMethodsOf);
		}
		bindScope(Singleton.class, config.singletonScope);

		install(new StandardModule());
	}
}
