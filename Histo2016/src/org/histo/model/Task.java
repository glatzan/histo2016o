package org.histo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.config.HistoSettings;
import org.histo.model.util.DiagnosisStatus;
import org.histo.model.util.StainingStatus;
import org.histo.model.util.StainingTreeParent;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

@Entity
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
public class Task implements StainingTreeParent<Patient>, StainingStatus, DiagnosisStatus {

	public static final int TAB_DIAGNOSIS = 0;
	public static final int TAB_STAINIG = 1;

	public static final byte EYE_RIGHT = 0;
	public static final byte EYE_LEFT = 1;

	private long id;

	/**
	 * Generated Task ID as String
	 */
	private String taskID;

	/**
	 * The Patient of the task;
	 */
	private Patient parent;

	/**
	 * Date of creation
	 */
	private Date creationDate;

	/**
	 * Date of reception of the first material
	 */
	private Date dateOfReceipt;

	/**
	 * If a dueDate is given
	 */
	private boolean dueDateSelected;

	/**
	 * The dueDate
	 */
	private Date dueDate;

	/**
	 * Liste aller Personen die über die Diangose informiert werden sollen.
	 */
	private List<Contact> contacts;

	/**
	 * Stationär/ambulant/Extern
	 */
	private byte typeOfOperation;

	/**
	 * Commentary TODO: is used=
	 */
	private String commentray;

	/**
	 * Details of the case
	 */
	private String caseHistory;

	/**
	 * Ey of the samples right/left/both
	 */
	private byte eye;

	/**
	 * Sample count, is incemented with every new sample
	 */
	private int sampleNumer = 1;

	/**
	 * List with all samples
	 */
	private List<Sample> samples;

	/**
	 * Material Type of sample, is used for default stainings within new blocks
	 */
	private StainingPrototypeList typeOfMaterial;

	/**
	 * Material name is first initialized with the name of the typeOfMaterial.
	 * Can be later changed.
	 */
	private String materialName;

	/**
	 * Der Task ist archiviert und wird nicht mehr angezeigt wenn true
	 */
	private boolean archived;

	/**
	 * all stainings completed
	 */
	private boolean stainingCompleted;

	/**
	 * date of staining completion
	 */
	private Date stainingCompletionDate;

	/**
	 * True if every diagnosis is finalized
	 */
	private boolean diagnosisCompleted;

	/**
	 * Date of diagnosis finalization
	 */
	private Date diagnosisCompletionDate;
	/******************************************************** Transient ********************************************************/
	/**
	 * Die Ausgewählte Probe
	 */
	private Sample selectedSample;

	/**
	 * Der Index des TabViews 0 = Diangosen Tab 1 = Objektträger Tab
	 */
	private int tabIndex;

	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	private ArrayList<StainingTableChooser> stainingTableChoosers;

	/**
	 * If set to true, this task is shown in the navigation column on the left
	 * hand side, however there are actions to perform or not.
	 */
	private boolean currentlyActive;

	/******************************************************** Transient ********************************************************/
	public Task() {
	}

	public Task(Patient parent) {
		this.parent = parent;
	}

	/**
	 * Erstellt eine Liste aller in diesem Task enthaltenen
	 * Proben/Blöcke/Färbungen. Standardmäßig werden archivierte Elemente nicht
	 * angezegit.
	 */
	public void generateStainingGuiList() {
		generateStainingGuiList(false);
	}

