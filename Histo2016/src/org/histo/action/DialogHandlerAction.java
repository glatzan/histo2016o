package org.histo.action;

import org.histo.action.dialog.OrganizationEditDialog;
import org.histo.action.dialog.OrganizationListDialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class DialogHandlerAction {

	private OrganizationListDialog organizationListDialog;
	private OrganizationEditDialog organizationEditDialog;

	public OrganizationListDialog getOrganizationListDialog() {
		if (organizationListDialog == null)
			organizationListDialog = new OrganizationListDialog();

		return organizationListDialog;
	}

	public OrganizationEditDialog getOrganizationEditDialog() {
		if (organizationEditDialog == null)
			organizationEditDialog = new OrganizationEditDialog();
		
		return organizationEditDialog;
	}

}
