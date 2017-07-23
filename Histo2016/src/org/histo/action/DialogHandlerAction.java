package org.histo.action;

import org.histo.action.dialog.OrganizationListDialog;
import org.histo.action.dialog.WorklistSearchDialog;
import org.histo.action.dialog.notification.ContactDialog;
import org.histo.action.dialog.notification.ContactNotificationDialog;
import org.histo.action.dialog.notification.ContactSelectDialog;
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

	private ContactSelectDialog contactSelectDialog;

	private ContactDialog contactDialog;

	private ContactNotificationDialog contactNotificationDialog;

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

	public CustomAddressDialog getCustomAddressDialog() {
		if (customAddressDialog == null)
			customAddressDialog = new CustomAddressDialog();

		return customAddressDialog;
	}

	public ContactSelectDialog getContactSelectDialog() {
		if (contactSelectDialog == null)
			contactSelectDialog = new ContactSelectDialog();

		return contactSelectDialog;
	}

	public ContactDialog getContactDialog() {
		if (contactDialog == null)
			contactDialog = new ContactDialog();

		return contactDialog;
	}

	public ContactNotificationDialog getContactNotificationDialog() {
		if (contactNotificationDialog == null)
			contactNotificationDialog = new ContactNotificationDialog();

		return contactNotificationDialog;
	}
}
