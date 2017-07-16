package org.histo.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactContainer {

	private AssociatedContact contact;

	private int copies;

	private boolean selected;

	private List<OrganizationChooser> organizazionsChoosers;

	public ContactContainer(Task task, Person person, ContactRole role) {
		this(new AssociatedContact(task, person, role));
	}

	public ContactContainer(AssociatedContact associatedContact) {
		this.contact = associatedContact;
		this.copies = 1;
		this.selected = false;
		this.organizazionsChoosers = new ArrayList<OrganizationChooser>();

		if (associatedContact.getPerson().getOrganizsations() != null)
			for (Organization organization : associatedContact.getPerson().getOrganizsations()) {
				this.organizazionsChoosers.add(new OrganizationChooser(this, organization));
			}
	}

	public String getGeneratedAddress() {
		Optional<String> address = Optional.ofNullable(getContact().getCustomContact());

		if (address.isPresent())
			return address.get();

		return generateAddress(this);
	}

	public static String generateAddress(ContactContainer contactContainer) {
		try {
			ContactContainer.OrganizationChooser organizationChooser = contactContainer.getOrganizazionsChoosers().stream()
					.filter(p -> p.isSelected()).collect(StreamUtils.singletonCollector());
			return generateAddress(contactContainer.getContact(), organizationChooser.getOrganization());
		} catch (IllegalStateException e) {
			return generateAddress(contactContainer.getContact(), null);
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
	
	@Getter
	@Setter
	public class OrganizationChooser {

		private ContactContainer parent;
		private Organization organization;
		private boolean selected;

		public OrganizationChooser(ContactContainer parent, Organization organization) {
			this.parent = parent;
			this.organization = organization;
		}
	}

	public static List<ContactContainer> factory(List<AssociatedContact> associatedContacts) {
		ArrayList<ContactContainer> result = new ArrayList<ContactContainer>();
		for (AssociatedContact associatedContact : associatedContacts) {
			result.add(new ContactContainer(associatedContact));
		}
		return result;
	}
}
