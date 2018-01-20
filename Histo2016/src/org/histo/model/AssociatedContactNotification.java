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

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "associatedcontactnotification_sequencegenerator", sequenceName = "associatedcontactnotification_sequence")
@Getter
@Setter
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
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

	@Column
	private boolean failed;
	
	@Column
	private boolean renewed;

	@Column(columnDefinition = "VARCHAR")
	private String commentary;

	@Temporal(TemporalType.DATE)
	private Date dateOfAction;

	@Column
	private boolean manuallyAdded;

	@Column(columnDefinition = "VARCHAR")
	private String contactAddress;

	public enum NotificationTyp {
		EMAIL, FAX, PHONE, LETTER, PRINT;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AssociatedContactNotification && ((AssociatedContactNotification) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}
}
