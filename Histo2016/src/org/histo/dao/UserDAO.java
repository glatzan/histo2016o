package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.histo.model.UserAcc;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserDAO extends AbstractDAO implements Serializable {

	public List<UserAcc> loadAllUsers() {
		Criteria c = getSession().createCriteria(UserAcc.class).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public UserAcc loadUserByName(String name) {
		Criteria c = getSession().createCriteria(UserAcc.class);
		c.add(Restrictions.eq("username", name)).setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<UserAcc> res = c.list();

		if (res.size() != 1) {
			return null;
		}

		return res.get(0);
	}

	public UserAcc saveUser(UserAcc userAcc) {
		Session session = getSession();
		try {
			session.saveOrUpdate(userAcc);
		} catch (HibernateException hibernateException) {
			userAcc = (UserAcc) session.merge(userAcc);
			hibernateException.printStackTrace();
		}
		return userAcc;
	}
}
