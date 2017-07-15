package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactChooser {

	private AssociatedContact contact;

	private int copies;

	private boolean selected;

	private List<OrganizationChooser> organizazionsChoosers;

	public ContactChooser(Task task, Person person, ContactRole role) {
		this(new AssociatedContact(task, person, role));
	}

	public ContactChooser(AssociatedContact associatedContact) {
		this.contact = associatedContact;
		this.copies = 1;
		this.selected = false;
		this.organizazionsChoosers = new ArrayList<OrganizationChooser>();

		if (associatedContact.getPerson().getOrganizsations() != null)
			for (Organization organization : associatedContact.getPerson().getOrganizsations()) {
				this.organizazionsChoosers.add(new OrganizationChooser(this, organization));
			}
	}

	@Getter
	@Setter
	public class OrganizationChooser {

		private ContactChooser parent;
		private Organization organization;
		private boolean selected;

		public OrganizationChooser(ContactChooser parent, Organization organization) {
			this.parent = parent;
			this.organization = organization;
		}
	}

	public static List<ContactChooser> factory(List<AssociatedContact> associatedContacts) {
		ArrayList<ContactChooser> result = new ArrayList<ContactChooser>();
		for (AssociatedContact associatedContact : associatedContacts) {
			result.add(new ContactChooser(associatedContact));
		}
		return result;
	}
}
