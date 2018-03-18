package org.histo.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.model.interfaces.HasID;

import lombok.Getter;
import lombok.Setter;

@Entity
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "organization_sequencegenerator", sequenceName = "organization_sequence", allocationSize = 1)
@Getter
@Setter
@Audited
public class Organization implements HasID, Serializable {

	private static final long serialVersionUID = 8370367938117220795L;

	@Id
	@GeneratedValue(generator = "organization_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;
	@Version
	private long version;
	@Column(columnDefinition = "VARCHAR", unique = true)
	private String name;
	@OneToOne(cascade = CascadeType.ALL)
	private Contact contact;
	@Column(columnDefinition = "VARCHAR")
	private String note;

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "organizsations")
	private List<Person> persons;

	@Column
	private boolean intern;

	@Column(columnDefinition = "boolean default true")
	private boolean archived = false;

	public Organization() {

	}

	public Organization(Contact contact) {
		this.contact = contact;
	}

	public Organization(String name, Contact contact) {
		this.name = name;
		this.contact = contact;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Organization && ((Organization) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	@Override
	public String toString() {
		return "Organization [id=" + id + ", version=" + version + ", name=" + name + ", contact=" + contact + ", note="
				+ note + ", intern=" + intern + "]";
	}

}
