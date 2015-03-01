package com.github.ruediste.salta.core.compile;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class SupplierRecipeTest {

	public static class TestClass {
		String innerName;
		String outerName;

		public TestClass(String innerName, String outerName) {
			super();
			this.innerName = innerName;
			this.outerName = outerName;
		}

	}

	@Test
	@Ignore
	public void testMethodSplit() throws Throwable {
		SupplierRecipe innerNameRecipe = new SupplierRecipe() {

			@Override
			protected Class<?> compileImpl(GeneratorAdapter mv,
					MethodCompilationContext ctx) {
				ctx.addFieldAndLoad(int.class, 56);
				ctx.castToPublic(int.class, Object.class);
				mv.pop();
				ctx.addFieldAndLoad(String.class, ctx.getClassCtx()
						.getInternalClassName());
				return String.class;
			}
		};
		SupplierRecipe recipe = new SupplierRecipe(3) {

			@Override
			protected Class<?> compileImpl(GeneratorAdapter mv,
					MethodCompilationContext ctx) {
				mv.newInstance(Type.getType(TestClass.class));
				mv.dup();
				innerNameRecipe.compile(ctx);
				mv.checkCast(Type.getType(String.class));
				ctx.addFieldAndLoad(String.class, ctx.getClassCtx()
						.getInternalClassName());
				mv.invokeConstructor(Type.getType(TestClass.class),
						Method.getMethod("void <init>(String, String)"));
				return TestClass.class;
			}
		};
		TestClass test = (TestClass) new RecipeCompiler().compileSupplier(
				recipe).get();
		assertTrue("expected different classes",
				!test.innerName.equals(test.outerName));
	}
}
