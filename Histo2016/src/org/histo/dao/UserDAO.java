package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.CustomAuthenticationProvider;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.model.HistoUser;
import org.histo.model.Physician;
import org.histo.model.interfaces.LogInfo;
import org.histo.model.util.LogListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserDAO extends AbstractDAO implements Serializable {

	@SuppressWarnings("unchecked")
	public List<HistoUser> loadAllUsers() {
		DetachedCriteria query = DetachedCriteria.forClass(HistoUser.class);
		query.addOrder(Order.asc("id"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return query.getExecutableCriteria(getSession()).list();
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
	 * Workaround because there is no session within the
	 * {@link CustomAuthenticationProvider}
	 * 
	 * @param histoUser
	 * @param logMessage
	 * @return
	 */
	public Object saveUser(Object histoUser, String logMessage) {
		Session session = getSession();
		try {
			// setting log info
			SecurityContextHolderUtil.setObjectToSecurityContext(LogListener.LOG_KEY_INFO, new LogInfo(logMessage));
			session.saveOrUpdate(histoUser);
		} catch (HibernateException hibernateException) {
			histoUser = (HistoUser) session.merge(histoUser);
			hibernateException.printStackTrace();
		}
		return histoUser;
	}

}
