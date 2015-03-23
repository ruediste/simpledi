package com.github.ruediste.salta.core;

import com.github.ruediste.salta.core.compile.SupplierRecipe;
import com.google.common.reflect.TypeToken;

/**
 * A Scope defines a visibility for an instance. The scope can either reuse an
 * instance or decide to create a new instance.
 */
public interface Scope {

	/**
	 * Create a recipe. No incoming parameter is on the stack. The scoped
	 * instance is expected afterwards. The calling thread always holds the
	 * {@link CoreInjector#recipeLock}
	 * 
	 * @param binding
	 *            binding which is beeing scoped
	 * @param boundType
	 *            type the binding was created for
	 * @return
	 */
	SupplierRecipe createRecipe(RecipeCreationContext ctx, Binding binding,
			TypeToken<?> requestedType);

	/**
	 * Perform an eager instantiation if applicable for this scope. Only called
	 * if eager instantiations should actually be perfomed, so the scope does
	 * not have to check a configuration by itself.
	 * 
	 * @param ctx
	 *            TODO
	 */
	default void performEagerInstantiation(RecipeCreationContext ctx,
			Binding binding) {
	}

}
