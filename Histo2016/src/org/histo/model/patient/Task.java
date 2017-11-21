package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.Eye;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.TaskPriority;
import org.histo.config.hibernate.RootAware;
import org.histo.model.Accounting;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.ui.StainingTableChooser;
import org.histo.ui.task.TaskStatus;
import org.histo.util.TimeUtil;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "task_sequencegenerator", sequenceName = "task_sequence")
@Getter
@Setter
public class Task implements Parent<Patient>, LogAble, PatientRollbackAble, HasDataList, HasID, RootAware<Patient> {

	private static Logger logger = Logger.getLogger("org.histo");

	@Id
	@GeneratedValue(generator = "task_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * Generated Task ID as String
	 */
	@Column(length = 6)
	private String taskID = "";

	/**
	 * The Patient of the task;
	 */
	@ManyToOne(targetEntity = Patient.class)
	private Patient parent;

	/**
	 * If true the program will provide default names for samples and blocks
	 */
	@Column
	private boolean useAutoNomenclature;

	/**
	 * Date of creation
	 */
	@Column
	private long creationDate = 0;

	/**
	 * The date of the sugery
	 */
	@Column
	private long dateOfSugery = 0;

	/**
	 * Date of reception of the first material
	 */
	@Column
	private long dateOfReceipt = 0;

	/**
	 * Priority of the task
	 */
	@Enumerated(EnumType.ORDINAL)
	private TaskPriority taskPriority;

	/**
	 * The dueDate
	 */
	@Column
	private long dueDate = 0;

	/**
	 * Station�r/ambulant/Extern
	 */
	@Column
	private byte typeOfOperation;

	/**
	 * Details of the case
	 */
	@Column(columnDefinition = "VARCHAR")
	private String caseHistory = "";

	/**
	 * Ward of the patient
	 */
	@Column
	private String ward = "";

	/**
	 * Ey of the samples right/left/both
	 */
	@Enumerated(EnumType.STRING)
	private Eye eye = Eye.UNKNOWN;

	/**
	 * date of staining completion
	 */
	@Column
	private long stainingCompletionDate = 0;

	/**
	 * Date of diagnosis finalization
	 */
	@Column
	private long diagnosisCompletionDate = 0;

	/**
	 * The date of the completion of the notificaiton.
	 */
	@Column
	private long notificationCompletionDate = 0;

	/**
	 * The date of the finalization.
	 */
	@Column
	private long finalizationDate = 0;

	/**
	 * False if the task can't be edited
	 */
	@Column
	private boolean editable = true;

	/**
	 * True if the task can't is completed
	 */
	@Column
	// TODO is this needeD?
	private boolean finalized;

	/**
	 * Liste aller Personen die �ber die Diangose informiert werden sollen.
	 */
	// TODO fetch lazy
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "task")
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@NotAudited
	private List<AssociatedContact> contacts = new ArrayList<AssociatedContact>();

	/**
	 * List with all samples
	 */
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	private List<Sample> samples = new ArrayList<Sample>();

	/**
	 * Element containg all diangnoses
	 */
	@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private DiagnosisContainer diagnosisContainer;

