package org.histo.service.dao.impl;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.histo.model.user.HistoUser;
import org.histo.service.dao.UserDao;
import org.springframework.stereotype.Repository;

@Repository("userDao")
public class UserDaoImpl extends HibernateDao<HistoUser, Long> implements UserDao {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.UserDao#list(boolean)
	 */
	@Override
	public List<HistoUser> list(boolean irgnoreArchived) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);

		criteria.select(root);
		if (irgnoreArchived)
			criteria.where(qb.equal(root.get("archived"), false));
		criteria.orderBy(qb.asc(root.get("id")));
		criteria.distinct(true);

		return currentSession().createQuery(criteria).getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.UserDao#find(java.lang.String)
	 */
	@Override
	public HistoUser find(String name) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);

		criteria.select(root);
		criteria.where(qb.like(root.get("username"), name));
		criteria.distinct(true);

		List<HistoUser> users = currentSession().createQuery(criteria).getResultList();

		return !users.isEmpty() ? users.get(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.UserDao#findListByGroup(long)
	 */
	@Override
	public List<HistoUser> findListByGroup(long id) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);

		criteria.select(root);
		criteria.distinct(true);
		criteria.where(qb.equal(root.get("group"), id));

		List<HistoUser> users = currentSession().createQuery(criteria).getResultList();

		return users;
	}
}
