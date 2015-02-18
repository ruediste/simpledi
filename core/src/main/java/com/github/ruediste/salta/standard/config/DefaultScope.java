package com.github.ruediste.salta.standard.config;

import com.github.ruediste.salta.core.Binding;
import com.github.ruediste.salta.core.CreationRecipe;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.standard.Scope;
import com.google.common.reflect.TypeToken;

final class DefaultScope implements Scope {

	@Override
	public String toString() {
		return "Default";
	}

	@Override
	public CreationRecipe createRecipe(RecipeCreationContext ctx,
			Binding binding, TypeToken<?> type, CreationRecipe innerRecipe) {
		return innerRecipe;
	}
}