	/**
	 * Generated PDFs of this task, lazy
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	private List<PDFContainer> attachedPdfs = new ArrayList<PDFContainer>();

	/**
	 * List of all councils of this task, lazy
	 */
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "task")
	@OrderBy("dateOfRequest DESC")
	@Fetch(value = FetchMode.SUBSELECT)
	private List<Council> councils = new ArrayList<Council>();

	/**
	 * List of all favorite Lists in which the task is listed
	 */
	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@NotAudited
	private List<FavouriteList> favouriteLists;

	@OneToOne(fetch = FetchType.LAZY)
	private Accounting accounting;

	/********************************************************
	 * Transient Variables
	 ********************************************************/
	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	@Transient
	private ArrayList<StainingTableChooser<?>> stainingTableRows;

	/**
	 * If set to true, this task is shown in the navigation column on the left
	 * hand side, however there are actions to perform or not.
	 */
	@Transient
	private boolean active;

	/**
	 * Contains static list entries for the gui, improves reload speed
	 */
	@Transient
	private TaskStatus taskStatus;

	/********************************************************
	 * Transient Variables
	 ********************************************************/

	public Task() {
	}

	/**
	 * Initializes a task with important values.
	 * 
	 * @param parent
	 */
	public Task(Patient parent) {

		long currentDay = TimeUtil.setDayBeginning(System.currentTimeMillis());
		setCreationDate(currentDay);
		setDateOfReceipt(currentDay);
		setDueDate(currentDay);
		setDateOfSugery(currentDay);

		// 20xx -2000 = tasknumber
		setParent(parent);
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	@Transient
	public void updateAllNames() {
		updateAllNames(useAutoNomenclature, false);
	}

	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		getSamples().stream().forEach(p -> p.updateAllNames(useAutoNomenclature, ignoreManuallyNamedItems));
	}

	@Transient
	public AssociatedContact getPrimarySurgeon() {
		return getPrimaryContact(ContactRole.SURGEON);
	}

	@Transient
	public AssociatedContact getPrimaryPrivatePhysician() {
		return getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
	}

	/**
	 * Returns a associatedContact marked als primary with the given role.
	 * 
	 * @param contactRole
	 * @return
	 */
	@Transient
	public AssociatedContact getPrimaryContact(ContactRole contactRole) {
		for (AssociatedContact associatedContact : contacts) {
			if (associatedContact.getRole() == contactRole)
				return associatedContact;
		}

		return null;
	}

	/**
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 */
	@Transient
	public final void generateSlideGuiList() {
		generateSlideGuiList(false);
	}

	/**
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 * 
	 * @param showArchived
	 */
	@Transient
	public final void generateSlideGuiList(boolean showArchived) {
		if (getStainingTableRows() == null)
			setStainingTableRows(new ArrayList<>());
		else
			getStainingTableRows().clear();

		boolean even = false;

		for (Sample sample : getSamples()) {
			// skips archived tasks

			StainingTableChooser<Sample> sampleChooser = new StainingTableChooser<Sample>(sample, even);
			getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// skips archived blocks

				StainingTableChooser<Block> blockChooser = new StainingTableChooser<Block>(block, even);
				getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Slide staining : block.getSlides()) {
					// skips archived sliedes

					StainingTableChooser<Slide> stainingChooser = new StainingTableChooser<Slide>(staining, even);
					getStainingTableRows().add(stainingChooser);
					blockChooser.addChild(stainingChooser);
				}
			}

			even = !even;
		}
	}

	@Transient
	public void generateTaskStatus() {
		if (getTaskStatus() == null)
			setTaskStatus(new TaskStatus(this));
		else
			getTaskStatus().updateStatus();
	}

	@Transient
	public boolean isListedInFavouriteList(PredefinedFavouriteList... predefinedFavouriteLists) {
		long[] arr = new long[predefinedFavouriteLists.length];
		int i = 0;
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			arr[i] = predefinedFavouriteList.getId();
			i++;
		}

		return isListedInFavouriteList(arr);
	}

	@Transient
	public boolean isListedInFavouriteList(long... idArr) {
		for (long id : idArr) {
			if (getFavouriteLists().stream().anyMatch(p -> p.getId() == id))
				return true;
		}
		return false;
	}

	@Transient
	public boolean isListedInFavouriteList(FavouriteList... favouriteLists) {
		long[] arr = new long[favouriteLists.length];
		int i = 0;
		for (FavouriteList favouriteList : favouriteLists) {
			arr[i] = favouriteList.getId();
			i++;
		}

		return isListedInFavouriteList(arr);
	}

	@Override
	@Transient
	public String toString() {
		return "ID: " + getId() + ", Task ID: " + getTaskID();
	}

	@Transient
	public boolean isActiveOrActionPending() {
		return isActiveOrActionPending(false);
	}

	/**
	 * Returns true if the task is marked as active or an action is pending. If
	 * activeOnly is true only the active attribute of the task will be
	 * evaluated.
	 * 
	 * @param task
	 * @return
	 */
	@Transient
	public boolean isActiveOrActionPending(boolean activeOnly) {
		if (activeOnly)
			return isActive();

		if (isActive())
			return true;

		if (isListedInFavouriteList(PredefinedFavouriteList.StainingList, PredefinedFavouriteList.ReStainingList,
				PredefinedFavouriteList.StayInStainingList, PredefinedFavouriteList.DiagnosisList,
				PredefinedFavouriteList.ReDiagnosisList, PredefinedFavouriteList.StayInDiagnosisList,
				PredefinedFavouriteList.NotificationList, PredefinedFavouriteList.StayInNotificationList))
			return true;

		return false;
	}

	@Transient
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Task && ((Task) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	@Transient
	public boolean containsContact(Person person) {
		if (getContacts() != null)
			return getContacts().stream().anyMatch(p -> p.getPerson().equals(person));
		return false;
	}

	@Transient
	public boolean containsContact(AssociatedContact associatedContact) {
		if (getContacts() != null)
			return getContacts().stream().anyMatch(p -> p.equals(associatedContact));
		return false;
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/

	@Transient
	public Date getCreationDateAsDate() {
		return new Date(getCreationDate());
	}

	public void setCreationDateAsDate(Date date) {
		setCreationDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfSugeryAsDate() {
		return new Date(getDateOfSugery());
	}

	public void setDateOfSugeryAsDate(Date date) {
		setDateOfSugery(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfReceiptAsDate() {
		return new Date(getDateOfReceipt());
	}

	public void setDateOfReceiptAsDate(Date date) {
		setDateOfReceipt(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDueDateAsDate() {
		return new Date(getDueDate());
	}

	public void setDueDateAsDate(Date date) {
		setDueDate(TimeUtil.setDayBeginning(date).getTime());
	}

	/**
	 * Returns true if priority is set to TaskPriority.Time
	 */
	@Transient
	public boolean isDueDateSelected() {
		if (getTaskPriority() == TaskPriority.TIME)
			return true;
		return false;
	}

	/**
	 * Sets if the given parameter is true TaskPriority.Time, if false
	 * TaskPriority.NONE
	 * 
	 * @param dueDate
	 */
	public void setDueDateSelected(boolean dueDate) {
		if (dueDate)
			setTaskPriority(TaskPriority.TIME);
		else
			setTaskPriority(TaskPriority.NONE);
	}

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent();
	}

	/**
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return this;
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return "Task-ID: " + getTaskID() + " (" + getId() + ")";
	}

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/

	@Override
	@Transient
	public String getDatalistIdentifier() {
		return "interface.hasDataList.task";
	}

	@Override
	@Transient
	public Patient root() {
		return parent;
	}
}
