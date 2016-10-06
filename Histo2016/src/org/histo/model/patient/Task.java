package org.histo.model.patient;

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
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.HistoSettings;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.util.DiagnosisStatus;
import org.histo.model.util.LogAble;
import org.histo.model.util.StainingStatus;
import org.histo.model.util.TaskTree;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
public class Task implements TaskTree<Patient>, StainingStatus, DiagnosisStatus, LogAble {

	public static final int TAB_DIAGNOSIS = 0;
	public static final int TAB_STAINIG = 1;

	public static final byte EYE_RIGHT = 0;
	public static final byte EYE_LEFT = 1;

	private long id;

	private long version;

	/**
	 * Generated Task ID as String
	 */
	private String taskID = "";

	/**
	 * The Patient of the task;
	 */
	private Patient parent;

	/**
	 * Date of creation
	 */
	private long creationDate = 0;

	/**
	 * The date of the sugery
	 */
	private long dateOfSugery = 0 ;
	
	/**
	 * Date of reception of the first material
	 */
	private long dateOfReceipt = 0;

	/**
	 * The dueDate
	 */
	private long dueDate = 0;
	
	/**
	 * If a dueDate is given
	 */
	private boolean dueDateSelected = false;

	/**
	 * Liste aller Personen die �ber die Diangose informiert werden sollen.
	 */
	private List<Contact> contacts;

	/**
	 * Station�r/ambulant/Extern
	 */
	private byte typeOfOperation;

	/**
	 * Commentary TODO: is used=
	 */
	private String commentray  = "";

	/**
	 * Details of the case
	 */
	private String caseHistory  = "";

	/**
	 * Ward of the patient
	 */
	private String ward  = "";

	/**
	 * Ey of the samples right/left/both
	 */
	private byte eye = EYE_RIGHT;

	/**
	 * Sample count, is incemented with every new sample
	 */
	private int sampleNumer = 1;

	/**
	 * List with all samples
	 */
	private List<Sample> samples;

	/**
	 * Der Task ist archiviert und wird nicht mehr angezeigt wenn true
	 */
	private boolean archived = false;

	/**
	 * all stainings completed
	 */
	private boolean stainingCompleted = false;

	/**
	 * date of staining completion
	 */
	private long stainingCompletionDate = 0;

	/**
	 * True if every diagnosis is finalized
	 */
	private boolean diagnosisCompleted = false;

	/**
	 * Date of diagnosis finalization
	 */
	private long diagnosisCompletionDate = 0;

	/**
	 * Generated PDFs of this task
	 */
	private List<PDFContainer> pdfs;

	/******************************************************** Transient ********************************************************/
	/**
	 * Die Ausgew�hlte Probe
	 */
	private Sample selectedSample;

	/**
	 * Der Index des TabViews 0 = Diangosen Tab 1 = Objekttr�ger Tab
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

	/**
	 * True if lazy initialision was successful.
	 */
	private boolean lazyInitialized;

	/******************************************************** Transient ********************************************************/
	public Task() {
	}

	public Task(Patient parent) {
		this.parent = parent;
	}

	/**
	 * Erstellt eine Liste aller in diesem Task enthaltenen
	 * Proben/Bl�cke/F�rbungen. Standardm��ig werden archivierte Elemente nicht
	 * angezegit.
	 */
	public void generateStainingGuiList() {
		generateStainingGuiList(false);
	}

