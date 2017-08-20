package org.histo.config.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class RootAwareEventListenerIntegrator implements org.hibernate.integrator.spi.Integrator {

	public static final RootAwareEventListenerIntegrator INSTANCE = new RootAwareEventListenerIntegrator();

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {

		final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);

//		eventListenerRegistry.appendListeners(EventType.PERSIST, RootAwareInsertEventListener.INSTANCE);
//		eventListenerRegistry.appendListeners(EventType.FLUSH_ENTITY, RootAwareUpdateAndDeleteEventListener.INSTANCE);
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// Do nothing
	}
}