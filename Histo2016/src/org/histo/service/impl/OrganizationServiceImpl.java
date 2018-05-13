package org.histo.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.service.OrganizationService;
import org.histo.service.dao.OrganizationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service("organizationService")
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class OrganizationServiceImpl extends AbstractService implements OrganizationService {

	@Autowired
	private OrganizationDao organizationDao;

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#addOrganization(java.lang.String, org.histo.model.Contact)
	 */
	@Override
	public Organization addOrganization(String name, Contact contact) {
		return addOrganization(new Organization(name, contact));
	}

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#addOrganization(org.histo.model.Organization)
	 */
	@Override
	public Organization addOrganization(Organization organization) {
		organizationDao.save(organization,
				organization.getId() == 0 ? "log.organization.save" : "log.organization.created",
				organization.getName());
		return organization;
	}

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#addPerson(org.histo.model.Organization, org.histo.model.Person)
	 */
	@Override
	public void addPerson(Organization organization, Person person) {
		if (person.getOrganizsations() == null)
			person.setOrganizsations(new ArrayList<Organization>());

		if (!person.getOrganizsations().stream().anyMatch(p -> p.equals(organization))) {

			person.getOrganizsations().add(organization);

			// only save if person was saved before
			if (person.getId() != 0)
				organizationDao.save(person, "log.organization.added", person.getFullName(), organization.getName());
			logger.debug("Added Organization to Person");
		}
	}

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#removePerson(org.histo.model.Organization, org.histo.model.Person)
	 */
	@Override
	public boolean removePerson(Organization organization, Person person) {
		return removePerson(organization, person, true);
	}

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#removePerson(org.histo.model.Organization, org.histo.model.Person, boolean)
	 */
	@Override
	public boolean removePerson(Organization organization, Person person, boolean save) {
		logger.debug("Removing Organization from Patient");
		// removing organization form person, for the database this will remove the
		// inverted association as well
		boolean result = person.getOrganizsations().remove(organization);
		organization.getPersons().remove(person);

		if (result && save) {
			organizationDao.save(person, "log.organization.remove", person.getFullName(), organization.getName());
		}

		return result;
	}

	/* (non-Javadoc)
	 * @see org.histo.service.OrganizationSerive#synchronizeOrganizations(java.util.List)
	 */
	@Override
	public void synchronizeOrganizations(List<Organization> organizations) {
		if (organizations == null)
			return;

		// saving new organizations
		for (int i = 0; i < organizations.size(); i++) {

			// do not reload loaded organizations
			if (organizations.get(i).getId() == 0) {
				Organization databaseOrganization = organizationDao.find(organizations.get(i).getName());
				if (databaseOrganization == null) {
					logger.debug("Organization " + organizations.get(i).getName() + " not found, creating!");
					addOrganization(organizations.get(i));
				} else {
					logger.debug("Organization " + organizations.get(i).getName() + " found, replacing in linst!");
					organizations.remove(i);
					organizations.add(i, databaseOrganization);
				}
			}
		}
	}
}
