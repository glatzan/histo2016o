package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.histo.model.Physician;
import org.histo.model.HistoUser;
import org.histo.model.patient.Diagnosis;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserDAO extends AbstractDAO implements Serializable {

	public List<HistoUser> loadAllUsers() {
		Criteria c = getSession().createCriteria(HistoUser.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public HistoUser loadUserByName(String name) {
		Criteria c = getSession().createCriteria(HistoUser.class);
		c.add(Restrictions.eq("username", name)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<HistoUser> res = c.list();

		if (res.size() != 1) {
			return null;
		}

		return res.get(0);
	}

	/**
	 * Load a physician with the given uid. Is used if a physician was added as a surgeon and 
	 * no login was performed. Is located in UserDao because of the scope.
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

	public HistoUser saveUser(HistoUser histoUser) {
		Session session = getSession();
		try {
			session.saveOrUpdate(histoUser);
		} catch (HibernateException hibernateException) {
			histoUser = (HistoUser) session.merge(histoUser);
			hibernateException.printStackTrace();
		}
		return histoUser;
	}
	
}
