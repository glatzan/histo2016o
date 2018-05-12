package org.histo.service.impl;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.histo.config.ResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Basis class for Services
 * 
 * @author andi
 *
 */
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
@Getter
@Setter
public class AbstractService {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	protected SessionFactory sessionFactory;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	protected ResourceBundle resourceBundle;

	protected Session currentSession() {
		return sessionFactory.getCurrentSession();
	}
}
