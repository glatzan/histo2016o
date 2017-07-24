package org.histo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "associatedcontactnotification_sequencegenerator", sequenceName = "associatedcontactnotification_sequence")
@Getter
@Setter
public class AssociatedContactNotification implements HasID {

	@Id
	@GeneratedValue(generator = "associatedcontactnotification_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@ManyToOne(targetEntity = AssociatedContact.class, fetch = FetchType.LAZY)
	private AssociatedContact contact;

	@Enumerated(EnumType.STRING)
	private NotificationTyp notificationTyp;

	@Column
	private boolean active;

	@Column
	private boolean performed;

	@Temporal(TemporalType.DATE)
	private Date performedDate;

	@Column
	private boolean manuallyAdded;
	
	public enum NotificationTyp {
		EMAIL, FAX, PHONE, LETTER;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AssociatedContactNotification && ((AssociatedContactNotification) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

}