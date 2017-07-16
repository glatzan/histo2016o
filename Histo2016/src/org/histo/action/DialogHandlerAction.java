package org.histo.action;

import org.histo.action.dialog.OrganizationListDialog;
import org.histo.action.dialog.WorklistSearchDialog;
import org.histo.action.dialog.print.CustomAddressDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class DialogHandlerAction {

	private OrganizationListDialog organizationListDialog;

	private WorklistSearchDialog worklistSearchDialog;
	
	private PrintDialog printDialog;

	private CustomAddressDialog customAddressDialog; 
	
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
	
	public PrintDialog getPrintDialog() {
		if (printDialog == null)
			printDialog = new PrintDialog();

		return printDialog;
	}
	
	public CustomAddressDialog getCustomAddress() {
		if(customAddressDialog == null)
			customAddressDialog = new CustomAddressDialog();
		
		return customAddressDialog;
	}
}

