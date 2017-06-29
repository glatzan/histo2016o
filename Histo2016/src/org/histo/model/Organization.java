package org.histo.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "organization_sequencegenerator", sequenceName = "organization_sequence", allocationSize = 1)
@Getter
@Setter
public class Organization {

	@Id
	@GeneratedValue(generator = "contact_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;
	@Column(columnDefinition = "VARCHAR")
	private String name;
	@OneToOne
	private Contact contact;
	@Column(columnDefinition = "VARCHAR")
	private String note;
	@OneToMany(fetch=FetchType.EAGER)
	private List<Person> persons;
	
}
