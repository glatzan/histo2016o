package org.histo.model;

import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Dialog;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "person_sequencegenerator", sequenceName = "person_sequence")
@Getter
@Setter
public class Person implements Serializable, LogAble, ArchivAble, HasID {

	private static final long serialVersionUID = 2533238775751991883L;

	@Version
	private long version;

	@Id
	@GeneratedValue(generator = "person_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Enumerated(EnumType.ORDINAL)
	private Gender gender = Gender.UNKNOWN;

	@Column(columnDefinition = "VARCHAR")
	private String title = "";

	@Column(columnDefinition = "VARCHAR")
	private String lastName;

	@Column(columnDefinition = "VARCHAR")
	private String firstName;

	@Column(columnDefinition = "VARCHAR")
	private String birthName;

	@Column(columnDefinition = "VARCHAR")
	@Type(type = "date")
	private Date birthday;

	@Column(columnDefinition = "VARCHAR")
	private String language;

	@Column(columnDefinition = "VARCHAR")
	private String note;

	@OneToOne(cascade = CascadeType.ALL)
	private Contact contact;

	@ManyToMany()
	@LazyCollection(FALSE)
	@JoinTable(name = "person_organization", joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "organization_id"))
	private List<Organization> organizsations;

	public enum Gender {
		MALE, FEMALE, UNKNOWN;
	}

	public Person() {
	}

	public Person(String name) {
		setLastName(name);
	}

	public Person(Contact contact) {
		this.contact = contact;
	}

	protected boolean archived;

	@Transient
	public String patienDataAsGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

	/**
	 * Returns a full name with title, name and surname.
	 * 
	 * @return
	 */
	@Transient
	public String getFullName() {
		StringBuilder result = new StringBuilder();

		if (getTitle() != null && !getTitle().isEmpty())
			result.append(getTitle() + " ");

		if (getLastName() != null && !getLastName().isEmpty())
			result.append(getLastName() + " ");

		if (getFirstName() != null && !getFirstName().isEmpty())
			result.append(getFirstName() + " ");

		// remove the last space from the string
		if (result.length() > 0)
			return result.substring(0, result.length() - 1);
		else
			return "";
	}

	/**
	 * Returns a title + name, if no title is provided,
	 * 
	 * @return
	 */
	@Transient
	public String getFullNameAndTitle() {
		StringBuilder result = new StringBuilder();

		if (getTitle() != null && !getTitle().isEmpty())
			result.append(getTitle() + " ");
		else {
			// TODO hardcoded!
			if (getGender() == Gender.FEMALE)
				result.append("Frau ");
			else
				result.append("Herr ");

		}

		int index = result.indexOf("Apl.");
		if (index != -1)
			result.replace(index, 4, "");

		if (getLastName() != null && !getLastName().isEmpty())
			result.append(getLastName() + " ");

		return result.substring(0, result.length() - 1);
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Person && ((Person) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

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
