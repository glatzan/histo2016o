package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.util.StreamUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 6964886166116853913L;

	private static Logger logger = Logger.getLogger("histo");

	public Organization getOrganizationByName(String name) {
		DetachedCriteria query = DetachedCriteria.forClass(Organization.class, "organization");

		query.add(Restrictions.like("name", name));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return ((List<Organization>) query.getExecutableCriteria(getSession()).list()).stream()
				.collect(StreamUtils.singletonCollector());
	}
	public List<Organization> getOrganizations() {
		return getOrganizations(true);
	}

	public List<Organization> getOrganizations(boolean orderById) {
		DetachedCriteria query = DetachedCriteria.forClass(Organization.class, "organization");
		if(orderById)
			query.addOrder(Order.asc("id"));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return (List<Organization>) query.getExecutableCriteria(getSession()).list();
	}

	public Organization createOrganization(String name, Contact contact) {
		Organization newOrganization = new Organization(contact);
		newOrganization.setName(name);

		save(newOrganization, "log.organization.created", new Object[] { name });

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

		initializeOrganization(organization);

		if (organization.getContact() == null)
			organization.setPersons((new ArrayList<Person>()));

		if (!organization.getPersons().stream().anyMatch(p -> p.equals(person))) {
			organization.getPersons().add(person);

			save(organization);
			logger.debug("Added Person to Organization");
		}
	}

	public void removeOrganization(Person person, Organization organization) {
		if (person.getOrganizsations().remove(organization)) {
			logger.debug("Removing Organization from Patient");
			save(person, "log.organization.remove", new Object[] { person.getFullName(), organization.getName() });
		}

		initializeOrganization(organization);
		
		if (organization.getPersons().remove(person)) {
			save(organization);
		}
	}

	public void initializeOrganization(Organization organization) {
		refresh(organization);
		Hibernate.initialize(organization.getPersons());
	}
}
