package com.github.ruediste.salta.standard.recipe;

import java.lang.reflect.Method;
import java.util.List;

import com.github.ruediste.salta.core.InjectionStrategy;
import com.github.ruediste.salta.core.compile.SupplierRecipe;

public class FixedMethodRecipeMembersInjector extends
		FixedMethodInvocationFunctionRecipe implements RecipeMembersInjector {

	public FixedMethodRecipeMembersInjector(Method method,
			List<SupplierRecipe> argumentRecipes,
			InjectionStrategy injectionStrategy) {
		super(method, argumentRecipes, injectionStrategy, true);
	}

}