	/**
	 * Erstellt eine Liste aller in diesem Task enthaltenen
	 * Proben/Bl�cke/F�rbungen. Es kann ausgew�hlt werden, ob archivierte
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
			// �berspringt archivierte Proben
			if (sample.isArchived() && !showArchived)
				continue;

			StainingTableChooser sampleChooser = new StainingTableChooser(sample, even);
			getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// �berspringt archivierte Bl�cke
				if (block.isArchived() && !showArchived)
					continue;

				StainingTableChooser blockChooser = new StainingTableChooser(block, even);
				getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Slide staining : block.getSlides()) {
					// �berspringt archivierte Objekttr�ger
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
	 * Updated den Tabindex wenn ein andere Tab (Diagnose oder F�rbung) in der
	 * Gui ausgew�hlt wurde
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

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Basic
	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	@Basic
	public long getDateOfReceipt() {
		return dateOfReceipt;
	}

	public void setDateOfReceipt(long dateOfReceipt) {
		this.dateOfReceipt = dateOfReceipt;
	}

	@Transient     
	public Date getDateOfReceiptAsDate(){
		return new Date(getDateOfReceipt());
	}
	
	public void setDateOfReceiptAsDate(Date date){
		setDateOfReceipt(TimeUtil.setDayBeginning(date).getTime());
	}
	
	@Basic
	public long getDueDate() {
		return dueDate;
	}

	public void setDueDate(long dueDate) {
		this.dueDate = dueDate;
	}
	
	@Transient
	public Date getDueDateAsDate(){
		return new Date(getDueDate());
	}
	
	public void setDueDateAsDate(Date date){
		setDueDate(TimeUtil.setDayBeginning(date).getTime());
	}
	
	@Basic
	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
	
	@Transient
	public Date getCreationDateAsDate(){
		return new Date(getCreationDate());
	}
	
	public void setCreationDateAsDate(Date date){
		setCreationDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Basic
	public long getDateOfSugery() {
		return dateOfSugery;
	}

	public void setDateOfSugery(long dateOfSugery) {
		this.dateOfSugery = dateOfSugery;
	}
	
	@Transient
	public Date getDateOfSugeryAsDate(){
		return new Date(getDateOfSugery());
	}
	
	public void setDateOfSugeryAsDate(Date date){
		setDateOfSugery(TimeUtil.setDayBeginning(date).getTime());
	}
	
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@NotAudited
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

	@Basic
	public byte getEye() {
		return eye;
	}

	public void setEye(byte eye) {
		this.eye = eye;
	}

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL , fetch = FetchType.EAGER)
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

	@Basic
	public int getSampleNumer() {
		return sampleNumer;
	}

	public void setSampleNumer(int sampleNumer) {
		this.sampleNumer = sampleNumer;
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
	public long getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(long stainingCompletionDate) {
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
	public long getDiagnosisCompletionDate() {
		return diagnosisCompletionDate;
	}

	public void setDiagnosisCompletionDate(long diagnosisCompletionDate) {
		this.diagnosisCompletionDate = diagnosisCompletionDate;
	}

	public String getWard() {
		return ward;
	}

	public void setWard(String ward) {
		this.ward = ward;
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

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public List<PDFContainer> getPdfs() {
		if (pdfs == null)
			pdfs = new ArrayList<PDFContainer>();

		return pdfs;
	}

	public void setPdfs(List<PDFContainer> pdfs) {
		this.pdfs = pdfs;
	}
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zur�ck wenn alle Diagnosen finalisiert wurden.
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
	 * �berschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zur�ck wenn mindestens eine Dinagnose nicht finalisiert wurde.
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
	 * �berschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zur�ck wenn mindestens eine Dinagnose nicht finalisiert wurde.
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
	 * �berschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zur�ck, wenn die Aufgabe am heutigen Tag erstellt wurde
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(creationDate, System.currentTimeMillis()))
			return true;
		return false;
	}

	/**
	 * �berschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zur�ck, wenn die Probe am heutigen Tag erstellt wrude
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
	 * �berschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zr�ck wenn mindestens eine F�rbung aussteht und die Batchnumber
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
	 * �berschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zr�ck wenn mindestens eine F�rbung aussteht und die Batchnumber
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
	@ManyToOne(targetEntity = Patient.class)
	public Patient getParent() {
		return parent;
	}

	public void setParent(Patient parent) {
		this.parent = parent;
	}

	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Setzt alle Kinder
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt Kinder
		for (Sample sample : getSamples()) {
			sample.setArchived(archived);
		}
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt die TaskID als
	 * identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getTaskID();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zur�ck
	 */
	@Transient
	@Override
	public String getArchiveDialog() {
		return HistoSettings.DIALOG_ARCHIV_TASK;
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/

}