package org.histo.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.action.TaskHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.model.interfaces.LogAble;

@Entity
@SequenceGenerator(name = "contact_sequencegenerator", sequenceName = "contact_sequence")
public class Contact implements LogAble {

	private static Logger logger = Logger.getLogger(TaskHandlerAction.class);

	private long id;

	private Person person;

	private ContactRole role = ContactRole.NONE;

	private boolean primaryContact;

	private boolean usePhone;

	private boolean useFax;

	private boolean useEmail;

	private boolean notificationPerformed;

	private long notificationDate;

	/**
	 * Transient, wird für das Auswählen neuer Kontakte und das abwählen alter
	 * benötigt
	 */
	private boolean selected;

	public Contact() {
	}

	public Contact(Person person) {
		this(person, ContactRole.NONE);
		logger.debug("Creating contact for " + person.getFullName());
	}

	public Contact(Person person, ContactRole role) {
		logger.debug("Creating contact for " + person.getFullName());
		this.person = person;
		this.role = role;
	}

	/********************************************************
	 * Getters/Setters
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "contact_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToOne(cascade = CascadeType.ALL)
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Enumerated(EnumType.STRING)
	public ContactRole getRole() {
		return role;
	}

	public void setRole(ContactRole role) {
		this.role = role;
	}

	public boolean isUsePhone() {
		return usePhone;
	}

	public void setUsePhone(boolean usePhone) {
		this.usePhone = usePhone;
	}

	public boolean isUseFax() {
		return useFax;
	}

	public void setUseFax(boolean useFax) {
		this.useFax = useFax;
	}

	public boolean isUseEmail() {
		return useEmail;
	}

	public void setUseEmail(boolean useEmail) {
		this.useEmail = useEmail;
	}

	public boolean isNotificationPerformed() {
		return notificationPerformed;
	}

	public void setNotificationPerformed(boolean notificationPerformed) {
		this.notificationPerformed = notificationPerformed;

		if (notificationPerformed)
			setNotificationDate(System.currentTimeMillis());
	}

	public boolean isPrimaryContact() {
		return primaryContact;
	}

	public void setPrimaryContact(boolean primaryContact) {
		this.primaryContact = primaryContact;
	}

	public long getNotificationDate() {
		return notificationDate;
	}

	public void setNotificationDate(long notificationDate) {
		this.notificationDate = notificationDate;
	}

	/********************************************************
	 * Getters/Setters
	 ********************************************************/

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/
	@Transient
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/

	@Override
	public String toString() {
		if (getPerson().getFullName() != null && !getPerson().getFullName().isEmpty())
			return getPerson().getFullName();
		else
			return getPerson().getTitle() + " " + getPerson().getSurname() + " " + getPerson().getName();
	}

}
