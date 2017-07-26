package org.histo.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "physician_sequencegenerator", sequenceName = "physician_sequence")
@Getter
@Setter
public class Physician implements Serializable, ArchivAble, HasID {

	private static Logger logger = Logger.getLogger("org.histo");

	private static final long serialVersionUID = 7358147861813210904L;

	@Id
	@GeneratedValue(generator = "physician_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * clinic internal title
	 */
	@Column(columnDefinition = "VARCHAR")
	private String clinicRole;

	/**
	 * Number of the employee
	 */
	@Column(columnDefinition = "VARCHAR")
	private String employeeNumber;

	/**
	 * Loginname of the physician
	 */
	@Column(columnDefinition = "VARCHAR")
	private String uid;

	/**
	 * True if clinic employee
	 */
	@Column
	// TODO remove
	private boolean clinicEmployee;

	/**
	 * List of all contactRoles
	 */

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Fetch(value = FetchMode.SUBSELECT)
	@Cascade(value = { org.hibernate.annotations.CascadeType.ALL })
	private Set<ContactRole> associatedRoles;

	/**
	 * Person data of the physician
	 */
	@OneToOne(cascade = CascadeType.ALL)
	private Person person;

	/**
	 * Transitory, if fetched from ldap this variable contains the dn objects
	 * name.
	 */
	@Transient
	private String dnObjectName;

	/**
	 * If true this object is archived
	 */
	@Column
	private boolean archived;

	/**
	 * Standard constructor for hibernate
	 */
	public Physician() {
	}

	/**
	 * Constructor with id
	 * 
	 * @param id
	 */
	public Physician(long id) {
		this.id = id;
	}

	/**
	 * Constructor setting person
	 * 
	 * @param person
	 */
	public Physician(Person person) {
		this.person = person;
	}

	/**
	 * Copies data from an other physician object into this object
	 * 
	 * @param dataToUpdate
	 */
	public void copyIntoObject(Physician dataToUpdate) {
		// TOTO move to person
		getPerson().setLastName(dataToUpdate.getPerson().getLastName());
		getPerson().setFirstName(dataToUpdate.getPerson().getFirstName());
		getPerson().getContact().setEmail(dataToUpdate.getPerson().getContact().getEmail());
		getPerson().getContact().setPhone(dataToUpdate.getPerson().getContact().getPhone());
		getPerson().getContact().setPager(dataToUpdate.getPerson().getContact().getPager());
		getPerson().setTitle(dataToUpdate.getPerson().getTitle());

		setEmployeeNumber(dataToUpdate.getEmployeeNumber());
		setEmployeeNumber(dataToUpdate.getEmployeeNumber());
		setUid(dataToUpdate.getUid());
		setClinicRole(dataToUpdate.getClinicRole());

		getPerson().setOrganizsations(dataToUpdate.getPerson().getOrganizsations());
		// TODO is this necessary ?
		// setAssociatedRoles(dataToUpdate.getAssociatedRoles());
	}

	public Set<ContactRole> getAssociatedRoles() {
		if (associatedRoles == null)
			associatedRoles = new HashSet<ContactRole>();

		return associatedRoles;
	}

	/********************************************************
	 * Transient
	 ********************************************************/
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Physician && ((Physician) obj).getId() == getId())
			return true;
		return super.equals(obj);
	}

	/**
	 * Used for gui, can only handle arrays
	 * 
	 * @return
	 */
	@Transient
	public ContactRole[] getAssociatedRolesAsArray() {
		return (ContactRole[]) getAssociatedRoles().toArray(new ContactRole[associatedRoles.size()]);
	}

	public void setAssociatedRolesAsArray(ContactRole[] associatedRoles) {
		this.associatedRoles = new HashSet<>(Arrays.asList(associatedRoles));
	}

	/**
	 * Returns true if physician has role
	 * 
	 * @param role
	 */
	@Transient
	public boolean hasAssociateRole(ContactRole role) {
		for (ContactRole contactRole : getAssociatedRoles()) {
			if (contactRole == role)
				return true;
		}

		return false;
	}

	/**
	 * Returns true if no role is associate
	 * 
	 * @return
	 */
	@Transient
	public boolean hasNoAssociateRole() {
		if (getAssociatedRoles().size() == 0)
			return true;
		return false;
	}

	/**
	 * Returns true if no role is associate
	 * 
	 * @return
	 */
	@Transient
	public void addAssociateRole(ContactRole role) {
		getAssociatedRoles().add(role);
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	/********************************************************
	 * Interace archive able
	 ********************************************************/
	@Override
	public boolean isArchived() {
		return archived;
	}

	@Override
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	@Override
	@Transient
	public String getTextIdentifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	@Transient
	public Dialog getArchiveDialog() {
		// TODO Auto-generated method stub
		return null;
	}
	/********************************************************
	 * Interace archive able
	 ********************************************************/
}
