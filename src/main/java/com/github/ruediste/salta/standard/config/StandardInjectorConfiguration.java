package com.github.ruediste.salta.standard.config;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.ruediste.attachedProperties4J.AttachedProperty;
import com.github.ruediste.salta.core.Binding;
import com.github.ruediste.salta.core.BindingContext;
import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.CoreInjectorConfiguration;
import com.github.ruediste.salta.core.ProvisionException;
import com.github.ruediste.salta.core.Scope;
import com.github.ruediste.salta.standard.Injector;
import com.github.ruediste.salta.standard.Message;
import com.github.ruediste.salta.standard.ScopeRule;
import com.github.ruediste.salta.standard.Stage;
import com.github.ruediste.salta.standard.recipe.RecipeInjectionListener;
import com.github.ruediste.salta.standard.recipe.RecipeInstantiator;
import com.github.ruediste.salta.standard.recipe.RecipeMembersInjector;
import com.github.ruediste.salta.standard.recipe.RecipeMembersInjectorFactory;
import com.google.common.reflect.TypeToken;

public class StandardInjectorConfiguration {
	public CoreInjectorConfiguration config;
	public final Stage stage;

	public StandardInjectorConfiguration(Stage stage,
			CoreInjectorConfiguration config) {
		this.stage = stage;
		this.config = config;
	}

	public Scope singletonScope = new SingletonScope();
	public Scope defaultScope = new DefaultScope();
	public final Map<Class<? extends Annotation>, Scope> scopeAnnotationMap = new HashMap<>();

	/**
	 * Strategy to create a {@link RecipeInstantiator} given a constructor.
	 */
	public BiFunction<Constructor<?>, TypeToken<?>, RecipeInstantiator> fixedConstructorInstantiatorFactory;

	public final List<InstantiatorRule> instantiatorRules = new ArrayList<>();

	/**
	 * Rules used to create {@link RecipeMembersInjector}s for a type. The
	 * injectors of the first one returning a non-null result are used.
	 */
	public final List<MembersInjectorRule> membersInjectorRules = new ArrayList<>();

	/**
	 * Default list of factories used to create {@link RecipeMembersInjector}s
	 * for a given type. These factories are used if no member of the
	 * {@link #membersInjectorRules} matches for a type.
	 */
	public final List<RecipeMembersInjectorFactory> defaultMembersInjectorFactories = new ArrayList<>();

	/**
	 * Create a list of members injectors based on the
	 * {@link #membersInjectorRules} and as fallback the
	 * {@link #defaultMembersInjectorFactories}
	 */
	public List<RecipeMembersInjector> createRecipeMembersInjectors(
			BindingContext ctx, TypeToken<?> type) {
		// test rules
		for (MembersInjectorRule rule : membersInjectorRules) {
			List<RecipeMembersInjector> membersInjectors = rule
					.getMembersInjectors(type);
			if (membersInjectors != null)
				return membersInjectors;
		}

		// use default factories
		ArrayList<RecipeMembersInjector> result = new ArrayList<>();
		for (RecipeMembersInjectorFactory factory : defaultMembersInjectorFactories) {
			result.addAll(factory.createInjectors(ctx, type));
		}
		return result;
	}

	public final List<InjectionListenerRule> injectionListenerRules = new ArrayList<>();

	public List<RecipeInjectionListener> createInjectionListeners(
			BindingContext ctx, TypeToken<?> type) {
		return injectionListenerRules.stream()
				.map(r -> r.getListener(ctx, type)).filter(x -> x != null)
				.collect(toList());
	}

	/**
	 * List of rules to determine the scope used for a type.
	 */
	public final List<ScopeRule> scopeRules = new ArrayList<>();

	public final Set<Class<?>> requestedStaticInjections = new HashSet<>();

	/**
	 * Create an {@link RecipeInstantiator} using the {@link #instantiatorRules}
	 * 
	 * @param ctx
	 */
	public <T> RecipeInstantiator createRecipeInstantiator(BindingContext ctx,
			TypeToken<?> type) {
		for (InstantiatorRule rule : instantiatorRules) {
			RecipeInstantiator instantiator = rule.apply(ctx, type);
			if (instantiator != null) {
				return instantiator;
			}
		}
		return null;
	}

	/**
	 * Get the scope for a type based on the {@link #scopeRules} followed by the
	 * {@link #scopeAnnotationMap}, falling back to the {@link #defaultScope}
	 */
	public Scope getScope(TypeToken<?> type) {
		Scope scope = null;

		// evaluate scope rules
		for (ScopeRule rule : scopeRules) {
			scope = rule.getScope(type);
			if (scope != null)
				return scope;
		}

		// scan scope annotation map
		for (Entry<Class<? extends Annotation>, Scope> entry : scopeAnnotationMap
				.entrySet()) {
			if (type.getRawType().isAnnotationPresent(entry.getKey())) {
				if (scope != null)
					throw new ProvisionException(
							"Multiple scope annotations present on " + type);
				scope = entry.getValue();
			}
		}

		if (scope == null)
			scope = defaultScope;
		return scope;
	}

	public Scope getScope(Class<? extends Annotation> scopeAnnotation) {
		Scope scope = scopeAnnotationMap.get(scopeAnnotation);
		if (scope == null)
			throw new ProvisionException("Unknown scope annotation "
					+ scopeAnnotation);
		return scope;
	}

	private static final class DefaultScope implements Scope {
		@Override
		public <T> T scope(Binding key, Supplier<T> unscoped) {
			return unscoped.get();
		}

		@Override
		public String toString() {
			return "Default";
		}
	}

	private static final class SingletonScope implements Scope {

		private final AttachedProperty<Binding, Object> instance = new AttachedProperty<>(
				"instance");

		Object lock = new Object();

		@SuppressWarnings("unchecked")
		@Override
		public <T> T scope(Binding key, Supplier<T> unscoped) {

			if (instance.isSet(key))
				return (T) instance.get(key);

			synchronized (lock) {
				if (instance.isSet(key))
					return (T) instance.get(key);
				T value = unscoped.get();
				instance.set(key, value);
				return value;
			}
		}

		@Override
		public String toString() {
			return "Singleton";
		}
	}

	/**
	 * Collect error messages to be shown when attempting to create the
	 * injector. Do not add errors while creating the injector.
	 */
	public final List<Message> errorMessages = new ArrayList<>();

	/**
	 * Initializers run once the Injector is constructed. These initializers may
	 * not create request instances from the injector.
	 */
	public final List<Consumer<Injector>> staticInitializers = new ArrayList<>();

	/**
	 * Initializers run once the Injector is constructed. Run after the
	 * {@link #staticInitializers}. These initializers can freely use the
	 * injector
	 */
	public final List<Consumer<Injector>> dynamicInitializers = new ArrayList<>();

	/**
	 * List of dependencies which sould be created after the creation of the
	 * injector
	 */
	public final List<CoreDependencyKey<?>> requestedEagerInstantiations = new ArrayList<>();

	public boolean requireAtInjectOnConstructors;
}
