package org.histo.model.view;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.hibernate.annotations.Immutable;
import org.histo.config.enums.ContactRole;

//@Entity
//@Immutable
public class ContactPhysicanRole {

	private Long id;
	
	private Long personID;
	
	private String title;
	
	private String name;
	
	private String surname;
	
	/**
	 * List of all contactRoles
	 */
	private Set<ContactRole> associatedRoles;

	// ************************ Getter/Setter ************************
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(updatable = false, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@Column
	public Long getPersonID() {
		return personID;
	}


	public void setPersonID(Long personID) {
		this.personID = personID;
	}

	@Column
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column
	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	@ElementCollection
	@CollectionTable(
	        name="physician_associatedroles",
	        joinColumns=@JoinColumn(name="physician_id")
	  )
	@Enumerated(EnumType.STRING)
	public Set<ContactRole> getAssociatedRoles() {
		return associatedRoles;
	}

	public void setAssociatedRoles(Set<ContactRole> associatedRoles) {
		this.associatedRoles = associatedRoles;
	}
	
	
}
