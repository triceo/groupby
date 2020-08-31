package org.optaplanner.examples.cloudbalancing;

import java.util.function.Supplier;

import org.kie.api.runtime.KieSession;

@FunctionalInterface
public interface KieSessionSupplier extends Supplier<KieSession> {

}
