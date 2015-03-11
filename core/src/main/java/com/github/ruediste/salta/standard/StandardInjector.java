package com.github.ruediste.salta.standard;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.inject.Provider;

import org.objectweb.asm.commons.GeneratorAdapter;

import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.CoreInjector;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.core.compile.FunctionRecipe;
import com.github.ruediste.salta.core.compile.MethodCompilationContext;
import com.github.ruediste.salta.standard.config.StandardInjectorConfiguration;
import com.github.ruediste.salta.standard.recipe.RecipeInitializer;
import com.github.ruediste.salta.standard.recipe.RecipeMembersInjector;
import com.google.common.reflect.TypeToken;

public class StandardInjector implements Injector {

	private final static class ClassDependencyKey<T> extends
			CoreDependencyKey<T> {

		private Class<T> type;

		ClassDependencyKey(Class<T> type) {
			this.type = type;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof ClassDependencyKey) {
				return type.equals(((ClassDependencyKey<?>) obj).type);
			}
			return false;

		}

		@Override
		public int hashCode() {
			return type.hashCode();
		}

		@Override
		public TypeToken<T> getType() {
			return TypeToken.of(type);
		}

		@Override
		public Class<T> getRawType() {
			return type;
		}

		@Override
		public AnnotatedElement getAnnotatedElement() {
			return new AnnotatedElement() {

				@Override
				public Annotation[] getDeclaredAnnotations() {
					return new Annotation[] {};
				}

				@Override
				public Annotation[] getAnnotations() {
					return getDeclaredAnnotations();
				}

				@Override
				public <A extends Annotation> A getAnnotation(
						Class<A> annotationClass) {
					return null;
				}
			};
		}
	}

	private final class ProviderImpl<T> implements Provider<T> {

		volatile private Supplier<T> supplier;
		private CoreDependencyKey<T> key;

		public ProviderImpl(CoreDependencyKey<T> key) {
			this.key = key;
		}

		@Override
		public T get() {
			checkInitialized();
			if (supplier == null) {
				synchronized (coreInjector.recipeLock) {
					if (supplier == null)
						supplier = coreInjector.getInstanceSupplier(key);
				}
			}
			return supplier.get();
		}

		@Override
		public String toString() {
			return "Provider<" + key + ">";
		}
	}

	private ConcurrentHashMap<TypeToken<?>, MembersInjector<?>> membersInjectorCache = new ConcurrentHashMap<>();
	private HashMap<TypeToken<?>, FunctionRecipe> membersInjectionRecipeCache = new HashMap<>();

	private boolean initialized;
	private StandardInjectorConfiguration config;
	private CoreInjector coreInjector;

	private void checkInitialized() {
		if (!initialized) {
			throw new SaltaException(
					"Cannot use injector before it is initialized");
		}
	}

	public void initialize(StandardInjectorConfiguration config) {
		this.config = config;

		config.postProcessModules();

		coreInjector = new CoreInjector(config.config);
		for (Consumer<Injector> initializer : config.staticInitializers) {
			initializer.accept(this);
		}
		initialized = true;
		while (!config.dynamicInitializers.isEmpty()) {
			ArrayList<Consumer<Injector>> tmp = new ArrayList<>(
					config.dynamicInitializers);
			config.dynamicInitializers.clear();
			for (Consumer<Injector> initializer : tmp) {
				initializer.accept(this);
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void injectMembers(Object instance) {
		injectMembers((TypeToken) TypeToken.of(instance.getClass()), instance);
	}

	@Override
	public <T> void injectMembers(TypeToken<T> type, T instance) {
		checkInitialized();
		getMembersInjector(type).injectMembers(instance);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> MembersInjector<T> getMembersInjector(TypeToken<T> typeLiteral) {

		return (MembersInjector<T>) membersInjectorCache.computeIfAbsent(
				typeLiteral, type -> new MembersInjectorImpl<T>(type, this,
						coreInjector));
	}

	@Override
	public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
		return getMembersInjector(TypeToken.of(type));
	}

	@Override
	public FunctionRecipe getMembersInjectionRecipe(TypeToken<?> typeToken,
			RecipeCreationContext ctx) {
		FunctionRecipe recipe = membersInjectionRecipeCache.get(typeToken);
		if (recipe != null)
			return recipe;

		List<RecipeMembersInjector> injectors = config
				.createRecipeMembersInjectors(ctx, typeToken);
		List<RecipeInitializer> initializers = config.createInitializers(ctx,
				typeToken);
		recipe = new FunctionRecipe() {

			@Override
			public Class<?> compileImpl(Class<?> argumentType,
					GeneratorAdapter mv, MethodCompilationContext ctx) {
				for (RecipeMembersInjector rmi : injectors) {
					argumentType = rmi.compile(argumentType, ctx);
				}
				for (RecipeInitializer initializer : initializers)
					argumentType = initializer.compile(argumentType, ctx);
				return argumentType;
			}
		};
		membersInjectionRecipeCache.put(typeToken, recipe);
		return recipe;
	}

	@Override
	public <T> Provider<T> getProvider(CoreDependencyKey<T> key) {

		return new ProviderImpl<T>(key);
	}

	@Override
	public <T> Provider<T> getProvider(Class<T> type) {
		return getProvider(DependencyKey.of(type));
	}

	@Override
	public <T> T getInstance(CoreDependencyKey<T> key) {

		checkInitialized();
		return coreInjector.getInstance(key);
	}

	@Override
	public <T> T getInstance(Class<T> type) {
		checkInitialized();
		return coreInjector.getInstance(new ClassDependencyKey<T>(type));
	}

	@Override
	public CoreInjector getCoreInjector() {
		return coreInjector;
	}
}
