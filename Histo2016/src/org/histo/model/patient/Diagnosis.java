package org.histo.model.patient;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.model.DiagnosisPreset;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.GsonAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;

import com.google.gson.annotations.Expose;

/**
 * Diagnose NAchbefundung Revision
 * 
 * @author andi
 *
 */
@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "diagnosis_sequencegenerator", sequenceName = "diagnosis_sequence")
public class Diagnosis implements Parent<DiagnosisRevision>, GsonAble, LogAble, ArchivAble {

	private long id;

	private long version;

	/**
	 * Parent of the diagnosis, sample bject
	 */
	private DiagnosisRevision parent;

	/**
	 * Nothing can be deleted. Mark deleted entities as achieved.
	 */
	private boolean archived;

	/**
	 * Date of diagnosis creation.
	 */
	private long generationDate;

	/**
	 * Date of diagnosis finalization.
	 */
	private long finalizedDate;

	/**
	 * True if finalized.
	 */
	private boolean finalized;

	/**
	 * Name of the diagnosis.
	 */
	private String name = "";

	/**
	 * Diagnosis as short string.
	 */
	private String diagnosis = "";

	/**
	 * True if finding is malign.
	 */
	private boolean malign;

	/**
	 * ICD10 Number of this diagnosis
	 */
	private String icd10 = "";

	/**
	 * Protoype used for this diagnosis.
	 */
	private DiagnosisPreset diagnosisPreset;

	/**
	 * Associated sample
	 */
	private Sample sample;

	/**
	 * Standard constructor
	 */
	public Diagnosis() {
	}

	/**
	 * Constructor for adding a sample to the diagnosis
	 * 
	 * @param sample
	 */
	public Diagnosis(DiagnosisRevision revision, Sample sample) {
		this.sample = sample;
		setGenerationDate(System.currentTimeMillis());
		setParent(revision);
		
		revision.getDiagnoses().add(this);

		// setName(getDiagnosisName(sample, diagnosis, resourceBundle));
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "diagnosis_sequencegenerator")
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

	@Column(columnDefinition = "text")
	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
	}

	@Basic
	public long getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(long generationDate) {
		this.generationDate = generationDate;
	}

	@Basic
	public long getFinalizedDate() {
		return finalizedDate;
	}

	public void setFinalizedDate(long finalizedDate) {
		this.finalizedDate = finalizedDate;
	}

	@Basic
	public boolean isFinalized() {
		return finalized;
	}

	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}

	@Basic
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Basic
	public boolean isMalign() {
		return malign;
	}

	public void setMalign(boolean malign) {
		this.malign = malign;
	}

	public String getIcd10() {
		return icd10;
	}

	public void setIcd10(String icd10) {
		this.icd10 = icd10;
	}

	@OneToOne
	@NotAudited
	public DiagnosisPreset getDiagnosisPrototype() {
		return diagnosisPreset;
	}

	public void setDiagnosisPrototype(DiagnosisPreset diagnosisPreset) {
		this.diagnosisPreset = diagnosisPreset;
	}

	@OneToOne
	public Sample getSample() {
		return sample;
	}

	public void setSample(Sample sample) {
		this.sample = sample;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	/**
	 * Returns the parent of this diagnosis. Overwrites method from
	 * StainingTreeParent.
	 */
	@ManyToOne
	public DiagnosisRevision getParent() {
		return parent;
	}

	public void setParent(DiagnosisRevision parent) {
		this.parent = parent;
	}

	/**
	 * Returns the patient of this diagnosis. Overwrites method from
	 * StainingTreeParent.
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/
	/**
	 * True if deleted. Overwrites method from StainingTreeParent.
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * If set true the diagnosis isn't displayed anymore. Overwrites method from
	 * StainingTreeParent.
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	/**
	 * TODO
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getDiagnosis();
	}

	/**
	 * TODO
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return null;
	}

	/********************************************************
	 * Interface ArchiveAble
	 ********************************************************/

	/********************************************************
	 * Transient
	 ********************************************************/
	/**
	 * Copies the parameters of a diagnosisPreset to this entity.
	 * 
	 * @param diagnosisPreset
	 */
	@Transient
	public void updateDiagnosisWithPrest(DiagnosisPreset diagnosisPreset) {
		setDiagnosis(diagnosisPreset.getDiagnosisText());
		setMalign(diagnosisPreset.isMalign());
		setIcd10(diagnosisPreset.getIcd10());
	}

	/********************************************************
	 * Transient
	 ********************************************************/
}
