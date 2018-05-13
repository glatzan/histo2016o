package org.histo.service.dao.impl;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.histo.model.Organization;
import org.histo.service.dao.OrganizationDao;
import org.springframework.stereotype.Repository;

@Repository("organizationDao")
public class OrganizationDaoImpl extends HibernateDao<Organization, Long> implements OrganizationDao {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.histo.service.dao.OrganizationDao#initialize(org.histo.model.Organization
	 * )
	 */
	@Override
	public void initialize(Organization organization) {
		initialize(organization, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.histo.service.dao.OrganizationDao#initialize(org.histo.model.Organization
	 * ,boolean)
	 */
	@Override
	public void initialize(Organization organization, boolean loadPersons) {
		reattach(organization);
		if (loadPersons)
			Hibernate.initialize(organization.getPersons());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.OrganizationDao#find(java.lang.String)
	 */
	@Override
	public Organization find(String name) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<Organization> criteria = qb.createQuery(Organization.class);
		Root<Organization> root = criteria.from(Organization.class);

		criteria.select(root);
		criteria.where(qb.like(root.get("name"), name));
		criteria.distinct(true);

		List<Organization> organizations = currentSession().createQuery(criteria).getResultList();

		return !organizations.isEmpty() ? organizations.get(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.OrganizationDao#list(boolean)
	 */
	@Override
	public List<Organization> list(boolean irgnoreArchived) {
		return list(true, false, irgnoreArchived);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.OrganizationDao#list(boolean, boolean, boolean)
	 */
	@Override
	public List<Organization> list(boolean orderById, boolean loadPersons, boolean irgnoreArchived) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<Organization> criteria = qb.createQuery(Organization.class);
		Root<Organization> root = criteria.from(Organization.class);

		criteria.select(root);

		if (orderById)
			criteria.orderBy(qb.asc(root.get("id")));

		if (loadPersons)
			root.fetch("person", JoinType.LEFT);

		if (irgnoreArchived)
			criteria.where(qb.equal(root.get("archived"), false));

		criteria.distinct(true);

		return currentSession().createQuery(criteria).getResultList();
	}
}
