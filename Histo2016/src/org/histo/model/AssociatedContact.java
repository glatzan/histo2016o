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
	
	@Column
	private boolean primaryContact;

	@Column
	private boolean usePhone;

	@Column
	private boolean useFax;

	@Column
	private boolean useEmail;

	@Column
	private long emailNotificationDate;

	@Column
	private long faxNotificationDate;

	@Column
	private long phoneNotificationDate;

	/********************************************************
	 * Transient
	 ********************************************************/
	/**
	 * Transient, is used for selecting contacts an marking already selected
	 * ones.
	 */
	@Transient
	private boolean selected;

	/**
	 * Transient, is used for selecting contacts from a list
	 */
	@Transient
	private int tmpId;

	/********************************************************
	 * Transient
	 ********************************************************/

	public AssociatedContact() {
	}

	public AssociatedContact(Task task,Person person) {
		this( task,person, ContactRole.NONE);
	}

	public AssociatedContact(Task task,Person person, ContactRole role) {
		logger.debug("Creating associatedContact for " + person.getFullName());
		this.person = person;
		this.role = role;
		this.task = task;
	}

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/
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
			return getPerson().getTitle() + " " + getPerson().getFirstName() + " " + getPerson().getLastName();
	}

}
