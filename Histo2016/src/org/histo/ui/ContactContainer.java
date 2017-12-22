package org.histo.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
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

	private static Logger logger = Logger.getLogger("org.histo");

	private AssociatedContact contact;

	private int copies;

	private boolean selected;

	private boolean organizationHasChagned;

	private List<OrganizationChooser> organizazionsChoosers;

	private String customAddress;
	
	private boolean emptyAddress;

	public ContactContainer(Task task, Person person, ContactRole role) {
		this(new AssociatedContact(task, person, role), false, false);
	}

	public ContactContainer(Task task, Person person, ContactRole role, boolean selected, boolean emptyAddress) {
		this(new AssociatedContact(task, person, role), selected, emptyAddress);
	}

	public ContactContainer(AssociatedContact associatedContact) {
		this(associatedContact, false, false);
	}

	public ContactContainer(AssociatedContact associatedContact, boolean selected, boolean emptyAddress) {
		this.contact = associatedContact;
		this.copies = 1;
		this.selected = selected;
		this.organizazionsChoosers = new ArrayList<OrganizationChooser>();

		if (associatedContact.getPerson().getOrganizsations() != null)
			for (Organization organization : associatedContact.getPerson().getOrganizsations()) {
				this.organizazionsChoosers.add(new OrganizationChooser(this, organization));
			}

		this.emptyAddress = emptyAddress;
		
		if (emptyAddress) {
			setCustomAddress(" ");
		} else
			generateAddress();
	}

	public OrganizationChooser getSelectedOrganization() {
		try {
			return getOrganizazionsChoosers().stream().filter(p -> p.isSelected())
					.collect(StreamUtils.singletonCollector());
		} catch (IllegalStateException e) {
			return null;
		}
	}

	public void generateAddress() {
		generateAddress(false);
	}

	public void generateAddress(boolean overwrite) {
		Optional<String> address = Optional.ofNullable(getCustomAddress());

		if (address.isPresent() && !overwrite)
			return;

		Optional<Organization> selectedOrganization = Optional.ofNullable(getSelectedOrganization())
				.map(OrganizationChooser::getOrganization);

		setCustomAddress(AssociatedContact.generateAddress(getContact(), selectedOrganization.orElse(null)));

		logger.debug("Custom Address is: " + getCustomAddress());
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
			result.add(new ContactContainer(associatedContact, false, false));
		}
		return result;
	}
}
