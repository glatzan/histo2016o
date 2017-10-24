package org.histo.dao;

import org.hibernate.Hibernate;
import org.histo.model.StainingPrototype;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class SettingsDAO extends AbstractDAO {

	
	public StainingPrototype initializeStainingPrototype(StainingPrototype stainingPrototype, boolean initialize) {
		stainingPrototype = reattach(stainingPrototype);

		if (initialize) {
			Hibernate.initialize(stainingPrototype.getBatchDetails());
		}

		return stainingPrototype;
	}

	
	public StainingPrototype getStainingPrototype(long id, boolean initialize) {

		StainingPrototype stainingPrototype = get(StainingPrototype.class, id);

		if (initialize) {
			Hibernate.initialize(stainingPrototype.getBatchDetails());
		}

		return stainingPrototype;
	}

}
