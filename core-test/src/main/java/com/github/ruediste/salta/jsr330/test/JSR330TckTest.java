package com.github.ruediste.salta.jsr330.test;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.Engine;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.SpareTire;

import com.github.ruediste.salta.jsr330.AbstractModule;
import com.github.ruediste.salta.jsr330.Salta;
import com.github.ruediste.salta.jsr330.util.Names;

public class JSR330TckTest {

	public static junit.framework.Test suite() {
		Car car = Salta.createInjector(new AbstractModule() {

			@Override
			protected void configure() {
				bind(Car.class).to(Convertible.class);
				bind(Seat.class).annotatedWith(Drivers.class).to(DriversSeat.class);
				bind(Engine.class).to(V8Engine.class);
				bind(Tire.class).annotatedWith(Names.named("spare")).to(SpareTire.class);
				requestStaticInjection(Convertible.class);
				requestStaticInjection(Tire.class);
				requestStaticInjection(SpareTire.class);
			}
		}).getInstance(Car.class);
		return Tck.testsFor(car, true, true);
	}
}
