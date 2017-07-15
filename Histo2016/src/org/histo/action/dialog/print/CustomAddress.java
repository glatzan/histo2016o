package org.histo.action.dialog.print;

import java.util.Optional;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
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

	private String customAddress;

	public void initAndPrepareBean(ContactChooser contactChooser) {
		initBean(contactChooser);
		prepareDialog();
	}

	public void initBean(ContactChooser contactChooser) {
		this.contactChooser = contactChooser;
		customAddress = generateAddress(contactChooser);
		super.initBean(null, Dialog.PRINT_ADDRESS, false);
	}

	public void copyCustomAddress() {
		contactChooser.getContact().setCustomContact(getCustomAddress());
	}

	public static String generateAddress(ContactChooser contactChooser) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(contactChooser.getContact().getPerson().getFullName() + "\r\n");

		try {
			ContactChooser.OrganizationChooser organizationChooser = contactChooser.getOrganizazionsChoosers().stream()
					.filter(p -> p.isSelected()).collect(StreamUtils.singletonCollector());

			buffer.append(organizationChooser.getOrganization().getName() + "\r\n");
			buffer.append(Optional.ofNullable(organizationChooser.getOrganization().getContact().getStreet()).orElse("")
					+ "\r\n");
			buffer.append(
					Optional.ofNullable(organizationChooser.getOrganization().getContact().getPostcode()).orElse("")
							+ " " + Optional.ofNullable(organizationChooser.getOrganization().getContact().getTown())
									.orElse("")
							+ "\r\n");
		} catch (IllegalStateException e) {
			// no organization is selected or present, so add the data of the
			// user
			buffer.append(contactChooser.getContact().getPerson().getContact().getStreet() + "\r\n");
			buffer.append(contactChooser.getContact().getPerson().getContact().getPostcode() + " "
					+ contactChooser.getContact().getPerson().getContact().getTown() + "\r\n");
		}

		return buffer.toString();
	}
}
