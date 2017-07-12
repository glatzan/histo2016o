package org.histo.action;

import org.histo.action.dialog.OrganizationListDialog;
import org.histo.action.dialog.WorklistSearchDialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class DialogHandlerAction {

	private OrganizationListDialog organizationListDialog;

	private WorklistSearchDialog worklistSearchDialog;

	public OrganizationListDialog getOrganizationListDialog() {
		if (organizationListDialog == null)
			organizationListDialog = new OrganizationListDialog();

		return organizationListDialog;
	}

	public WorklistSearchDialog getWorklistSearchDialog() {
		if (worklistSearchDialog == null)
			worklistSearchDialog = new WorklistSearchDialog();

		return worklistSearchDialog;
	}
}
