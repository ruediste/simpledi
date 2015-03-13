package com.github.ruediste.salta.standard.util;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.objectweb.asm.commons.GeneratorAdapter;

import com.github.ruediste.salta.core.CompiledFunction;
import com.github.ruediste.salta.core.CoreDependencyKey;
import com.github.ruediste.salta.core.CreationRule;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.core.compile.FunctionRecipe;
import com.github.ruediste.salta.core.compile.MethodCompilationContext;
import com.github.ruediste.salta.core.compile.SupplierRecipe;
import com.github.ruediste.salta.standard.config.StandardInjectorConfiguration;
import com.github.ruediste.salta.standard.recipe.RecipeInitializer;
import com.github.ruediste.salta.standard.recipe.RecipeMembersInjector;
import com.google.common.reflect.TypeToken;

public abstract class MembersInjectorCreationRuleBase implements CreationRule {

	private ConcurrentHashMap<TypeToken<?>, Consumer<?>> membersInjectorCache = new ConcurrentHashMap<>();
	private HashMap<TypeToken<?>, FunctionRecipe> membersInjectionRecipeCache = new HashMap<>();
	private StandardInjectorConfiguration config;

	public MembersInjectorCreationRuleBase(StandardInjectorConfiguration config) {
		this.config = config;
	}

	@SuppressWarnings("unchecked")
	public <T> Consumer<T> getMembersInjector(TypeToken<T> typeLiteral,
			RecipeCreationContext ctx) {

		return (Consumer<T>) membersInjectorCache.computeIfAbsent(typeLiteral,
				type -> getMembersInjectorNoCache(type, ctx));
	}

	public <T> Consumer<T> getMembersInjectorNoCache(TypeToken<T> type,
			RecipeCreationContext ctx) {
		CompiledFunction function = ctx.getCompiler().compileFunction(
				getMembersInjectionRecipe(type, ctx));
		return new Consumer<T>() {

			@Override
			public void accept(T t) {
				function.getNoThrow(t);
			}

			@Override
			public String toString() {
				return "MembersInjector<" + type + ">";
			}
		};
	}

	public FunctionRecipe getMembersInjectionRecipe(TypeToken<?> typeToken,
			RecipeCreationContext ctx) {
		FunctionRecipe recipe = membersInjectionRecipeCache.get(typeToken);
		if (recipe != null)
			return recipe;

		try {
			List<RecipeMembersInjector> injectors = config
					.createRecipeMembersInjectors(ctx, typeToken);
			List<RecipeInitializer> initializers = config.createInitializers(
					ctx, typeToken);
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
		} catch (Throwable t) {
			throw new SaltaException(
					"Error while creating MembersInjection recipe for "
							+ typeToken, t);
		}
		membersInjectionRecipeCache.put(typeToken, recipe);
		return recipe;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public SupplierRecipe apply(CoreDependencyKey<?> key,
			RecipeCreationContext ctx) {
		TypeToken<?> dependency = getDependency(key);
		if (dependency == null)
			return null;

		Consumer<?> saltaMembersInjector = getMembersInjector(dependency, ctx);

		Object wrappedInjector = wrapInjector((Consumer) saltaMembersInjector);
		return new SupplierRecipe() {

			@Override
			protected Class<?> compileImpl(GeneratorAdapter mv,
					MethodCompilationContext ctx) {
				Class<?> wrappedInjectorType = getWrappedInjectorType();
				ctx.addFieldAndLoad((Class) wrappedInjectorType,
						wrappedInjector);
				return wrappedInjectorType;
			}

		};
	}

	protected abstract TypeToken<?> getDependency(CoreDependencyKey<?> key);

	protected abstract Object wrapInjector(Consumer<Object> saltaMembersInjector);

	protected abstract Class<?> getWrappedInjectorType();

}