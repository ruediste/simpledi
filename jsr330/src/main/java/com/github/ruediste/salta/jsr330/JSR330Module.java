package com.github.ruediste.salta.jsr330;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.github.ruediste.salta.core.Binding;
import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.CoreInjector;
import com.github.ruediste.salta.core.CreationRule;
import com.github.ruediste.salta.core.CreationRuleImpl;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.core.compile.SupplierRecipe;
import com.github.ruediste.salta.core.compile.SupplierRecipeImpl;
import com.github.ruediste.salta.standard.DefaultFixedConstructorInstantiationRule;
import com.github.ruediste.salta.standard.DefaultJITBindingKeyRule;
import com.github.ruediste.salta.standard.DefaultJITBindingRule;
import com.github.ruediste.salta.standard.DependencyKey;
import com.github.ruediste.salta.standard.ProviderMethodBinder;
import com.github.ruediste.salta.standard.Stage;
import com.github.ruediste.salta.standard.StandardInjector;
import com.github.ruediste.salta.standard.config.DefaultConstructionRule;
import com.github.ruediste.salta.standard.config.MembersInjectorFactory;
import com.github.ruediste.salta.standard.config.StandardInjectorConfiguration;
import com.github.ruediste.salta.standard.util.ConstructorInstantiatorRuleBase;
import com.github.ruediste.salta.standard.util.ImplementedByConstructionRuleBase;
import com.github.ruediste.salta.standard.util.MembersInjectorCreationRuleBase;
import com.github.ruediste.salta.standard.util.MembersInjectorFactoryBase;
import com.github.ruediste.salta.standard.util.MethodOverrideIndex;
import com.github.ruediste.salta.standard.util.ProvidedByConstructionRuleBase;
import com.github.ruediste.salta.standard.util.ProviderCreationRule;
import com.github.ruediste.salta.standard.util.RecipeInitializerFactoryBase;
import com.github.ruediste.salta.standard.util.StaticMembersInjectorBase;
import com.google.common.reflect.TypeToken;

public class JSR330Module extends AbstractModule {

	@Override
	protected void configure() {
		StandardInjectorConfiguration config = binder().config().standardConfig;

		addProvidedByConstructionRule(config);
		addImplementedByConstructionRule(config);
		addTypeTokenConstructionRule(config);
		bind(Injector.class).toInstance(binder().getInjector());

		addConstructionInstantiatorRule(config);

		addFixedConstructorInstantiatorFactory(config);

		addInjectionOptionalRule(config);

		addStageCreationRule(config);

		addMembersInjectorFactory(config);

		addPostConstructInitializerFactory(config);

		addProviderCreationRule(config);

		addMembersInjectorCreationRule(config);
		addQualifierExtractors(config);

		addStaticMembersDynamicInitializer(config);

		addProviderMethodBinderModulePostProcessor(config);
		bindScopes(config);

		addJitBindingKeyRule(config);

		addJitBindingRule(config);

		addDefaultConstructionRule(config);
		setMembersInjectorFactory(config);
		if (config.stage == Stage.PRODUCTION)
			addSingletonInstantiationDynamicInitializer(config);

	}

	protected void addDefaultConstructionRule(StandardInjectorConfiguration config) {
		config.construction.constructionRules.add(new DefaultConstructionRule(config));
	}

	protected void addJitBindingRule(StandardInjectorConfiguration config) {
		config.creationPipeline.jitBindingRules.add(new DefaultJITBindingRule(config));
	}

	protected void addJitBindingKeyRule(StandardInjectorConfiguration config) {
		config.creationPipeline.jitBindingKeyRules.add(new DefaultJITBindingKeyRule(config));
	}

	protected void bindScopes(StandardInjectorConfiguration config) {
		bindScope(Singleton.class, config.singletonScope);
		bindScope(DefaultScope.class, config.defaultScope);
	}

	protected void addStageCreationRule(StandardInjectorConfiguration config) {
		// stage creation rule
		config.creationPipeline.creationRules
				.add(new CreationRuleImpl(key -> Stage.class.equals(key.getRawType()), key -> () -> config.stage));
	}

