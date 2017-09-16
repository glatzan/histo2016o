package org.histo.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.histo.model.HistoUser;
import org.histo.model.Organization;
import org.histo.model.Physician;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class TransientDAO extends AbstractDAO {

	private static final long serialVersionUID = -4244598921496670778L;

	/**
	 * Methode is tranactional, is used out of session (loggin in)
	 * 
	 * @param name
	 * @return
	 */
	public Organization getOrganizationByName(String name) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Organization> criteria = qb.createQuery(Organization.class);
		Root<Organization> root = criteria.from(Organization.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("name"), name));
		criteria.distinct(true);

		List<Organization> organizations = getSession().createQuery(criteria).getResultList();

		if (!organizations.isEmpty())
			return organizations.get(0);

		return null;
	}

	public HistoUser loadUserByName(String name) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("username"), name));
		criteria.distinct(true);

		List<HistoUser> users = getSession().createQuery(criteria).getResultList();

		if (!users.isEmpty()) {
			return users.get(0);
		}

		return null;
	}

	public Physician loadPhysicianByUID(String uid) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Physician> criteria = qb.createQuery(Physician.class);
		Root<Physician> root = criteria.from(Physician.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("uid"), uid));
		criteria.distinct(true);

		List<Physician> physician = getSession().createQuery(criteria).getResultList();

		if (!physician.isEmpty()) {
			return physician.get(0);
		}

		return null;
	}
}
