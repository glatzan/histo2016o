package org.histo.action.dialog.print;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.impl.PatientDaoImpl;
import org.histo.ui.selectors.ContactSelector;
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

	private ContactSelector contactContainer;

	private String customAddress;

	private boolean addressChanged;

	public void initAndPrepareBean(Task task, ContactSelector contactContainer) {
		initBean(task, contactContainer);
		prepareDialog();
	}

	public void initBean(Task task, ContactSelector contactContainer) {
		this.contactContainer = contactContainer;

		customAddress = contactContainer.getCustomAddress();

		setAddressChanged(false);

		super.initBean(task, Dialog.PRINT_ADDRESS, false);
	}

	public void copyCustomAddress() {
		if (!getCustomAddress().equals(getContactContainer().getCustomAddress()))
			setAddressChanged(true);

		getContactContainer().setCustomAddress(getCustomAddress());
	}

}
