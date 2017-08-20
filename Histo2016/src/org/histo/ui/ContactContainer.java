package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

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

	private boolean organizationHasChagned;
	
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

	/**
	 * Checks if an organization was selected, then true will be returned an a
	 * customAddress will be generated
	 * 
	 * @param contactContainer
	 * @return
	 */
	public static boolean generateCustomOrganizationAddress(ContactContainer contactContainer) {
		try {
			// organization was selected generating customAddress field
			ContactContainer.OrganizationChooser organizationChooser = contactContainer.getOrganizazionsChoosers()
					.stream().filter(p -> p.isSelected()).collect(StreamUtils.singletonCollector());

			contactContainer.getContact().setCustomContact(AssociatedContact
					.generateAddress(contactContainer.getContact(), organizationChooser.getOrganization()));
			
			return true;
		} catch (IllegalStateException e) {
			// no organization was selected, nothin to do
			return false;
		}

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
