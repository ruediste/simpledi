package com.github.ruediste.salta.standard.recipe;

import com.github.ruediste.salta.core.ContextualInjector;
import com.github.ruediste.salta.core.InstantiationContext;

public interface RecipeMembersInjector<T> {

	void injectMembers(T instance, ContextualInjector injector);

	TransitiveMembersInjector createTransitive(InstantiationContext ctx);

}
