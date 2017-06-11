package org.histo.model.patient;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Gender;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomNullPatientExcepetion;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "patient_sequencegenerator", sequenceName = "patient_sequence")
public class Patient
		implements Parent<Patient>, CreationDate, LogAble, ArchivAble, PatientRollbackAble, HasDataList, HasID {

	private long id;

	private long version;

	/**
	 * PIZ
	 */
	private String piz = "";

	/**
	 * Insurance of the patient
	 */
	private String insurance = "";

	/**
	 * Date of adding to the database
	 */
	private long creationDate = 0;

	/**
	 * Person data
	 */
	private Person person;

	/**
	 * Task for this patient
	 */
	private List<Task> tasks;

	/**
	 * True if insurance is private
	 */
	private boolean privateInsurance = false;

	/**
	 * True if patient was added as an external patient.
	 */
	private boolean externalPatient = false;

	/**
	 * Pdf attached to this patient, this might be an informed consent
	 */
	private List<PDFContainer> attachedPdfs;

	/**
	 * If true the patient is archived. Thus he won't be displayed.
	 */
	private boolean archived = false;

	/**
	 * True if saved in database, false if only in clinic backend
	 */
	private boolean inDatabase = true;

	/**
	 * Standard constructor
	 */
	public Patient() {
	}

	public Patient(Person person) {
		this.person = person;
	}

	/********************************************************
	 * Transient Methods
	 ********************************************************/

	/**
	 * Updates the patient data with a given patient dummy. Returns true if data
	 * are changed. Returns true if data were changed
	 * 
	 * @param patient
	 */
	public final boolean copyIntoObject(Patient patient) {
		boolean change = false;

		if (isStringDifferent(getPiz(), patient.getPiz())) {
			change = true;
			setPiz(patient.getPiz());
		}

		if (isStringDifferent(getInsurance(), patient.getInsurance())) {
			change = true;
			setInsurance(getInsurance());
		}

		if (isStringDifferent(getPerson().getTitle(), patient.getPerson().getTitle())) {
			change = true;
			getPerson().setTitle(patient.getPerson().getTitle());
		}

		if (isStringDifferent(getPerson().getName(), patient.getPerson().getName())) {
			change = true;
			getPerson().setName(patient.getPerson().getName());
		}

		if (isStringDifferent(getPerson().getSurname(), patient.getPerson().getSurname())) {
			change = true;
			getPerson().setSurname(patient.getPerson().getSurname());
		}

		if (isStringDifferent(getPerson().getBirthday(), patient.getPerson().getBirthday())) {
			change = true;
			getPerson().setBirthday(patient.getPerson().getBirthday());
		}

		if (isStringDifferent(getPerson().getTown(), patient.getPerson().getTown())) {
			change = true;
			getPerson().setTown(patient.getPerson().getTown());
		}

		if (isStringDifferent(getPerson().getCountry(), patient.getPerson().getCountry())) {
			change = true;
			getPerson().setCountry(patient.getPerson().getCountry());
		}

		if (isStringDifferent(getPerson().getPostcode(), patient.getPerson().getPostcode())) {
			change = true;
			getPerson().setPostcode(patient.getPerson().getPostcode());
		}

		if (isStringDifferent(getPerson().getStreet(), patient.getPerson().getStreet())) {
			change = true;
			getPerson().setStreet(patient.getPerson().getStreet());
		}

		if (isStringDifferent(getPerson().getPhoneNumber(), patient.getPerson().getPhoneNumber())) {
			change = true;
			getPerson().setPhoneNumber(patient.getPerson().getPhoneNumber());
		}

		if (isStringDifferent(getPerson().getGender(), patient.getPerson().getGender())) {
			change = true;
			getPerson().setGender(patient.getPerson().getGender());
		}

		if (isStringDifferent(getPerson().getEmail(), patient.getPerson().getEmail())) {
			change = true;
			getPerson().setEmail(patient.getPerson().getEmail());
		}

		return change;
	}

	/**
	 * Updates the patient data with a given json string.
	 * 
	 * @param json
	 * @throws CustomNullPatientExcepetion
	 * @throws JSONException
	 */
	public final void copyIntoObject(String json) throws JSONException, CustomNullPatientExcepetion {
		copyIntoObject(new JSONObject(json));
	}

	/**
	 * Updates the patient object with a given json array from the clinic
	 * backend
	 *
	 * { "vorname":"Test", "mode":"W", "status":null, "piz":"25201957",
	 * "sonderinfo":"", "iknr":"00190", "kvnr":null, "titel":"Prof. Dr. med.",
	 * "versichertenstatus":" ", "tel":"12-4085", "anschrift": "Gillenweg 4",
	 * "wop":null, "plz":"79110", "name":"Test", "geburtsdatum":"1972-08-22",
	 * "gueltig_bis":null, "krankenkasse":"Wissenschaftliche Unters.",
	 * "versnr":null, "land":"D", "weiblich":"", "ort":"Freiburg",
	 * "status2":null }
	 * 
	 * @param patient
	 * @param json
	 * @return
	 * @throws CustomNullPatientExcepetion
	 */
	public final void copyIntoObject(JSONObject obj) throws CustomNullPatientExcepetion {

		// check if not an null patient is returned

		if (obj.optString("name").isEmpty())
			throw new CustomNullPatientExcepetion();

		Person person = getPerson();

		// Person data
		person.setTitle(obj.optString("titel"));
		person.setName(obj.optString("name"));

		person.setSurname(obj.optString("vorname"));

		// parsing date
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
		Date date;
		try {
			date = format.parse(obj.optString("geburtsdatum"));
		} catch (ParseException e) {
			date = new Date();
		}
		person.setBirthday(date);

		person.setTown(obj.optString("ort"));
		person.setCountry(obj.optString("land"));
		person.setPostcode(obj.optString("plz"));
		person.setStreet(obj.optString("anschrift"));
		person.setPhoneNumber(obj.optString("tel"));

		// 1 equals female, empty equals male
		person.setGender(obj.optString("weiblich").equals("1") ? Gender.FEMALE : Gender.MALE);

		// TODO
		person.setEmail("");

		// patient data
		setPiz(obj.optString("piz"));
		setInsurance(obj.optString("krankenkasse"));
	}

	@Override
	@Transient
	public String toString() {
		return "ID: " + getId() + ", Name: " + getPerson().getFullName() + ", PIZ: "
				+ (getPiz() == null ? "extern" : getPiz());
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Patient && ((Patient) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	@Transient
	public boolean isStringDifferent(Object arg1, Object arg2) {
		if (arg1 == arg2)
			return false;
		if (arg1 == null || arg2 == null)
			return true;
		if (arg1.equals(arg2))
			return false;
		return true;
	}

	@Transient
	public boolean isInDatabase() {
		return inDatabase;
	}

	public void setInDatabase(boolean inDatabase) {
		this.inDatabase = inDatabase;
	}

	@Transient
	public List<Task> getActiveTasks() {
		return getActiveTasks(false);
	}

	/**
	 * Returns a list with all currently active tasks of a Patient
	 * 
	 * @return
	 */
	@Transient
	public List<Task> getActiveTasks(boolean activeOnly) {
		return getTasks().stream().filter(p -> p.isActiveOrActionPending(activeOnly)).collect(Collectors.toList());
	}

	@Transient
	public boolean hasActiveTasks() {
		return hasActiveTasks(false);
	}

	/**
	 * Returns true if at least one task is marked as active
	 * 
	 * @param patient
	 * @return
	 */
	@Transient
	public boolean hasActiveTasks(boolean activeOnly) {
		return getTasks().stream().anyMatch(p -> p.isActiveOrActionPending(activeOnly));
	}

	/**
	 * Returns a list with tasks which are not active
	 * 
	 * @return
	 */
	@Transient
	public List<Task> getNoneActiveTasks() {
		return getTasks().stream().filter(p -> !p.isActiveOrActionPending()).collect(Collectors.toList());
	}

	/**
	 * Returns true if at least one task is not marked as active
	 * 
	 * @param patient
	 * @return
	 */
	@Transient
	public boolean hasNoneActiveTasks() {
		return getTasks().stream().anyMatch(p -> !p.isActiveOrActionPending());
	}
	
	/********************************************************
	 * Transient Methods
	 ********************************************************/
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Override
	@Id
	@GeneratedValue(generator = "patient_sequencegenerator")
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

	@Column
	public String getPiz() {
		return piz;
	}

	public void setPiz(String piz) {
		this.piz = piz;
	}

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id DESC")
	public List<Task> getTasks() {
		if (tasks == null)
			tasks = new ArrayList<Task>();
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@NotAudited
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Basic
	public boolean isExternalPatient() {
		return externalPatient;
	}

	public void setExternalPatient(boolean externalPatient) {
		this.externalPatient = externalPatient;
	}

	@Column
	@Override
	public long getCreationDate() {
		return creationDate;
	}

	@Override
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Column
	public String getInsurance() {
		return insurance;
	}

	public void setInsurance(String insurance) {
		this.insurance = insurance;
	}

	public boolean isPrivateInsurance() {
		return privateInsurance;
	}

	public void setPrivateInsurance(boolean privateInsurance) {
		this.privateInsurance = privateInsurance;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	public List<PDFContainer> getAttachedPdfs() {
		if (attachedPdfs == null)
			attachedPdfs = new ArrayList<PDFContainer>();
		return attachedPdfs;
	}

	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@Transient
	@Override
	public Patient getPatient() {
		return this;
	}

	@Override
	public boolean isArchived() {
		return archived;
	}

	@Override
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	@Transient
	@Override
	public String getTextIdentifier() {
		return null;
	}

	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return null;
	}

	@Transient
	@Override
	public Patient getParent() {
		return null;
	}

	@Override
	public void setParent(Patient parent) {
	}

	@Override
	@Transient
	public Task getTask() {
		// TODO Auto-generated method stub
		return null;
	}

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return "Patient-Name: " + getPerson().getFullName() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/

	@Override
	@Transient
	public String getDatalistIdentifier() {
		return "interface.hasDataList.patient";
	}
	
	
}
