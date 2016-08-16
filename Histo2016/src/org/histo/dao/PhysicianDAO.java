package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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
	public List<Physician> getPhysicians(boolean surgeon, boolean extern, boolean other) {

		if (!surgeon && !extern && !other)
			return new ArrayList<>();

		Criteria c = getSession().createCriteria(Physician.class, "physician");
		c.addOrder(Order.asc("roleSurgeon"));
		c.addOrder(Order.asc("roleResidentDoctor"));

		Disjunction objDisjunction = Restrictions.disjunction();

		if (surgeon)
			objDisjunction.add(Restrictions.eq("roleSurgeon", true));
		if (extern)
			objDisjunction.add(Restrictions.eq("roleResidentDoctor", true));
		if (other)
			objDisjunction.add(Restrictions.eq("roleMiscellaneous", true));

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