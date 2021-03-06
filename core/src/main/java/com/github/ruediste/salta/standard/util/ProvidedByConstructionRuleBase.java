package com.github.ruediste.salta.standard.util;

import java.util.Optional;
import java.util.function.Function;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.github.ruediste.salta.core.RecipeCreationContext;
import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.core.compile.MethodCompilationContext;
import com.github.ruediste.salta.core.compile.SupplierRecipe;
import com.github.ruediste.salta.standard.DependencyKey;
import com.github.ruediste.salta.standard.config.ConstructionRule;
import com.github.ruediste.salta.standard.recipe.RecipeInstantiator;
import com.google.common.reflect.TypeToken;

public abstract class ProvidedByConstructionRuleBase implements ConstructionRule {

    private Class<?> providerClass;
    private String methodName;
    private Class<?> returnType;
    private Class<?>[] parameterTypes;

    public ProvidedByConstructionRuleBase(Class<?> providerClass) {
        this(providerClass, "get", Object.class);
    }

    public ProvidedByConstructionRuleBase(Class<?> providerClass, String methodName, Class<?> returnType,
            Class<?>... argumentTypes) {
        this.providerClass = providerClass;
        this.methodName = methodName;
        this.returnType = returnType;
        this.parameterTypes = argumentTypes;
    }

    /**
     * Get the provider key to be used. If null is returned, the rule does not
     * match
     */
    protected abstract DependencyKey<?> getProviderKey(TypeToken<?> type);

    @Override
    public Optional<Function<RecipeCreationContext, SupplierRecipe>> createConstructionRecipe(TypeToken<?> type) {

        DependencyKey<?> providerKey = getProviderKey(type);
        if (providerKey != null) {
            return Optional.of(ctx -> {
                SupplierRecipe recipe = ctx.getRecipe(providerKey);
                return new RecipeInstantiator() {

                    @Override
                    public Class<?> compileImpl(GeneratorAdapter mv, MethodCompilationContext compilationContext) {
                        recipe.compile(compilationContext);
                        Method method;
                        try {
                            method = Method.getMethod(providerClass.getMethod(methodName, parameterTypes));
                        } catch (NoSuchMethodException | SecurityException e) {
                            throw new SaltaException("Error while retrieving method", e);
                        }
                        mv.invokeInterface(Type.getType(providerClass), method);
                        return returnType;
                    }
                };
            });
        }

        return Optional.empty();
    }
}