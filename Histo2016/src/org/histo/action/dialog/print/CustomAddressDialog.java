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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CustomAddressDialog extends AbstractDialog {

	@Autowired
	private PatientDao patientDao;

	private ContactContainer contactContainer;

	private String customAddress;

	private boolean addressChanged;
	
	public void initAndPrepareBean(Task task, ContactContainer contactContainer) {
		initBean(task, contactContainer);
		prepareDialog();
	}

	public void initBean(Task task, ContactContainer contactContainer) {
		this.contactContainer = contactContainer;
		customAddress = contactContainer.getContact().getContactAsString();
		
		setAddressChanged(false);
		
		super.initBean(task, Dialog.PRINT_ADDRESS, false);
	}

	public void copyCustomAddress() {
		if(!getCustomAddress().equals(getContactContainer().getContact().getCustomContact()))
			setAddressChanged(true);
		
		getContactContainer().getContact().setCustomContact(getCustomAddress());
	}

}