	protected void addInjectionOptionalRule(StandardInjectorConfiguration config) {
		config.injectionOptionalRules.add(e -> Optional.of(e.isAnnotationPresent(InjectionOptional.class)));
	}

	protected void addFixedConstructorInstantiatorFactory(StandardInjectorConfiguration config) {
		config.fixedConstructorInstantiatorFactoryRules.add(new DefaultFixedConstructorInstantiationRule(config));
	}

	protected void addSingletonInstantiationDynamicInitializer(StandardInjectorConfiguration config) {
		Injector injector = binder().getInjector();
		config.dynamicInitializers.add(() -> {
			injector.getDelegate().getCoreInjector().withRecipeCreationContext(ctx -> {
				for (Binding b : config.creationPipeline.staticBindings) {
					b.getScope().performEagerInstantiation(ctx, b);
				}
				return null;
			});
		});
	}

	protected void setMembersInjectorFactory(StandardInjectorConfiguration config) {
		config.membersInjectorFactory = new MembersInjectorFactory() {

			Injector injector = binder().getInjector();

			@Override
			public <T> Consumer<T> createMembersInjector(TypeToken<T> type) {
				MembersInjector<T> delegate = injector.getMembersInjector(type);
				return new Consumer<T>() {

					@Override
					public void accept(T t) {
						delegate.injectMembers(t);
					}

					@Override
					public String toString() {
						return delegate.toString();
					}
				};
			}
		};
	}

	protected void addTypeTokenConstructionRule(StandardInjectorConfiguration config) {

		config.creationPipeline.creationRules.add(new CreationRule() {

			@Override
			public Optional<Function<RecipeCreationContext, SupplierRecipe>> apply(CoreDependencyKey<?> key,
					CoreInjector injector) {

				if (TypeToken.class.equals(key.getRawType())) {
					TypeToken<?> type = key.getType().resolveType(TypeToken.class.getTypeParameters()[0]);

					return Optional.of(ctx -> new SupplierRecipeImpl(() -> type));
				}
				return Optional.empty();
			}
		});
	}

	protected void addProviderMethodBinderModulePostProcessor(StandardInjectorConfiguration config) {
		// register scanner for provides methods
		{
			ProviderMethodBinder b = new ProviderMethodBinder(config) {

				@Override
				protected boolean isProviderMethod(Method m) {
					if (!m.isAnnotationPresent(Provides.class)) {
						return false;
					}
					if (void.class.equals(m.getReturnType())) {
						throw new SaltaException("@Provides method returns void: " + m);
					}
					return true;
				}
			};
			config().modulePostProcessors.add(b::bindProviderMethodsOf);
		}
	}

	protected void addStaticMembersDynamicInitializer(StandardInjectorConfiguration config) {
		// register initializer for requested static injections
		StandardInjector injector = binder.getInjector().getDelegate();
		config.dynamicInitializers.add(() -> new StaticMembersInjectorBase() {
			@Override
			protected InjectionInstruction shouldInject(Method method) {
				return method.isAnnotationPresent(Inject.class) ? InjectionInstruction.INJECT
						: InjectionInstruction.NO_INJECT;
			}

			@Override
			protected InjectionInstruction shouldInject(Field field) {
				return field.isAnnotationPresent(Inject.class) ? InjectionInstruction.INJECT
						: InjectionInstruction.NO_INJECT;
			}
		}.injectStaticMembers(config, injector));
	}

