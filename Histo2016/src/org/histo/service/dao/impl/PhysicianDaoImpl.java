package org.histo.service.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.histo.config.enums.ContactRole;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.service.dao.PhysicianDao;
import org.springframework.stereotype.Repository;

@Repository("physicianDao")
public class PhysicianDaoImpl extends HibernateDao<Physician, Long> implements PhysicianDao {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#findList(org.histo.config.enums.
	 * ContactRole, boolean)
	 */
	@Override
	public List<Physician> list(ContactRole role, boolean irgnoreArchived) {
		return list(Arrays.asList(role), irgnoreArchived, PhysicianSortOrder.NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#findList(java.util.List, boolean)
	 */
	@Override
	public List<Physician> list(List<ContactRole> roles, boolean irgnoreArchived) {
		return list(roles, irgnoreArchived, PhysicianSortOrder.NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#findList(org.histo.config.enums.
	 * ContactRole[], boolean)
	 */
	@Override
	public List<Physician> list(ContactRole[] roles, boolean irgnoreArchived) {
		return list(roles, irgnoreArchived, PhysicianSortOrder.NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#findList(org.histo.config.enums.
	 * ContactRole[], boolean,
	 * org.histo.service.dao.PhysicianDaoImpl.PhysicianSortOrder)
	 */
	@Override
	public List<Physician> list(ContactRole[] roles, boolean irgnoreArchived, PhysicianSortOrder sortOrder) {
		return list(Arrays.asList(roles), irgnoreArchived, sortOrder);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#findList(java.util.List, boolean,
	 * org.histo.service.dao.PhysicianDaoImpl.PhysicianSortOrder)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Physician> list(List<ContactRole> roles, boolean irgnoreArchived, PhysicianSortOrder sortOrder) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<Physician> criteria = qb.createQuery(Physician.class);
		Root<Physician> root = criteria.from(Physician.class);

		criteria.select(root);

		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(root.join("associatedRoles", JoinType.LEFT).in(roles));

		if (irgnoreArchived)
			predicates.add(qb.equal(root.get("archived"), false));

		criteria.where(qb.and(predicates.toArray(new Predicate[predicates.size()])));

		Path<Person> fetchAsPath = (Path<Person>) root.fetch("person", JoinType.LEFT);

		if (sortOrder == PhysicianSortOrder.NAME) {
			criteria.orderBy(qb.asc(fetchAsPath.get("lastName")));
		} else if (sortOrder == PhysicianSortOrder.PRIORITY)
			criteria.orderBy(qb.desc(root.get("priorityCount")), qb.asc(fetchAsPath.get("lastName")));
		else
			criteria.orderBy(qb.asc(root.get("id")));

		criteria.distinct(true);

		return currentSession().createQuery(criteria).getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#find(java.lang.String)
	 */
	@Override
	public Physician find(String uid) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<Physician> criteria = qb.createQuery(Physician.class);
		Root<Physician> root = criteria.from(Physician.class);

		criteria.select(root);

		criteria.where(qb.like(root.get("uid"), uid));
		criteria.distinct(true);

		List<Physician> physician = currentSession().createQuery(criteria).getResultList();

		return !physician.isEmpty() ? physician.get(0) : null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.histo.service.dao.PhysicianDao#getPhysicianByPerson(org.histo.model.
	 * Person)
	 */
	@Override
	public Physician find(Person person) {
		CriteriaBuilder qb = currentSession().getCriteriaBuilder();
		CriteriaQuery<Physician> criteria = qb.createQuery(Physician.class);
		Root<Physician> root = criteria.from(Physician.class);

		criteria.select(root);
		criteria.where(qb.equal(root.get("person"), person));
		criteria.distinct(true);

		List<Physician> persons = currentSession().createQuery(criteria).getResultList();

		return !persons.isEmpty() ? persons.get(0) : null;
	}
}
