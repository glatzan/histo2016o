package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "contact_sequencegenerator", sequenceName = "contact_sequence")
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@Getter
@Setter
@Audited
public class Contact {

	@Id
	@GeneratedValue(generator = "contact_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;
	@Column(columnDefinition = "VARCHAR")
	private String building;
	@Column(columnDefinition = "VARCHAR")
	private String street;
	@Column(columnDefinition = "VARCHAR")
	private String town;
	@Column(columnDefinition = "VARCHAR")
	private String postcode;
	@Column(columnDefinition = "VARCHAR")
	private String country;
	@Column(columnDefinition = "VARCHAR")
	private String phone;
	@Column(columnDefinition = "VARCHAR")
	private String mobile;
	@Column(columnDefinition = "VARCHAR")
	private String email;
	@Column(columnDefinition = "VARCHAR")
	private String homepage;
	@Column(columnDefinition = "VARCHAR")
	private String fax;
	@Column(columnDefinition = "VARCHAR")
	private String pager;
	
	
}
