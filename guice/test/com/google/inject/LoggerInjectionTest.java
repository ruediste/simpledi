package com.google.inject;

import java.util.logging.Logger;

import junit.framework.TestCase;

import com.google.inject.name.Names;

/**
 * Test built-in injection of loggers.
 *
 * @author jessewilson
 */
public class LoggerInjectionTest extends TestCase {

    @Inject
    Logger logger;

    public void testLoggerWithMember() {
        Injector injector = Guice.createInjector();
        injector.injectMembers(this);
        assertEquals("com.google.inject.LoggerInjectionTest", logger.getName());
    }

    public void testLoggerInConstructor() {
        Injector injector = Guice.createInjector();
        Foo foo = injector.getInstance(Foo.class);
        assertEquals("com.google.inject.LoggerInjectionTest$Foo",
                foo.logger.getName());
    }

    private static class Foo {
        Logger logger;

        @SuppressWarnings("unused")
        @Inject
        Foo(Logger logger) {
            this.logger = logger;
        }
    }

    public void testLoggerWithoutMember() {
        Injector injector = Guice.createInjector();
        assertNull(injector.getInstance(Logger.class).getName());
        assertNull(injector.getProvider(Logger.class).get().getName());
        assertNull(injector.getBinding(Logger.class).getProvider().get()
                .getName());
        assertEquals("Provider<java.util.logging.Logger>", injector
                .getProvider(Logger.class).toString());
    }

    public void testCanBindAnnotatedLogger() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Logger.class).annotatedWith(Names.named("anonymous"))
                        .toInstance(Logger.getAnonymousLogger());
            }
        });

        assertNull(injector.getInstance(
                Key.get(Logger.class, Names.named("anonymous"))).getName());
    }

}
