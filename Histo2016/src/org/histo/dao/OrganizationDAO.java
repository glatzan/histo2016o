package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
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

	public List<Organization> getOrganizations() {
		return getOrganizations(true, false);
	}

	public List<Organization> getOrganizations(boolean orderById, boolean initialized) {
		DetachedCriteria query = DetachedCriteria.forClass(Organization.class, "organization");
		if (orderById)
			query.addOrder(Order.asc("id"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Organization> result = (List<Organization>) query.getExecutableCriteria(getSession()).list();

		if (initialized && result != null)
			result.stream().forEach(p -> initializeOrganization(p));

		return result;
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

	public void addOrganization(Person person, Organization organization) {
		if (person.getOrganizsations() == null)
			person.setOrganizsations(new ArrayList<Organization>());

		if (!person.getOrganizsations().stream().anyMatch(p -> p.equals(organization))) {

			person.getOrganizsations().add(organization);

			save(person, "log.organization.added", new Object[] { person.getFullName(), organization.getName() });
			logger.debug("Added Organization to Person");
		}
	}

	public void removeOrganization(Person person, Organization organization) {
		if (person.getOrganizsations().remove(organization)) {
			logger.debug("Removing Organization from Patient");
			save(person, "log.organization.remove", new Object[] { person.getFullName(), organization.getName() });
		}
	}

	public void initializeOrganization(Organization organization) {
		reattach(organization);
		Hibernate.initialize(organization.getPersons());
	}
}
