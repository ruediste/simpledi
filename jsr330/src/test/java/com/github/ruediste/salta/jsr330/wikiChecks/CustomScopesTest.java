package com.github.ruediste.salta.jsr330.wikiChecks;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Scope;

import org.junit.Before;
import org.junit.Test;

import com.github.ruediste.salta.core.SaltaException;
import com.github.ruediste.salta.jsr330.AbstractModule;
import com.github.ruediste.salta.jsr330.Injector;
import com.github.ruediste.salta.jsr330.Salta;
import com.github.ruediste.salta.standard.ScopeImpl;
import com.github.ruediste.salta.standard.util.SimpleScopeHandler;

public class CustomScopesTest {

    private Injector injector;

    @Inject
    @Named("batchScope")
    private SimpleScopeHandler handler;

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Scope
    private @interface BatchScoped {
    }

    @Before
    public void before() {
        injector = Salta.createInjector(new AbstractModule() {

            @Override
            protected void configure() throws Exception {
                SimpleScopeHandler handler = new SimpleScopeHandler();
                bind(SimpleScopeHandler.class).named("batchScope").toInstance(
                        handler);
                bindScope(BatchScoped.class, new ScopeImpl(handler));
            }
        });

        injector.injectMembers(this);
    }

    @BatchScoped
    private static class A {
    }

    @Test
    public void testCustomScope() {
        handler.enter();
        A a1 = injector.getInstance(A.class);
        A a2 = injector.getInstance(A.class);
        assertSame(a1, a2);
        handler.exit();

        handler.enter();
        A a3 = injector.getInstance(A.class);
        assertNotSame(a3, a1);
        handler.exit();
    }

    @Test
    public void accessOutsideScopeFails() {
        try {
            injector.getInstance(A.class);
            fail();
        } catch (SaltaException e) {
            if (!e.getMessage().contains("outside of a scoping block"))
                throw e;
        }
    }
}