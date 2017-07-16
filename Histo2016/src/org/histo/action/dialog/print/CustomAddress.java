package org.histo.action.dialog.print;

import java.util.Optional;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.model.AssociatedContact;
import org.histo.model.Organization;
import org.histo.model.patient.Task;
import org.histo.ui.ContactChooser;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CustomAddress extends AbstractDialog {

	private ContactChooser contactChooser;

	private PatientDao patientDao;

	private String customAddress;

	public void initAndPrepareBean(Task task, ContactChooser contactChooser) {
		initBean(task, contactChooser);
		prepareDialog();
	}

	public void initBean(Task task, ContactChooser contactChooser) {
		this.contactChooser = contactChooser;
		customAddress = getGeneratedAddress(contactChooser);
		super.initBean(task, Dialog.PRINT_ADDRESS, false);
	}

	public void copyCustomAddress() {
		contactChooser.getContact().setCustomContact(getCustomAddress());
		try {
			patientDao.savePatientAssociatedDataFailSave(getContactChooser().getContact(), task,
					"log.patient.task.contact.customAddress");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public String getGeneratedAddress(ContactChooser contactChooser) {
		Optional<String> address = Optional.ofNullable(contactChooser.getContact().getCustomContact());
		System.out.println("hallo");

		if (address.isPresent())
			return address.get();

		return generateAddress(contactChooser);
	}

	public static String generateAddress(ContactChooser contactChooser) {
		try {
			ContactChooser.OrganizationChooser organizationChooser = contactChooser.getOrganizazionsChoosers().stream()
					.filter(p -> p.isSelected()).collect(StreamUtils.singletonCollector());
			return generateAddress(contactChooser.getContact(), organizationChooser.getOrganization());
		} catch (IllegalStateException e) {
			return generateAddress(contactChooser.getContact(), null);
		}

	}

	public static String generateAddress(AssociatedContact associatedContact, Organization selectedOrganization) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(associatedContact.getPerson().getFullName() + "\r\n");

		Optional<String> street;
		Optional<String> postcode;
		Optional<String> town;

		if (selectedOrganization != null) {
			street = Optional.ofNullable(selectedOrganization.getContact().getStreet()).filter(s -> !s.isEmpty());
			postcode = Optional.ofNullable(selectedOrganization.getContact().getPostcode()).filter(s -> !s.isEmpty());
			town = Optional.ofNullable(selectedOrganization.getContact().getTown()).filter(s -> !s.isEmpty());
			buffer.append(selectedOrganization.getName() + "\r\n");

		} else {
			// no organization is selected or present, so add the data of the
			// user
			street = Optional.ofNullable(associatedContact.getPerson().getContact().getStreet())
					.filter(s -> !s.isEmpty());
			postcode = Optional.ofNullable(associatedContact.getPerson().getContact().getPostcode())
					.filter(s -> !s.isEmpty());
			town = Optional.ofNullable(associatedContact.getPerson().getContact().getTown()).filter(s -> !s.isEmpty());
		}

		buffer.append(street.isPresent() ? street.get() + "\r\n" : "");
		buffer.append(postcode.isPresent() ? postcode.get() + " " : "");
		buffer.append(town.isPresent() ? town.get() + "\r\n" : "");

		return buffer.toString();
	}
}
