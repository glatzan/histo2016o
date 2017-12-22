package org.histo.model.patient;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
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
import org.histo.util.HistoUtil;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "patient_sequencegenerator", sequenceName = "patient_sequence")
@Getter
@Setter
public class Patient
		implements Parent<Patient>, CreationDate, LogAble, ArchivAble, PatientRollbackAble, HasDataList, HasID {

	@Id
	@GeneratedValue(generator = "patient_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * PIZ
	 */
	@Column
	private String piz = "";

	/**
	 * Insurance of the patient
	 */
	@Column
	private String insurance = "";

	/**
	 * Date of adding to the database
	 */
	@Column
	private long creationDate = 0;

	/**
	 * Person data
	 */
	@OneToOne(cascade = CascadeType.ALL)
	@NotAudited
	private Person person;

	/**
	 * Task for this patient
	 */
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("taskid DESC")
	private List<Task> tasks = new ArrayList<Task>();

	/**
	 * True if patient was added as an external patient.
	 */
	@Column
	private boolean externalPatient = false;

	/**
	 * Pdf attached to this patient, this might be an informed consent
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	private List<PDFContainer> attachedPdfs = new ArrayList<PDFContainer>();
	/**
	 * If true the patient is archived. Thus he won't be displayed.
	 */
	@Column
	private boolean archived = false;

	/**
	 * True if saved in database, false if only in clinic backend
	 */
	@Transient
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
	 * Updates the patient data with a given patient dummy. Returns true if data are
	 * changed. Returns true if data were changed
	 * 
	 * @param patient
	 */
	@Transient
	public final boolean copyIntoObject(Patient patient) {
		boolean change = false;

		if (HistoUtil.isStringDifferent(getPiz(), patient.getPiz())) {
			change = true;
			setPiz(patient.getPiz());
		}

		if (HistoUtil.isStringDifferent(getInsurance(), patient.getInsurance())) {
			change = true;
			setInsurance(getInsurance());
		}

		// update person data if update is true
		
		if (getPerson().isAutoUpdate()) {
			if (HistoUtil.isStringDifferent(getPerson().getTitle(), patient.getPerson().getTitle())) {
				change = true;
				getPerson().setTitle(patient.getPerson().getTitle());
			}

			if (HistoUtil.isStringDifferent(getPerson().getLastName(), patient.getPerson().getLastName())) {
				change = true;
				getPerson().setLastName(patient.getPerson().getLastName());
			}

			if (HistoUtil.isStringDifferent(getPerson().getFirstName(), patient.getPerson().getFirstName())) {
				change = true;
				getPerson().setFirstName(patient.getPerson().getFirstName());
			}

			if (HistoUtil.isStringDifferent(getPerson().getBirthday(), patient.getPerson().getBirthday())) {
				change = true;
				getPerson().setBirthday(patient.getPerson().getBirthday());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getTown(),
					patient.getPerson().getContact().getTown())) {
				change = true;
				getPerson().getContact().setTown(patient.getPerson().getContact().getTown());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getCountry(),
					patient.getPerson().getContact().getCountry())) {
				change = true;
				getPerson().getContact().setCountry(patient.getPerson().getContact().getCountry());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getPostcode(),
					patient.getPerson().getContact().getPostcode())) {
				change = true;
				getPerson().getContact().setPostcode(patient.getPerson().getContact().getPostcode());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getStreet(),
					patient.getPerson().getContact().getStreet())) {
				change = true;
				getPerson().getContact().setStreet(patient.getPerson().getContact().getStreet());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getPhone(),
					patient.getPerson().getContact().getPhone())) {
				change = true;
				getPerson().getContact().setPhone(patient.getPerson().getContact().getPhone());
			}

			if (HistoUtil.isStringDifferent(getPerson().getGender(), patient.getPerson().getGender())) {
				change = true;
				getPerson().setGender(patient.getPerson().getGender());
			}

			if (HistoUtil.isStringDifferent(getPerson().getContact().getEmail(),
					patient.getPerson().getContact().getEmail())) {
				change = true;
				getPerson().getContact().setEmail(patient.getPerson().getContact().getEmail());
			}
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
	@Transient
	public final void copyIntoObject(String json) throws JSONException, CustomNullPatientExcepetion {
		copyIntoObject(new JSONObject(json));
	}

	/**
	 * Updates the patient object with a given json array from the clinic backend
	 *
	 * { "vorname":"Test", "mode":"W", "status":null, "piz":"25201957",
	 * "sonderinfo":"", "iknr":"00190", "kvnr":null, "titel":"Prof. Dr. med.",
	 * "versichertenstatus":" ", "tel":"12-4085", "anschrift": "Gillenweg 4",
	 * "wop":null, "plz":"79110", "name":"Test", "geburtsdatum":"1972-08-22",
	 * "gueltig_bis":null, "krankenkasse":"Wissenschaftliche Unters.",
	 * "versnr":null, "land":"D", "weiblich":"", "ort":"Freiburg", "status2":null }
	 * 
	 * @param patient
	 * @param json
	 * @return
	 * @throws CustomNullPatientExcepetion
	 */
	@Transient
	public final void copyIntoObject(JSONObject obj) throws CustomNullPatientExcepetion {

		// check if not an null patient is returned

		if (obj.optString("name").isEmpty())
			throw new CustomNullPatientExcepetion();

		Person person = getPerson();

		// Person data
		person.setTitle(obj.optString("titel"));
		person.setLastName(obj.optString("name"));

		person.setFirstName(obj.optString("vorname"));

		// parsing date
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
		Date date;
		try {
			date = format.parse(obj.optString("geburtsdatum"));
		} catch (ParseException e) {
			date = new Date();
		}
		person.setBirthday(date);

		person.getContact().setTown(obj.optString("ort"));
		person.getContact().setCountry(obj.optString("land"));
		person.getContact().setPostcode(obj.optString("plz"));
		person.getContact().setStreet(obj.optString("anschrift"));
		person.getContact().setPhone(obj.optString("tel"));

		// 1 equals female, empty equals male
		person.setGender(obj.optString("weiblich").equals("1") ? Person.Gender.FEMALE : Person.Gender.MALE);

		// TODO
		person.getContact().setEmail("");

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
		return getTasks() != null
				? getTasks().stream().filter(p -> p.isActiveOrActionPending(activeOnly)).collect(Collectors.toList())
				: null;
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
		return getTasks() != null ? getTasks().stream().anyMatch(p -> p.isActiveOrActionPending(activeOnly)) : false;
	}

	/**
	 * Returns a list with tasks which are not active
	 * 
	 * @return
	 */
	@Transient
	public List<Task> getNoneActiveTasks() {
		return getTasks() != null
				? getTasks().stream().filter(p -> !p.isActiveOrActionPending()).collect(Collectors.toList())
				: null;
	}

	/**
	 * Returns true if at least one task is not marked as active
	 * 
	 * @param patient
	 * @return
	 */
	@Transient
	public boolean hasNoneActiveTasks() {
		return getTasks() != null ? getTasks().stream().anyMatch(p -> !p.isActiveOrActionPending()) : false;
	}

	/********************************************************
	 * Transient Methods
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@Transient
	@Override
	public Patient getPatient() {
		return this;
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
