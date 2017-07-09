package org.histo.action.dialog;

import java.util.List;

import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.OrganizationDAO;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class OrganizationListDialog extends AbstractDialog {

	@Autowired
	private OrganizationDAO organizationDAO;

	@Getter
	@Setter
	private List<Organization> organizations;

	@Getter
	@Setter
	private Person person;

	@Getter
	@Setter
	private boolean selectMode;

	@Getter
	@Setter
	private Organization selectedOrganization;

	public void initAndPrepareBean() {
		initAndPrepareBean(null);
	}

	public void initAndPrepareBean(Person person) {
		initBean(person);
		prepareDialog();
	}

	public void initBean(Person person) {
		super.initBean(null, Dialog.ORGANIZATION_LIST);
		if (person == null)
			this.selectMode = false;
		else {
			try {
				organizationDAO.refresh(person);
				setPerson(person);
				this.selectMode = true;
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}
		updateOrganizationList();
	}

	public void updateOrganizationList() {
		setSelectedOrganization(null);
		setOrganizations(organizationDAO.getOrganizations());
	}

	public void selectOrganisation() {
		try {
			if (getSelectedOrganization() != null) {
				organizationDAO.addOrganization(getPerson(), getSelectedOrganization());
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}