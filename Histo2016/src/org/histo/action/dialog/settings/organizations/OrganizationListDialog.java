package org.histo.action.dialog.settings.organizations;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.service.dao.OrganizationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class OrganizationListDialog extends AbstractDialog {

	@Autowired
	private OrganizationDao organizationDao;

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
		initAndPrepareBean(false);
	}

	public void initAndPrepareBean(boolean selectMode) {
		initBean(selectMode);
		prepareDialog();
	}

	public void initBean(boolean selectMode) {
		super.initBean(null, Dialog.SETTINGS_ORGANIZATION_LIST);

		this.selectMode = selectMode;

		updateOrganizationList();
	}

	public void updateOrganizationList() {
		setSelectedOrganization(null);
		setOrganizations(organizationDao.list(true));
	}

	public void selectOrganisation() {
		super.hideDialog(selectedOrganization);
	}
	
	public void removeOrganization(Person person, Organization organization) {
		person.getOrganizsations().remove(organization);
	}
}