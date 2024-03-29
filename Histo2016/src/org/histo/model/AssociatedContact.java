package org.histo.model;

import static javax.persistence.CascadeType.ALL;
import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.patient.Task;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "associatedcontact_sequencegenerator", sequenceName = "associatedcontact_sequence")
@Getter
@Setter
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
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

	@OrderColumn(name = "position")
	@LazyCollection(FALSE)
	@OneToMany(mappedBy = "contact", cascade = ALL)
	private List<AssociatedContactNotification> notifications = new ArrayList<AssociatedContactNotification>();

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

	@Transient
	public boolean isNotificationPerformed() {
		if (getNotifications() != null && getNotifications().size() > 0) {
			if (getNotifications().stream().anyMatch(p -> p.isPerformed()))
				return true;
		}
		return false;
	}

	@Transient
	public boolean containsNotificationTyp(NotificationTyp type, boolean performed) {
		return getNotificationTypAsList(type, performed).size() > 0;
	}

	@Transient
	public List<AssociatedContactNotification> getNotificationTypAsList(NotificationTyp type, boolean active) {
		return getNotifications().stream().filter(p -> p.getNotificationTyp().equals(type) && p.isActive() == active)
				.collect(Collectors.toList());
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

		Optional<String> addition1;
		Optional<String> addition2;
		Optional<String> street;
		Optional<String> postcode;
		Optional<String> town;

		if (organization != null) {
			street = Optional.ofNullable(organization.getContact().getStreet()).filter(s -> !s.isEmpty());
			postcode = Optional.ofNullable(organization.getContact().getPostcode()).filter(s -> !s.isEmpty());
			town = Optional.ofNullable(organization.getContact().getTown()).filter(s -> !s.isEmpty());
			addition1 = Optional.ofNullable(organization.getContact().getAddressadditon()).filter(s -> !s.isEmpty());
			addition2 = Optional.ofNullable(organization.getContact().getAddressadditon2()).filter(s -> !s.isEmpty());
			buffer.append(organization.getName() + "\r\n");

		} else {
			// no organization is selected or present, so add the data of the
			// user
			street = Optional.ofNullable(associatedContact.getPerson().getContact().getStreet())
					.filter(s -> !s.isEmpty());
			postcode = Optional.ofNullable(associatedContact.getPerson().getContact().getPostcode())
					.filter(s -> !s.isEmpty());
			town = Optional.ofNullable(associatedContact.getPerson().getContact().getTown()).filter(s -> !s.isEmpty());
			addition1 = Optional.ofNullable(associatedContact.getPerson().getContact().getAddressadditon()).filter(s -> !s.isEmpty());
			addition2 = Optional.ofNullable(associatedContact.getPerson().getContact().getAddressadditon2()).filter(s -> !s.isEmpty());
		}

		buffer.append(addition1.isPresent() ? addition1.get() + "\r\n" : "");
		buffer.append(addition2.isPresent() ? addition2.get() + "\r\n" : "");
		buffer.append(street.isPresent() ? street.get() + "\r\n" : "");
		buffer.append(postcode.isPresent() ? postcode.get() + " " : "");
		buffer.append(town.isPresent() ? town.get() + "\r\n" : "");

		return buffer.toString();
	}
}
