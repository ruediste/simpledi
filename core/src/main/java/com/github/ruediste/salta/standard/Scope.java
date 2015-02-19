package com.github.ruediste.salta.standard;

import com.github.ruediste.salta.core.Binding;
import com.github.ruediste.salta.core.CreationRecipe;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.google.common.reflect.TypeToken;

/**
 * A Scope defines a visibility for an instance. The scope can either reuse an
 * instance or decide to create a new instance.
 */
public interface Scope {

	/**
	 * Create a recipe. No incoming parameter on the stack. The scoped instance
	 * is expected afterwards
	 * 
	 * @param ctx
	 * @param binding
	 *            binding which is beeing scoped
	 * @param type
	 *            type the binding was created for
	 * @param innerRecipe
	 *            recipe resulting in the unscoped instance
	 * @return
	 */
	CreationRecipe createRecipe(RecipeCreationContext ctx, Binding binding,
			TypeToken<?> type, CreationRecipe innerRecipe);

}
