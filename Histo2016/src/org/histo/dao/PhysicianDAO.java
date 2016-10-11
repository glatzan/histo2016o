package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.ContactRole;
import org.histo.model.Physician;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PhysicianDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7297474866699408016L;

	/**
	 * Gets a list of physicians which are associated with one role in the
	 * Contactrole Array. If archived is true, even archived physicians will be
	 * Returned.
	 * 
	 * @param roles
	 * @param archived
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(List<ContactRole> roles, boolean archived) {
		ContactRole[] contactRoles = new ContactRole[roles.size()];
		roles.toArray(contactRoles);
		return getPhysicians(contactRoles, archived);
	}

	/**
	 * Gets a list of physicians which are associated with one role in the
	 * Contactrole Array. If archived is true, even archived physicians will be
	 * Returned.
	 * 
	 * @param roles
	 * @param archived
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(ContactRole[] roles, boolean archived) {

		if (roles == null || roles.length == 0)
			return new ArrayList<>();

		Criteria c = getSession().createCriteria(Physician.class);
		c.addOrder(Order.asc("defaultContactRole"));
		c.addOrder(Order.asc("clinicEmployee"));

		// don't select archived physicians
		if (!archived)
			c.add(Restrictions.eq("archived", false));

		Disjunction objDisjunction = Restrictions.disjunction();

		for (ContactRole contactRole : roles) {
			objDisjunction.add(Restrictions.eq("defaultContactRole", contactRole));
		}

		c.add(objDisjunction);

		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return (List<Physician>) c.list();
	}

	/**
	 * Load a physician with the given uid. Is used if a physician was added as
	 * a surgeon and no login was performed. Is located in UserDao because of
	 * the scope.
	 * 
	 * @param uid
	 * @return
	 */
	public Physician loadPhysicianByUID(String uid) {
		Criteria c = getSession().createCriteria(Physician.class);
		c.add(Restrictions.eq("uid", uid)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<Physician> res = c.list();

		if (res.size() != 1) {
			return null;
		}

		return res.get(0);
	}

}