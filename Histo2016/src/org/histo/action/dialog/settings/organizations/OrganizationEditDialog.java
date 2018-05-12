package org.histo.action.dialog.settings.organizations;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.dao.OrganizationDAO;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class OrganizationEditDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private OrganizationDAO organizationDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	private Organization organization;

	private boolean newOrganization;

	/**
	 * Stores persons to remove from organization when saving
	 */
	private List<Person> removeFromOrganization;

	public void initAndPrepareBean() {
		if (initBean(new Organization(new Contact())))
			prepareDialog();
	}

	public void initAndPrepareBean(Organization organization) {
		if (initBean(organization))
			prepareDialog();
	}

	public boolean initBean(Organization organization) {

		if (organization.getId() != 0) {
			try {
				organizationDAO.initializeOrganization(organization);
			} catch (HistoDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				return false;
			}
		}

		setOrganization(organization);

		setNewOrganization(organization.getId() == 0);

		setRemoveFromOrganization(new ArrayList<Person>());

		super.initBean(task, Dialog.SETTINGS_ORGANIZATION_EDIT);

		return true;
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void save() {
		try {
			organizationDAO.saveOrUpdateOrganization(organization);

			for (Person person : removeFromOrganization) {
				genericDAO.save(person, "log.person.organization.remove", new Object[] { person, organization });
			}

		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Removes a organization temporary from the person and adds it to an array in
	 * order to save the change when the user clicks on the save button.
	 * 
	 * @param person
	 * @param organization
	 */
	public void removePersonFromOrganization(Person person, Organization organization) {
		organizationDAO.removeOrganization(person, organization, false);
		getRemoveFromOrganization().add(person);
	}
}
