package org.histo.action.dialog.print;

import java.util.Date;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.PatientDao;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.ui.ContactContainer;
import org.histo.util.HistoUtil;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	private PDFContainer pdf;

	private AssociatedContact contact;

	private String number;

	private boolean faxButtonDisabled;

	public void initAndPrepareBean(Task task, AssociatedContact contact, PDFContainer pdf) {
		initBean(task, contact, pdf);
		prepareDialog();
	}

	public void initBean(Task task, AssociatedContact contact, PDFContainer pdf) {
		this.contact = contact;
		this.pdf = pdf;

		System.out.println("gallo");
		
		if (contact != null)
			this.number = contact.getPerson().getContact().getFax();
		else
			this.number = null;
		
		updateFaxButton();

		super.initBean(task, Dialog.PRINT_FAX, false);
	}

	public void updateFaxButton() {
		if (HistoUtil.isNotNullOrEmpty(getNumber()))
			setFaxButtonDisabled(false);
		else
			setFaxButtonDisabled(true);
	}

	public void sendFax() {

		try {

			if (getContact() != null) {
				// adding new contact method fax
				AssociatedContactNotification associatedContactNotification = contactDAO.addNotificationType(task,
						getContact(), NotificationTyp.FAX);

				associatedContactNotification.setPerformed(true);
				associatedContactNotification.setDateOfAction(new Date(System.currentTimeMillis()));
				associatedContactNotification.setContactAddress(getNumber());

				genericDAO.savePatientData(associatedContactNotification, getTask(),
						"log.patient.task.contact.notification.performed", associatedContactNotification.toString(),
						getContact().toString());

				logger.debug("saving notification status");

			}

			settingsHandler.getFaxHandler().sendFax(number, pdf);

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}

	}
}
