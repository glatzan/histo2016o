package org.histo.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.envers.Audited;
import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "organization_sequencegenerator", sequenceName = "organization_sequence", allocationSize = 1)
@Getter
@Setter
@Audited
public class Organization implements HasID {

	@Id
	@GeneratedValue(generator = "organization_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;
	@Column(columnDefinition = "VARCHAR", unique = true)
	private String name;
	@OneToOne(cascade = CascadeType.ALL)
	private Contact contact;
	@Column(columnDefinition = "VARCHAR")
	private String note;
	@OneToMany(fetch = FetchType.LAZY)
	private List<Person> persons;
	@Column
	private boolean intern;
}
