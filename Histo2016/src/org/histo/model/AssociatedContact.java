package org.histo.model;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.util.List;
import java.util.Optional;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.hibernate.annotations.LazyCollection;
import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.patient.Task;
import org.histo.util.latex.TextToLatexConverter;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "associatedcontact_sequencegenerator", sequenceName = "associatedcontact_sequence")
@Getter
@Setter
public class AssociatedContact implements LogAble, HasID {

	private static Logger logger = Logger.getLogger("org.histo");

	@ManyToOne(fetch = FetchType.LAZY)
	private Task task;

	@Id
	@GeneratedValue(generator = "associatedcontact_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	/**
	 * All cascade types, but not removing!
	 * 
	 * @return
	 */
	@OneToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH })
	private Person person;

	@Enumerated(EnumType.STRING)
	private ContactRole role = ContactRole.NONE;

	@Column(columnDefinition = "VARCHAR")
	private String customContact;

	@OrderColumn(name = "position")
	@LazyCollection(FALSE)
	@OneToMany(mappedBy = "contact", cascade = ALL)
	private List<AssociatedContactNotification> notifications;

	public AssociatedContact() {
	}

	public AssociatedContact(Task task, Person person) {
		this(task, person, ContactRole.NONE);
	}

	public AssociatedContact(Task task, Person person, ContactRole role) {
		this.person = person;
		this.role = role;
		this.task = task;
	}

	/**
	 * Returns if set the customContact (manually changed by the user),
	 * otherwise it will generate the default address field
	 * 
	 * @return
	 */
	@Transient
	public String getContactAsString() {
		Optional<String> address = Optional.ofNullable(getCustomContact());

		if (address.isPresent())
			return address.get();

		return generateAddress(this);
	}

	@Transient
	public String getContactAsLatex() {
		return (new TextToLatexConverter()).convertToTex(getContactAsString());
	}

	@Transient
	public boolean isNotificationPerformed() {
		if (getNotifications() != null && getNotifications().size() > 0) {
			if (getNotifications().stream().anyMatch(p -> p.isPerformed()))
				return true;
		}
		return false;
	}

	@Transient
	public boolean containsNotificationTyp(NotificationTyp type) {
		return getNotifications().stream().anyMatch(p -> p.getNotificationTyp().equals(type) && !p.isPerformed());
	}
	
	@Override
	public String toString() {
		if (getPerson().getFullName() != null && !getPerson().getFullName().isEmpty())
			return getPerson().getFullName();
		else
			return getPerson().getTitle() + " " + getPerson().getFirstName() + " " + getPerson().getLastName();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AssociatedContact) {
			if (((AssociatedContact) obj).getId() == getId())
				return true;
			// same person with the same role, same object
			if (((AssociatedContact) obj).getPerson().equals(getPerson())
					&& ((AssociatedContact) obj).getRole().equals(getRole()))
				return true;
		}

		return super.equals(obj);
	}

	public static String generateAddress(AssociatedContact associatedContact) {
		return generateAddress(associatedContact, null);
	}

	public static String generateAddress(AssociatedContact associatedContact, Organization organization) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(associatedContact.getPerson().getFullName() + "\r\n");

		Optional<String> street;
		Optional<String> postcode;
		Optional<String> town;

		if (organization != null) {
			street = Optional.ofNullable(organization.getContact().getStreet()).filter(s -> !s.isEmpty());
			postcode = Optional.ofNullable(organization.getContact().getPostcode()).filter(s -> !s.isEmpty());
			town = Optional.ofNullable(organization.getContact().getTown()).filter(s -> !s.isEmpty());
			buffer.append(organization.getName() + "\r\n");

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
