package com.github.ruediste.salta.standard.config;

import org.mockito.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.github.ruediste.salta.core.Binding;
import com.github.ruediste.salta.core.CreationRecipe;
import com.github.ruediste.salta.core.RecipeCompilationContext;
import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.standard.Scope;
import com.google.common.reflect.TypeToken;

public class SingletonScope implements Scope {

	@Override
	public String toString() {
		return "Singleton";
	}

	@Override
	public CreationRecipe createRecipe(RecipeCreationContext ctx,
			Binding binding, TypeToken<?> type, CreationRecipe innerRecipe) {
		Object instance = ctx.compileRecipe(innerRecipe).getNoThrow();
		return new CreationRecipe() {

			@Override
			public void compile(GeneratorAdapter mv,
					RecipeCompilationContext compilationContext) {
				compilationContext.addFieldAndLoad(
						Type.getDescriptor(type.getRawType()), instance);
			}
		};
	}
}