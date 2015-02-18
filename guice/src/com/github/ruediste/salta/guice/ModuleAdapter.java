package com.github.ruediste.salta.guice;

import com.github.ruediste.salta.guice.binder.BinderImpl;
import com.github.ruediste.salta.guice.binder.GuiceInjectorConfiguration;
import com.github.ruediste.salta.standard.Module;
import com.github.ruediste.salta.standard.binder.Binder;

public class ModuleAdapter implements Module {
	private com.google.inject.Module delegate;
	private GuiceInjectorConfiguration config;

	public ModuleAdapter(com.google.inject.Module delegate,
			GuiceInjectorConfiguration config) {
		this.delegate = delegate;
		this.config = config;
	}

	@Override
	public void configure(Binder binder) {
		delegate.configure(new BinderImpl(binder, config));
	}

}