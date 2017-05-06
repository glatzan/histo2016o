package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.ContactRole;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
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
	public List<Physician> getPhysicians(ContactRole role, boolean archived) {
		return getPhysicians(new ContactRole[]{role}, archived);
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

		
		DetachedCriteria query = DetachedCriteria.forClass(Physician.class,"physician");
		query.addOrder(Order.asc("clinicEmployee"));
		query.createAlias("physician.associatedRoles", "a");

		// don't select archived physicians
		if (!archived)
			query.add(Restrictions.eq("archived", false));

		query.add(Restrictions.in("a.elements", roles));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		
		List<Physician> result = query.getExecutableCriteria(getSession()).list();
		
		return result;
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

	/**
	 * Gets a physician for the provided person, returns null if none physician
	 * is available
	 * 
	 * @param person
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Physician getPhysicianByPerson(Person person) {

		DetachedCriteria query = DetachedCriteria.forClass(Physician.class, "physician");

		query.add(Restrictions.eq("person", person));

		List<Physician> result = query.getExecutableCriteria(getSession()).list();

		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

}