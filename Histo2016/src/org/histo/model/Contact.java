package org.histo.model;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.config.enums.ContactRole;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.patient.Task;

@Entity
@SequenceGenerator(name = "contact_sequencegenerator", sequenceName = "contact_sequence")
public class Contact implements LogAble, HasID {

	private static Logger logger = Logger.getLogger("org.histo");

	private Task task;

	private long id;

	private Person person;

	private ContactRole role = ContactRole.NONE;

	private boolean primaryContact;

	private boolean usePhone;

	private boolean useFax;

	private boolean useEmail;

	private long emailNotificationDate;

	private long faxNotificationDate;

	private long phoneNotificationDate;

	/********************************************************
	 * Transient
	 ********************************************************/
	/**
	 * Transient, is used for selecting contacts an marking already selected
	 * ones.
	 */
	private boolean selected;

	/**
	 * Transient, is used for selecting contacts from a list
	 */
	private int tmpId;

	/********************************************************
	 * Transient
	 ********************************************************/

	public Contact() {
	}

	public Contact(Task task,Person person) {
		this( task,person, ContactRole.NONE);
	}

	public Contact(Task task,Person person, ContactRole role) {
		logger.debug("Creating contact for " + person.getFullName());
		this.person = person;
		this.role = role;
		this.task = task;
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

	/**
	 * All cascade types, but not removing!
	 * 
	 * @return
	 */
	@OneToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH })
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

	public boolean isPrimaryContact() {
		return primaryContact;
	}

	public void setPrimaryContact(boolean primaryContact) {
		this.primaryContact = primaryContact;
	}

	public long getEmailNotificationDate() {
		return emailNotificationDate;
	}

	public long getFaxNotificationDate() {
		return faxNotificationDate;
	}

	public long getPhoneNotificationDate() {
		return phoneNotificationDate;
	}

	public void setEmailNotificationDate(long emailNotificationDate) {
		this.emailNotificationDate = emailNotificationDate;
	}

	public void setFaxNotificationDate(long faxNotificationDate) {
		this.faxNotificationDate = faxNotificationDate;
	}

	public void setPhoneNotificationDate(long phoneNotificationDate) {
		this.phoneNotificationDate = phoneNotificationDate;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
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

	@Transient
	public int getTmpId() {
		return tmpId;
	}

	public void setTmpId(int tmpId) {
		this.tmpId = tmpId;
	}

	@Transient
	public boolean isEmailNotificationPerformed() {
		return getEmailNotificationDate() != 0;
	}

	public void setEmailNotificationPerformed(boolean performed) {
		setEmailNotificationDate(performed ? System.currentTimeMillis() : 0);
	}

	@Transient
	public boolean isFaxNotificationPerformed() {
		return getFaxNotificationDate() != 0;
	}

	public void setFaxNotificationPerformed(boolean performed) {
		setFaxNotificationDate(performed ? System.currentTimeMillis() : 0);
	}

	@Transient
	public boolean isPhoneNotificationPerformed() {
		return getPhoneNotificationDate() != 0;
	}

	public void setPhoneNotificationPerformed(boolean performed) {
		setPhoneNotificationDate(performed ? System.currentTimeMillis() : 0);
	}

	/**
	 * 
	 * @return
	 */
	@Transient
	public boolean isNotificationPerformed() {

		if (isUseEmail() || isUseFax() || isUsePhone()) {
			boolean email = true, fax = true, phone = true;

			// sets email to false if useEmails and not performed
			if (isUseEmail() && getPhoneNotificationDate() == 0)
				email = false;

			if (isUseFax() && getFaxNotificationDate() == 0)
				fax = false;

			if (isUsePhone() && getPhoneNotificationDate() == 0)
				phone = false;

			return email && fax && phone;
		}
		return false;
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
