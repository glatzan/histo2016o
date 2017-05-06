package org.histo.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;

import com.google.gson.annotations.Expose;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "physician_sequencegenerator", sequenceName = "physician_sequence")
public class Physician implements Serializable, ArchivAble, HasID {

	private static Logger logger = Logger.getLogger("org.histo");

	private static final long serialVersionUID = 7358147861813210904L;

	private long version;

	protected long id;

	/**
	 * Pager Number
	 */
	@Expose
	private String pager;

	/**
	 * clinic internal title
	 */
	@Expose
	private String clinicRole;

	/**
	 * Number of the employee
	 */
	@Expose
	private String employeeNumber;

	/**
	 * Loginname of the physician
	 */
	@Expose
	private String uid;

	/**
	 * True if clinic employee
	 */
	private boolean clinicEmployee;

	/**
	 * List of all contactRoles
	 */
	private Set<ContactRole> associatedRoles;

	/**
	 * Person data of the physician
	 */
	private Person person;

	/**
	 * Transitory, if fetched from ldap this variable contains the dn objects
	 * name.
	 */
	private String dnObjectName;

	/**
	 * If true this object is archived
	 */
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
		getPerson().setName(dataToUpdate.getPerson().getName());
		getPerson().setSurname(dataToUpdate.getPerson().getSurname());
		getPerson().setEmail(dataToUpdate.getPerson().getEmail());
		getPerson().setPhoneNumber(dataToUpdate.getPerson().getPhoneNumber());
		getPerson().setDepartment(dataToUpdate.getPerson().getDepartment());

		setEmployeeNumber(dataToUpdate.getEmployeeNumber());
		setEmployeeNumber(dataToUpdate.getEmployeeNumber());
		setUid(dataToUpdate.getUid());
		setPager(dataToUpdate.getPager());
		setClinicRole(dataToUpdate.getClinicRole());
		setAssociatedRoles(dataToUpdate.getAssociatedRoles());
	}

	/**
	 * Copies data from ldap into this physician object. cn: Dr. Michael Reich
	 * ou: Klinik für Augenheilkunde givenName: Andreas mail:
	 * andreas.glatz@uniklinik-freiburg.de sn: Glatz title: Arzt
	 * telephonenumber: +49 761 270 40010 pager: 12-4027
	 * 
	 * @param attrs
	 */
	public void copyIntoObject(Attributes attrs) {

		logger.debug("Upadting physician data for " + getUid() + " from ldap");

		try {
			// name surname title
			Attribute attr = attrs.get("personalTitle");

			if (attr != null && attr.size() == 1) {
				getPerson().setTitle(attr.get().toString());
			}

			// uid
			attr = attrs.get("uid");
			if (attr != null && attr.size() == 1) {
				setUid(attr.get().toString());
			}

			// name
			attr = attrs.get("sn");
			if (attr != null && attr.size() == 1) {
				getPerson().setName(attr.get().toString());
			}

			attr = attrs.get("employeeNumber");
			if (attr != null && attr.size() == 1) {
				setEmployeeNumber(attr.get().toString());
			}

			attr = attrs.get("givenName");
			if (attr != null && attr.size() == 1) {
				getPerson().setSurname(attr.get().toString());
			}

			attr = attrs.get("mail");
			if (attr != null && attr.size() == 1) {
				getPerson().setEmail(attr.get().toString());
			}

			attr = attrs.get("telephonenumber");
			if (attr != null && attr.size() == 1) {
				getPerson().setPhoneNumber(attr.get().toString());
			}

			attr = attrs.get("pager");
			if (attr != null && attr.size() == 1) {
				setPager(attr.get().toString());
			}

			// role in clinic
			attr = attrs.get("title");
			if (attr != null && attr.size() == 1) {
				setClinicRole(attr.get().toString());
			}

			// department
			attr = attrs.get("ou");
			if (attr != null && attr.size() == 1) {
				getPerson().setDepartment(attr.get().toString());
			}
		} catch (NamingException e) {
			logger.error("Error while updating physician data for " + getUid() + " from ldap", e);
		}
	}

	@Id
	@GeneratedValue(generator = "physician_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@OneToOne(cascade = CascadeType.ALL)
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public String getPager() {
		return pager;
	}

	public void setPager(String pager) {
		this.pager = pager;
	}

	public String getClinicRole() {
		return clinicRole;
	}

	public void setClinicRole(String clinicRole) {
		this.clinicRole = clinicRole;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public boolean isClinicEmployee() {
		return clinicEmployee;
	}

	public void setClinicEmployee(boolean clinicEmployee) {
		this.clinicEmployee = clinicEmployee;
	}
	

	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Fetch(value = FetchMode.SUBSELECT)
	public Set<ContactRole> getAssociatedRoles() {
		if(associatedRoles == null)
			associatedRoles = new HashSet<ContactRole>();
		
		return associatedRoles;
	}

	public void setAssociatedRoles(Set<ContactRole> associatedRoles) {
		this.associatedRoles = associatedRoles;
	}

	/********************************************************
	 * Transient
	 ********************************************************/
	
	@Transient
	public String getDnObjectName() {
		return dnObjectName;
	}

	public void setDnObjectName(String dnObjectName) {
		this.dnObjectName = dnObjectName;
	}

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
