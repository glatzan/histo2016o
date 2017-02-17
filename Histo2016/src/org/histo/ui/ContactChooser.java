package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Person;

public class ContactChooser {

	public Contact contact;

	public int copies;

	public boolean selected;

	public ContactChooser(Person person, ContactRole role) {
		this(new Contact(person, role));
	}
	
	public ContactChooser(Contact contact){
		this.contact = contact;
		this.copies = 1;
		this.selected = false;
	}
	
	public static List<ContactChooser> getContactChooserList(List<Contact> contacts){
		ArrayList<ContactChooser> result = new ArrayList<ContactChooser>();
		for (Contact contact : contacts) {
			result.add(new ContactChooser(contact));
		}
		return result;
	}
	
	public Contact getContact() {
		return contact;
	}

	public int getCopies() {
		return copies;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
	}

	public void setCopies(int copies) {
		this.copies = copies;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
