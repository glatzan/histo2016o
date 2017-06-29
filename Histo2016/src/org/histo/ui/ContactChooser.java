package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContact;
import org.histo.model.Person;
import org.histo.model.patient.Task;

public class ContactChooser {

	public AssociatedContact associatedContact;

	public int copies;

	public boolean selected;

	public ContactChooser(Task task,Person person, ContactRole role) {
		this(new AssociatedContact(task,person, role));
	}
	
	public ContactChooser(AssociatedContact associatedContact){
		this.associatedContact = associatedContact;
		this.copies = 1;
		this.selected = false;
	}
	
	public static List<ContactChooser> getContactChooserList(List<AssociatedContact> associatedContacts){
		ArrayList<ContactChooser> result = new ArrayList<ContactChooser>();
		for (AssociatedContact associatedContact : associatedContacts) {
			result.add(new ContactChooser(associatedContact));
		}
		return result;
	}
	
	public AssociatedContact getContact() {
		return associatedContact;
	}

	public int getCopies() {
		return copies;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setContact(AssociatedContact associatedContact) {
		this.associatedContact = associatedContact;
	}

	public void setCopies(int copies) {
		this.copies = copies;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
