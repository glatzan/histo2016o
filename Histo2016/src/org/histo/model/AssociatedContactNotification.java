package org.histo.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.histo.model.patient.Patient;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "associatedcontactnotification_sequencegenerator", sequenceName = "associatedcontactnotification_sequence")
@Getter
@Setter
public class AssociatedContactNotification {

	@Id
	@GeneratedValue(generator = "associatedcontactnotification_sequencegenerator")
	@Column(unique = true, nullable = false)
	private int id;

	@Enumerated(EnumType.STRING)
	private NotificationTyp notificationTyp;

	@Column
	private boolean active;

	@Column
	private boolean performed;

	@ManyToOne(targetEntity = AssociatedContact.class)
	private AssociatedContact contact;
	
	@Temporal(TemporalType.DATE)
	private Date performedDate;

	public enum NotificationTyp {
		EMAIL, FAX, PHONE, NOTIFY, LETTER;
	}

}
