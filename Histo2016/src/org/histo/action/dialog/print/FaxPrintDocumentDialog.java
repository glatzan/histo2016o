package org.histo.action.dialog.print;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.Dialog;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.ui.ContactContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class FaxPrintDocumentDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	private PDFContainer pdf;

	private AssociatedContact contact;

	private String number;

	public void initAndPrepareBean(Task task, AssociatedContact contact, PDFContainer pdf) {
		initBean(task, contact, pdf);
		prepareDialog();
	}

	public void initBean(Task task, AssociatedContact contact, PDFContainer pdf) {
		this.contact = contact;
		this.pdf = pdf;

		if (contact != null)
			this.number = contact.getPerson().getContact().getFax();

		super.initBean(task, Dialog.PRINT_FAX, false);
	}

	public void sendFax() {
		settingsHandler.getFaxHandler().sendFax(number, pdf);
	}
}
