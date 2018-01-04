package org.histo.action.dialog.settings.organizations;

import java.util.Arrays;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.OrganizationDAO;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
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

	private Organization organization;

	private boolean newOrganization;

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
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				return false;
			}
		}

		setOrganization(organization);

		setNewOrganization(organization.getId() == 0);

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
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void removePersonFromOrganization(Person person, Organization organization) {
		try {
			logger.debug("Removing Person from Organization");
			organizationDAO.removeOrganization(person, organization);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
