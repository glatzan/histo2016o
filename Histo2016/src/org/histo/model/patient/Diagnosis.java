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
import org.histo.config.enums.DiagnosisType;
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
public class Diagnosis implements Parent<Sample>, GsonAble, LogAble, ArchivAble {

	private long id;

	private long version;

	/**
	 * Parent of the diagnosis, sample objekt
	 */
	private Sample parent;

	/**
	 * Nothing can be deleted. Mark deleted entities as achieved.
	 */
	@Expose
	private boolean archived;

	/**
	 * Date of diagnosis creation.
	 */
	@Expose
	private long generationDate;

	/**
	 * Date of diagnosis finalization.
	 */
	@Expose
	private long finalizedDate;

	/**
	 * True if finalized.
	 */
	@Expose
	private boolean finalized;

	/**
	 * Name of the diagnosis.
	 */
	@Expose
	private String name = "";

	/**
	 * Diagnosis as short string.
	 */
	@Expose
	private String diagnosis = "";

	/**
	 * True if finding is malign.
	 */
	@Expose
	private boolean malign;

	/**
	 * ICD10 Number of this diagnosis
	 */
	@Expose
	private String icd10 = "";

	/**
	 * If true the followUp field will be used.
	 */
	@Expose
	private boolean diagnosisRevision;

	/**
	 * Followup field for adding follow up comments.
	 */
	@Expose
	private String diagnosisRevisionText = "";

	/**
	 * Diagnosis type, normal = 0, follow up = 1 , revision = 2.
	 */
	@Expose
	private DiagnosisType type;

	/**
	 * Order in diagnosis array
	 */
	@Expose
	private int diagnosisOrder;

	/**
	 * Protoype used for this diagnosis.
	 */
	private DiagnosisPreset diagnosisPreset;

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

	@Enumerated(EnumType.STRING)
	public DiagnosisType getType() {
		return type;
	}

	public void setType(DiagnosisType type) {
		this.type = type;
	}

	@Basic
	public int getDiagnosisOrder() {
		return diagnosisOrder;
	}

	public void setDiagnosisOrder(int diagnosisOrder) {
		this.diagnosisOrder = diagnosisOrder;
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

	public boolean isDiagnosisRevision() {
		return diagnosisRevision;
	}

	public String getDiagnosisRevisionText() {
		return diagnosisRevisionText;
	}

	@Column(columnDefinition = "text")
	public void setDiagnosisRevision(boolean diagnosisRevision) {
		this.diagnosisRevision = diagnosisRevision;
	}

	public void setDiagnosisRevisionText(String diagnosisRevisionText) {
		this.diagnosisRevisionText = diagnosisRevisionText;
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
	public Sample getParent() {
		return parent;
	}

	public void setParent(Sample parent) {
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