	/**
	 * Erstellt eine Liste aller in diesem Task enthaltenen
	 * Proben/Blöcke/Färbungen. Es kann ausgewählt werden, ob archivierte
	 * Element angezeigt werden oder nicht.
	 * 
	 * @param showArchived
	 */
	public void generateStainingGuiList(boolean showArchived) {
		if (getStainingTableRows() == null)
			setStainingTableRows(new ArrayList<>());
		else
			getStainingTableRows().clear();

		boolean even = false;

		for (Sample sample : samples) {
			// überspringt archivierte Proben
			if (sample.isArchived() && !showArchived)
				continue;

			StainingTableChooser sampleChooser = new StainingTableChooser(sample, even);
			getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// überspringt archivierte Blöcke
				if (block.isArchived() && !showArchived)
					continue;

				StainingTableChooser blockChooser = new StainingTableChooser(block, even);
				getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Staining staining : block.getStainings()) {
					// überspringt archivierte Objektträger
					if (staining.isArchived() && !showArchived)
						continue;

					StainingTableChooser stainingChooser = new StainingTableChooser(staining, even);
					getStainingTableRows().add(stainingChooser);
					blockChooser.addChild(stainingChooser);
				}
			}
			even = !even;
		}
	}

	/**
	 * Updated den Tabindex wenn ein andere Tab (Diagnose oder Färbung) in der
	 * Gui ausgewählt wurde
	 * 
	 * @param event
	 */
	@Transient
	public void tabIndexListener(TabChangeEvent event) {
		setTabIndex(((TabView) event.getSource()).getIndex());
	}

	public void incrementSampleNumber() {
		this.sampleNumer++;
	}

	public void decrementSmapleNumber() {
		this.sampleNumer--;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "sample_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column
	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	@Column
	public Date getDateOfReceipt() {
		return dateOfReceipt;
	}

	public void setDateOfReceipt(Date dateOfReceipt) {
		this.dateOfReceipt = dateOfReceipt;
	}

	@Column
	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	public List<Contact> getContacts() {
		if (contacts == null)
			contacts = new ArrayList<Contact>();
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	@Column
	public byte getTypeOfOperation() {
		return typeOfOperation;
	}

	public void setTypeOfOperation(byte typeOfOperation) {
		this.typeOfOperation = typeOfOperation;
	}

	@Column
	public String getCommentray() {
		return commentray;
	}

	public void setCommentray(String commentray) {
		this.commentray = commentray;
	}

	@Column
	public String getCaseHistory() {
		return caseHistory;
	}

	public void setCaseHistory(String caseHistory) {
		this.caseHistory = caseHistory;
	}

	@Column
	public byte getEye() {
		return eye;
	}

	public void setEye(byte eye) {
		this.eye = eye;
	}

	@Basic
	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, targetEntity = Sample.class, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	public List<Sample> getSamples() {
		if (samples == null)
			samples = new ArrayList<Sample>();
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}

	@OneToOne
	public StainingPrototypeList getTypeOfMaterial() {
		return typeOfMaterial;
	}

	public void setTypeOfMaterial(StainingPrototypeList typeOfMaterial) {
		this.typeOfMaterial = typeOfMaterial;
	}

	@Basic
	public int getSampleNumer() {
		return sampleNumer;
	}

	public void setSampleNumer(int sampleNumer) {
		this.sampleNumer = sampleNumer;
	}

	@Basic
	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	@Basic
	public boolean isDueDateSelected() {
		return dueDateSelected;
	}

	public void setDueDateSelected(boolean dueDateSelected) {
		this.dueDateSelected = dueDateSelected;
	}

	@Basic
	public boolean isStainingCompleted() {
		return stainingCompleted;
	}

	public void setStainingCompleted(boolean stainingCompleted) {
		this.stainingCompleted = stainingCompleted;
	}

	@Basic
	public Date getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(Date stainingCompletionDate) {
		this.stainingCompletionDate = stainingCompletionDate;
	}

	@Basic
	public boolean isDiagnosisCompleted() {
		return diagnosisCompleted;
	}

	public void setDiagnosisCompleted(boolean diagnosisCompleted) {
		this.diagnosisCompleted = diagnosisCompleted;
	}

	@Basic
	public Date getDiagnosisCompletionDate() {
		return diagnosisCompletionDate;
	}

	public void setDiagnosisCompletionDate(Date diagnosisCompletionDate) {
		this.diagnosisCompletionDate = diagnosisCompletionDate;
	}

	@Transient
	public Sample getSelectedSample() {
		return selectedSample;
	}

	public void setSelectedSample(Sample selectedSample) {
		this.selectedSample = selectedSample;
	}

	@Transient
	public ArrayList<StainingTableChooser> getStainingTableRows() {
		return stainingTableChoosers;
	}

	public void setStainingTableRows(ArrayList<StainingTableChooser> stainingTableChoosers) {
		this.stainingTableChoosers = stainingTableChoosers;
	}

	@Transient
	public int getTabIndex() {
		return tabIndex;
	}

	public void setTabIndex(int tabIndex) {
		this.tabIndex = tabIndex;
	}

	@Transient
	public boolean isCurrentlyActive() {
		return currentlyActive;
	}

	public void setCurrentlyActive(boolean currentlyActive) {
		this.currentlyActive = currentlyActive;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn alle Diagnosen finalisiert wurden.
	 */
	@Override
	@Transient
	public boolean isDiagnosisPerformed() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (!sample.isDiagnosisPerformed())
				return false;
		}
		return true;
	}

	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn mindestens eine Dinagnose nicht finalisiert wurde.
	 */
	@Override
	@Transient
	public boolean isDiagnosisNeeded() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isDiagnosisNeeded())
				return true;
		}
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn mindestens eine Dinagnose nicht finalisiert wurde.
	 */
	@Override
	@Transient
	public boolean isReDiagnosisNeeded() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isReDiagnosisNeeded())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Aufgabe am heutigen Tag erstellt wurde
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(creationDate, new Date(System.currentTimeMillis())))
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wrude
	 */
	@Transient
	@Override
	public boolean isStainingPerformed() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (!sample.isStainingPerformed())
				return false;
		}
		return true;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht und die Batchnumber
	 * == 0 ist.
	 */
	@Override
	@Transient
	public boolean isStainingNeeded() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isStainingNeeded())
				return true;
		}
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht und die Batchnumber
	 * > 0 ist.
	 */
	@Override
	@Transient
	public boolean isReStainingNeeded() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isReStainingNeeded())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@ManyToOne
	public Patient getParent() {
		return parent;
	}

	public void setParent(Patient parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Setzt alle Kinder
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt Kinder
		for (Sample sample : getSamples()) {
			sample.setArchived(archived);
		}
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt die TaskID als
	 * identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getTaskID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zurück
	 */
	@Transient
	@Override
	public String getArchiveDialog() {
		return HistoSettings.dialog(HistoSettings.DIALOG_ARCHIV_TASK);
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
}
