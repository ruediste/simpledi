package com.github.ruediste.salta.standard.config;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste.salta.Salta;
import com.github.ruediste.salta.jsr330.JSR330Module;
import com.github.ruediste.salta.standard.Injector;

public class SingletonScopeTest {

	private Injector injector;

	@Singleton
	private static class TestClass {
		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

	}

	private static class TestClassB {
		@Inject
		TestClass a;

		@Inject
		TestClass b;

		int compare() {
			assertEquals(a.getValue(), b.getValue());
			return a.getValue();
		}
	}

	@Before
	public void setup() {
		injector = Salta.createInjector(new JSR330Module());
	}

	@Test
	public void testGetInstance() {
		assertEquals(0, injector.getInstance(TestClass.class).getValue());
		injector.getInstance(TestClass.class).setValue(5);
		assertEquals(5, injector.getInstance(TestClass.class).getValue());
	}

	@Test
	public void testInjection() {
		assertEquals(0, injector.getInstance(TestClassB.class).compare());
		injector.getInstance(TestClass.class).setValue(5);
		assertEquals(5, injector.getInstance(TestClassB.class).compare());
	}
}