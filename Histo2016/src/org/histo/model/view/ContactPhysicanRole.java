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

import lombok.Getter;
import lombok.Setter;

//@Entity
//@Immutable
//@Getter
//@Setter
public class ContactPhysicanRole {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(updatable = false, nullable = false)
	private Long id;
	
	@Column
	private Long personID;
	
	@Column
	private String title;
	
	@Column
	private String lastName;
	
	@Column
	private String firstName;
	
	/**
	 * List of all contactRoles
	 */
	@ElementCollection
	@CollectionTable(
	        name="physician_associatedroles",
	        joinColumns=@JoinColumn(name="physician_id")
	  )
	@Enumerated(EnumType.STRING)
	private Set<ContactRole> associatedRoles;

	// ************************ Getter/Setter ************************
}
