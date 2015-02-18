package com.github.ruediste.salta.standard.recipe;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.github.ruediste.salta.core.CreationRecipe;
import com.github.ruediste.salta.core.RecipeCompilationContext;
import com.github.ruediste.salta.standard.util.ConstructorInstantiatorRuleBase;

/**
 * Instantiate a fixed class using a fixed constructor. Use a subclass of
 * {@link ConstructorInstantiatorRuleBase} to create an instance
 */
public class FixedConstructorRecipeInstantiator implements RecipeInstantiator {

	Constructor<?> constructor;
	List<CreationRecipe> argumentDependencies;

	public FixedConstructorRecipeInstantiator(Constructor<?> constructor,
			List<CreationRecipe> argumentDependencies) {
		constructor.setAccessible(true);
		this.constructor = constructor;
		this.argumentDependencies = new ArrayList<>(argumentDependencies);
	}

	@Override
	public void compile(GeneratorAdapter mv,
			RecipeCompilationContext compilationContext) {
		if (isConstructorAccessible())
			compileDirect(mv, compilationContext);
		else
			compileReflection(mv, compilationContext);
	}

	boolean isConstructorAccessible() {
		if (Modifier.isPublic(constructor.getModifiers())) {
			Class<?> declaringClass = constructor.getDeclaringClass();
			do {
				if (!Modifier.isPublic(declaringClass.getModifiers()))
					return false;
				declaringClass = declaringClass.getEnclosingClass();
			} while (declaringClass != null);

			return true;
		}
		return false;
	}

	private void compileReflection(GeneratorAdapter mv,
			RecipeCompilationContext compilationContext) {

		// push constructor
		compilationContext.addFieldAndLoad(Type.getDescriptor(Constructor.class),
				constructor);

		// push dependencies as an array
		mv.push(argumentDependencies.size());
		mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

		for (int i = 0; i < argumentDependencies.size(); i++) {
			mv.dup();
			mv.push(i);
			CreationRecipe dependency = argumentDependencies.get(i);
			dependency.compile(mv, compilationContext);
			mv.visitInsn(AASTORE);
		}

		// call constructor
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l1,
				Type.getInternalName(InvocationTargetException.class));
		mv.visitLabel(l0);
		mv.invokeVirtual(Type.getType(Constructor.class), new Method(
				"newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;"));
		mv.goTo(l2);
		mv.visitLabel(l1);
		mv.visitMethodInsn(INVOKEVIRTUAL,
				Type.getInternalName(InvocationTargetException.class),
				"getCause", "()Ljava/lang/Throwable;", false);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l2);
	}

	private void compileDirect(GeneratorAdapter mv,
			RecipeCompilationContext compilationContext) {

		mv.newInstance(Type.getType(constructor.getDeclaringClass()));
		mv.dup();

		// push dependencies

		for (int i = 0; i < argumentDependencies.size(); i++) {
			CreationRecipe dependency = argumentDependencies.get(i);
			dependency.compile(mv, compilationContext);
		}

		// call constructor
		mv.invokeConstructor(Type.getType(constructor.getDeclaringClass()),
				Method.getMethod(constructor));
	}

}