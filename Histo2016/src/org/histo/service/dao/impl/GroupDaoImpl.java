package org.histo.service.dao.impl;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.histo.model.user.HistoGroup;
import org.histo.service.dao.GroupDao;
import org.springframework.stereotype.Repository;

@Repository("groupDao")
public class GroupDaoImpl extends HibernateDao<HistoGroup, Long> implements GroupDao {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.GroupDao#initializeGroup(org.histo.model.user.
	 * HistoGroup)
	 */
	@Override
	public HistoGroup initialize(HistoGroup group) {
		return initialize(group, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.GroupDao#initializeGroup(org.histo.model.user.
	 * HistoGroup, boolean)
	 */
	@Override
	public HistoGroup initialize(HistoGroup group, boolean loadSettings) {
		reattach(group);

		if (loadSettings)
			Hibernate.initialize(group.getSettings());

		return group;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.GroupDao#find(boolean)
	 */
	@Override
	public List<HistoGroup> list(boolean irgnoreArchived) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<HistoGroup> criteria = qb.createQuery(HistoGroup.class);
		Root<HistoGroup> root = criteria.from(HistoGroup.class);

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
	 * @see org.histo.service.dao.GroupDao#find(long)
	 */
	@Override
	public HistoGroup find(long id) {
		return find(id, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.GroupDao#find(long, boolean)
	 */
	@Override
	public HistoGroup find(long id, boolean loadSettings) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<HistoGroup> criteria = qb.createQuery(HistoGroup.class);
		Root<HistoGroup> root = criteria.from(HistoGroup.class);

		criteria.select(root);
		criteria.distinct(true);
		criteria.where(getBuilder().equal(root.get("id"), id));

		if (loadSettings)
			root.fetch("settings", JoinType.LEFT);

		List<HistoGroup> groups = currentSession().createQuery(criteria).getResultList();

		return !groups.isEmpty() ? groups.get(0) : null;
	}

}