	protected void addQualifierExtractors(StandardInjectorConfiguration config) {
		config.requiredQualifierExtractors.add(annotatedElement -> Arrays.stream(annotatedElement.getAnnotations())
				.filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)));

		config.availableQualifierExtractors.add(annotated -> Arrays.stream(annotated.getAnnotations())
				.filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)));
	}

	protected void addMembersInjectorCreationRule(StandardInjectorConfiguration config) {
		// rule for members injectors
		config.creationPipeline.creationRules.add(new MembersInjectorCreationRuleBase(config) {

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
					throw new SaltaException("Cannot inject a MembersInjector that has no type parameter");
				}
				TypeToken<?> dependency = key.getType().resolveType(MembersInjector.class.getTypeParameters()[0]);
				return dependency;
			}
		});
	}

	protected void addProviderCreationRule(StandardInjectorConfiguration config) {
		config.creationPipeline.creationRules.add(new ProviderCreationRule(key -> {
			return key.getRawType().equals(Provider.class);
		}, (type, supplier) -> new Provider<Object>() {
			@Override
			public Object get() {
				return supplier.get();
			}

			@Override
			public String toString() {
				return supplier.toString();
			}
		}, Provider.class));
	}

	protected void addPostConstructInitializerFactory(StandardInjectorConfiguration config) {
		config.construction.initializerFactories.add(new RecipeInitializerFactoryBase(config) {

			@Override
			protected boolean isInitializer(TypeToken<?> declaringType, Method method,
					MethodOverrideIndex overrideIndex) {

				if (method.isAnnotationPresent(PostConstruct.class)) {
					if (method.getTypeParameters().length > 0) {
						throw new SaltaException("@PostConstruct methods may not declare generic type parameters");
					}
					return !overrideIndex.isOverridden(method);
				}

				return false;
			}

		});
	}

	protected void addMembersInjectorFactory(StandardInjectorConfiguration config) {
		config.construction.membersInjectorFactories.add(new MembersInjectorFactoryBase(config) {

			@Override
			protected InjectionInstruction getInjectionInstruction(TypeToken<?> declaringType, Method method,
					MethodOverrideIndex index) {
				if (!method.isAnnotationPresent(Inject.class))
					return InjectionInstruction.NO_INJECTION;
				if (Modifier.isAbstract(method.getModifiers()))
					throw new SaltaException("Method annotated with @Inject is abstract: " + method);
				if (method.getTypeParameters().length > 0) {
					throw new SaltaException(
							"Method is annotated with @Inject but declares type parameters. Method:\n" + method);
				}
				if (index.isOverridden(method))
					return InjectionInstruction.NO_INJECTION;
				return config.isInjectionOptional(method) ? InjectionInstruction.INJECT_OPTIONAL
						: InjectionInstruction.INJECT;
			}

			@Override
			protected InjectionInstruction getInjectionInstruction(TypeToken<?> declaringType, Field field) {
				boolean annotationPresent = field.isAnnotationPresent(Inject.class);
				if (annotationPresent && Modifier.isFinal(field.getModifiers())) {
					throw new SaltaException("Final field annotated with @Inject");
				}
				if (Modifier.isStatic(field.getModifiers()))
					return InjectionInstruction.NO_INJECTION;
				if (!annotationPresent)
					return InjectionInstruction.NO_INJECTION;
				else
					return config.isInjectionOptional(field) ? InjectionInstruction.INJECT_OPTIONAL
							: InjectionInstruction.INJECT;
			}

		});
	}

	protected void addConstructionInstantiatorRule(StandardInjectorConfiguration config) {
		// default instantiator rule
		config.construction.instantiatorRules.add(new ConstructorInstantiatorRuleBase(config) {

			@Override
			protected Integer getConstructorPriority(Constructor<?> c) {
				if (c.isAnnotationPresent(Inject.class))
					return 2;
				boolean isInnerClass = c.getDeclaringClass().getEnclosingClass() != null;

				if (c.getParameterCount() == 0 && (Modifier.isPublic(c.getModifiers()) || isInnerClass))
					return 1;
				return null;
			}

		});
	}

	protected void addImplementedByConstructionRule(StandardInjectorConfiguration config) {
		config.construction.constructionRules.add(new ImplementedByConstructionRuleBase() {
			@Override
			protected DependencyKey<?> getImplementorKey(TypeToken<?> type) {
				ImplementedBy implementedBy = type.getRawType().getAnnotation(ImplementedBy.class);

				if (implementedBy != null)
					return DependencyKey.of(implementedBy.value());
				else
					return null;
			}

		});
	}

	protected void addProvidedByConstructionRule(StandardInjectorConfiguration config) {
		config.construction.constructionRules.add(new ProvidedByConstructionRuleBase(Supplier.class) {
			@Override
			protected DependencyKey<?> getProviderKey(TypeToken<?> type) {
				ProvidedBy providedBy = type.getRawType().getAnnotation(ProvidedBy.class);

				if (providedBy != null)
					return DependencyKey.of(providedBy.value());
				else
					return null;
			}
		});
	}
}
