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

	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(List<ContactRole> roles) {
		ContactRole[] contactRoles = new ContactRole[roles.size()];
		roles.toArray(contactRoles);
		return getPhysicians(contactRoles);
	}
	
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(ContactRole[] roles) {

		if (roles == null)
			return new ArrayList<>();

		Criteria c = getSession().createCriteria(Physician.class, "physician");
		c.addOrder(Order.asc("defaultcontactrole;"));
		c.addOrder(Order.asc("clinicemployee;"));

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