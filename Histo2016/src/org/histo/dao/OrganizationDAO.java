package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class OrganizationDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 6964886166116853913L;

	private static Logger logger = Logger.getLogger("histo");

	/**
	 * Takes a list of organizations, all organizations which are not in the
	 * database will be created
	 * 
	 * @param organizations
	 */
	public void synchronizeOrganizations(List<Organization> organizations) {
		if (organizations == null)
			return;

		// saving new organizations
		for (int i = 0; i < organizations.size(); i++) {

			// do not reload loaded organizations
			if (organizations.get(i).getId() == 0) {
				Organization databaseOrganization = getOrganizationByName(organizations.get(i).getName());
				if (databaseOrganization == null) {
					logger.debug("Organization " + organizations.get(i).getName() + " not found, creating!");
					saveOrUpdateOrganization(organizations.get(i));
				} else {
					logger.debug("Organization " + organizations.get(i).getName() + " found, replacing in linst!");
					organizations.remove(i);
					organizations.add(i, databaseOrganization);
				}
			}
		}
	}

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

	/**
	 * Returns a list of organizations, ordered by id asc, not initialized (person),
	 * archived user depending.
	 * 
	 * @param archived
	 * @return
	 */
	public List<Organization> getOrganizations(boolean archived) {
		return getOrganizations(true, false, archived);
	}

	/**
	 * Returns a list of organizations.
	 * 
	 * @param orderById
	 * @param initialize
	 * @param archived
	 * @return
	 */
	public List<Organization> getOrganizations(boolean orderById, boolean initialize, boolean archived) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Organization> criteria = qb.createQuery(Organization.class);
		Root<Organization> root = criteria.from(Organization.class);
		criteria.select(root);

		if (orderById)
			criteria.orderBy(qb.asc(root.get("id")));

		if (initialize)
			root.fetch("person", JoinType.LEFT);

		if (!archived)
			criteria.where(qb.equal(root.get("archived"), false));

		criteria.distinct(true);

		return getSession().createQuery(criteria).getResultList();
	}

	public Organization createOrganization(String name, Contact contact) {
		Organization newOrganization = new Organization(contact);
		newOrganization.setName(name);

		return saveOrUpdateOrganization(newOrganization);
	}

	/**
	 * Creates a new organization or updates an existing one
	 * 
	 * @param organization
	 * @return
	 */
	public Organization saveOrUpdateOrganization(Organization organization) {
		save(organization, organization.getId() == 0 ? "log.organization.save" : "log.organization.created",
				new Object[] { organization.getName() });

		return organization;
	}

	public void addOrganization(Person person, Organization organization) {
		if (person.getOrganizsations() == null)
			person.setOrganizsations(new ArrayList<Organization>());

		if (!person.getOrganizsations().stream().anyMatch(p -> p.equals(organization))) {

			person.getOrganizsations().add(organization);

			// only save if person was saved before
			if (person.getId() != 0)
				save(person, "log.organization.added", new Object[] { person.getFullName(), organization.getName() });
			logger.debug("Added Organization to Person");
		}
	}

	public void removeOrganization(Person person, Organization organization) {
		if (person.getOrganizsations().remove(organization)) {
			organization.getPersons().remove(person);
			logger.debug("Removing Organization from Patient");
			save(person, "log.organization.remove", new Object[] { person.getFullName(), organization.getName() });
		}
	}

	public void initializeOrganization(Organization organization) {
		reattach(organization);
		Hibernate.initialize(organization.getPersons());
	}
}
