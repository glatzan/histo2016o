package org.histo.model;

import static org.hibernate.annotations.LazyCollectionOption.FALSE;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

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
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Dialog;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

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
public class Person implements Serializable, LogAble, ArchivAble {

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
	@OneToOne
	private Contact contact;

	@ManyToMany
	@LazyCollection(FALSE)
	@JoinTable(uniqueConstraints = @UniqueConstraint(columnNames = { "person_id",
			"organization_id" }), joinColumns = @JoinColumn(name = "person_id"), inverseJoinColumns = @JoinColumn(name = "organization_id"))
	private List<Organization> organizsations;

	public enum Gender {
		MALE, FEMALE, UNKNOWN;
	}
	//
	// @ExposeORDINAL
	// protected String name = "";
	// @Expose
	// protected String street = "";
	// @Expose
	// protected String postcode = "";
	// @Expose
	// protected String town = "";
	// @Expose
	// protected String surname = "";
	// @Expose
	// protected String phoneNumber = "";
	// @Expose
	// protected String mobileNumber = "";
	// @Expose
	// protected String fax = "";
	// @Expose
	// protected String email = "";
	// @Expose
	// protected String country = "";
	// @Expose
	// protected String department = "";

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

		if (getName() != null && !getName().isEmpty())
			result.append(getName() + " ");

		if (getSurname() != null && !getSurname().isEmpty())
			result.append(getSurname() + " ");

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

		if (getName() != null && !getName().isEmpty())
			result.append(getName() + " ");

		return result.substring(0, result.length() - 1);
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
