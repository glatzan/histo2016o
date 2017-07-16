package org.histo.action.dialog.print;

import java.util.Optional;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.AssociatedContact;
import org.histo.model.Organization;
import org.histo.model.patient.Task;
import org.histo.ui.ContactContainer;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CustomAddressDialog extends AbstractDialog {

	private ContactContainer contactContainer;

	private PatientDao patientDao;

	private String customAddress;

	public void initAndPrepareBean(Task task, ContactContainer contactContainer) {
		initBean(task, contactContainer);
		prepareDialog();
	}

	public void initBean(Task task, ContactContainer contactContainer) {
		this.contactContainer = contactContainer;
		customAddress = contactContainer.getGeneratedAddress();
		super.initBean(task, Dialog.PRINT_ADDRESS, false);
	}

	public void copyCustomAddress() {
		contactContainer.getContact().setCustomContact(getCustomAddress());
		try {
			patientDao.savePatientAssociatedDataFailSave(getContactContainer().getContact(), task,
					"log.patient.task.contact.customAddress");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
