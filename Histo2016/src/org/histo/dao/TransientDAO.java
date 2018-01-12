package org.histo.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Physician;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.util.CopySettingsUtil;
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

	public void synchronizeOrganizations(List<Organization> organizations) {
		// saving new organizations
		for (int i = 0; i < organizations.size(); i++) {
			Organization databaseOrganization = getOrganizationByName(organizations.get(i).getName());
			if (databaseOrganization == null) {
				logger.debug("Organization " + organizations.get(i).getName() + " not found, creating!");
				createOrganization(organizations.get(i));
			} else {
				logger.debug("Organization " + organizations.get(i).getName() + " found, replacing in linst!");
				organizations.remove(i);
				organizations.add(i, databaseOrganization);
			}
		}
	}

	public Organization createOrganization(String name, Contact contact) {
		Organization newOrganization = new Organization(contact);
		newOrganization.setName(name);

		return createOrganization(newOrganization);
	}

	public Organization createOrganization(Organization newOrganization) {

		save(newOrganization, "log.organization.created", new Object[] { newOrganization.getName() });

		return newOrganization;
	}

	public HistoGroup getHistoGroup(long id, boolean initialize) {

		HistoGroup group = get(HistoGroup.class, id);

		if (initialize) {
			Hibernate.initialize(group.getSettings());
		}

		return group;
	}
	
	/**
	 * Merges two physicians an updates their organizations (
	 * 
	 * @param source
	 * @param destination
	 */
	public void mergePhysicians(Physician source, Physician destination) {
		CopySettingsUtil.copyPhysicianData(source, destination, false);
		synchronizeOrganizations(destination.getPerson().getOrganizsations());
		save(destination, "user.role.settings.update", new Object[] { destination.toString() });
	}
}
