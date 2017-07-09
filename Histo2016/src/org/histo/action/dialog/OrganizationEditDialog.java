package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.OrganizationDAO;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class OrganizationEditDialog extends AbstractDialog {

	@Autowired
	private OrganizationDAO organizationDAO;

	@Getter
	@Setter
	private Organization organization;

	@Getter
	@Setter
	private boolean newOrganization;

	public void initAndPrepareBean() {
		initAndPrepareBean(null);
	}

	public void initAndPrepareBean(Organization organization) {
		initBean(organization);
		prepareDialog();
	}

	public void initBean(Organization organization) {
		if (organization != null) {
			try {
				organizationDAO.refresh(organization);
				setOrganization(organization);
				setNewOrganization(false);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		} else {
			setOrganization(new Organization(new Contact()));
			setNewOrganization(true);
		}

		super.initBean(null, Dialog.ORGANIZATION_EDIT);
	}

	public void saveOrUpdate() {
		try {
			organizationDAO.save(getOrganization(),
					getOrganization().getId() == 0 ? "log.organization.save" : "log.organization.created",
					new Object[] { getOrganization().getName() });
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}