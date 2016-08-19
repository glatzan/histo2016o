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
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.config.HistoSettings;
import org.histo.util.TimeUtil;

import histo.model.util.DiagnosisStatus;
import histo.model.util.StainingStatus;
import histo.model.util.StainingTreeParent;

@Entity
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
public class Sample implements StainingTreeParent<Task>, StainingStatus, DiagnosisStatus {

	private long id;

	/**
	 * Parent of this sample.
	 */
	private Task parent;

	/**
	 * Sample ID as string
	 */
	private String sampleID;

	/**
	 * Date of sample creation
	 */
	private Date generationDate;

	/**
	 * Number of the staining batch,
	 */
	private boolean reStainingPhase;

	/**
	 * block number
	 */
	private int blockNumber = 0;

	/**
	 * blocks array
	 */
	private List<Block> blocks;

	/**
	 * Number increases with every new diagnosis
	 */
	private int diagnosisNumber;

	/**
	 * All diagnoses of this sample
	 */
	private List<Diagnosis> diagnoses;

	/**
	 * Wenn archived true ist, wird dieser sample nicht mehr angezeigt
	 */
	private boolean archived;

	public void incrementBlockNumber() {
		this.blockNumber++;
	}

	public void decrementBlockNumber() {
		this.blockNumber--;
	}

	public void addDiagnosis(Diagnosis diagnosis) {
		getDiagnoses().add(diagnosis);
	}

	public void incrementDiagnosisNumber() {
		this.diagnosisNumber++;
	}

	public void decrementDiagnosisNumber() {
		this.diagnosisNumber--;
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

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("blockID ASC")
	public List<Block> getBlocks() {
		if (blocks == null)
			blocks = new ArrayList<Block>();
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	@OneToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	public List<Diagnosis> getDiagnoses() {
		if (diagnoses == null)
			diagnoses = new ArrayList<>();
		return diagnoses;
	}

	public void setDiagnoses(List<Diagnosis> diagnoses) {
		this.diagnoses = diagnoses;
	}

	@Basic
	public String getSampleID() {
		return sampleID;
	}

	public void setSampleID(String sampleID) {
		this.sampleID = sampleID;
	}

	@Basic
	public Date getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(Date generationDate) {
		this.generationDate = generationDate;
	}

	@Basic
	public int getDiagnosisNumber() {
		return diagnosisNumber;
	}

	public void setDiagnosisNumber(int diagnosisNumber) {
		this.diagnosisNumber = diagnosisNumber;
	}

	@Basic
	public boolean isReStainingPhase() {
		return reStainingPhase;
	}

	public void setReStainingPhase(boolean reStainingPhase) {
		this.reStainingPhase = reStainingPhase;
	}

	@Basic
	public int getBlockNumber() {
		return blockNumber;
	}

	public void setBlockNumber(int blockNumber) {
		this.blockNumber = blockNumber;
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
		for (Diagnosis diagnosis : getDiagnoses()) {
			if (!diagnosis.isFinalized())
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
		if (!isDiagnosisPerformed() && getDiagnosisNumber() == 1)
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn mindestens eine Dinagnose nicht finalisiert wurde.
	 */
	@Override
	@Transient
	public boolean isReDiagnosisNeeded() {
		if (!isDiagnosisPerformed() && getDiagnosisNumber() > 1)
			return true;
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
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wurde
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(generationDate, new Date(System.currentTimeMillis())))
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wrude
	 */
	@Override
	@Transient
	public boolean isStainingPerformed() {

		boolean found = false;
		for (Block block : getBlocks()) {

			if (block.isArchived())
				continue;

			if (!block.isStainingPerformed())
				return false;
			else
				found = true;
		}

		return found;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht und die Batchnumber
	 * == 0 ist.
	 */
	@Override
	@Transient
	public boolean isStainingNeeded() {
		if (!isStainingPerformed() && !isReStainingPhase())
			return true;
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
		if (!isStainingPerformed() && isReStainingPhase())
			return true;
		return false;
	}

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@ManyToOne(targetEntity = Task.class)
	public Task getParent() {
		return parent;
	}

	public void setParent(Task parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Setzt alle Kinder
	 */
	@Basic
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt alle Kinder
		for (Block block : getBlocks()) {
			block.setArchived(archived);
		}
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt die SampleID als
	 * identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getSampleID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zurück
	 */
	@Transient
	@Override
	public String getArchiveDialog() {
		return HistoSettings.dialog(HistoSettings.DIALOG_ARCHIV_SAMPLE);
	}
	/******************************************************** ArchiveAble ********************************************************/
}
