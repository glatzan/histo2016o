package org.histo.model;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import org.hibernate.envers.RelationTargetAuditMode;
import org.histo.config.HistoSettings;
import org.histo.model.util.GsonAble;
import org.histo.model.util.StainingTreeParent;

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
public class Diagnosis implements StainingTreeParent<Sample>, GsonAble {

	public static final int TYPE_DIAGNOSIS = 0;
	public static final int TYPE_FOLLOW_UP_DIAGNOSIS = 1;
	public static final int TYPE_DIAGNOSIS_REVISION = 2;

	private long id;

	private int version;

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
	private Date generationDate;

	/**
	 * Date of diagnosis finalization.
	 */
	@Expose
	private Date finalizedDate;

	/**
	 * True if finalized.
	 */
	@Expose
	private boolean finalized;

	/**
	 * Name of the diagnosis.
	 */
	@Expose
	private String name;

	/**
	 * Diagnosis as short string.
	 */
	@Expose
	private String diagnosis;

	/**
	 * True if finding is malign.
	 */
	@Expose
	private boolean malign;

	/**
	 * ICD10 Number of this diagnosis
	 */
	@Expose
	private String icd10;
	
	/**
	 * Commentary for internal purpose.
	 */
	@Expose
	private String commentary;

	/**
	 * Diagnosis type, normal = 0, follow up = 1 , revision = 2.
	 */
	@Expose
	private int type;

	/**
	 * Order in diagnosis array
	 */
	@Expose
	private int diagnosisOrder;

	/**
	 * Protoype used for this diagnosis.
	 */
	private DiagnosisPrototype diagnosisPrototype;
	
	// TODO is used?
	private String extendedDiagnosisText;

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
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
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
	public Date getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(Date generationDate) {
		this.generationDate = generationDate;
	}

	@Basic
	public int getType() {
		return type;
	}

	public void setType(int type) {
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
	public Date getFinalizedDate() {
		return finalizedDate;
	}

	public void setFinalizedDate(Date finalizedDate) {
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

	@Column(columnDefinition = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	@Basic
	public boolean isMalign() {
		return malign;
	}

	public void setMalign(boolean malign) {
		this.malign = malign;
	}

	@Column(columnDefinition = "text")
	public String getExtendedDiagnosisText() {
		return extendedDiagnosisText;
	}

	public void setExtendedDiagnosisText(String extendedDiagnosisText) {
		this.extendedDiagnosisText = extendedDiagnosisText;
	}

	public String getIcd10() {
		return icd10;
	}

	public void setIcd10(String icd10) {
		this.icd10 = icd10;
	}

	@OneToOne
	@NotAudited
	public DiagnosisPrototype getDiagnosisPrototype() {
		return diagnosisPrototype;
	}

	public void setDiagnosisPrototype(DiagnosisPrototype diagnosisPrototype) {
		this.diagnosisPrototype = diagnosisPrototype;
	}
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
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
	public String getArchiveDialog() {
		return null;
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	
	/********************************************************
	 * Transient
	 ********************************************************/
	@Transient
	public String getDiagnosisTypAsName() {
		switch (getType()) {
		case TYPE_DIAGNOSIS:
			return "Diagnose";
		case TYPE_FOLLOW_UP_DIAGNOSIS:
			return "Nachbefundung";
		default:
			return "Revision";
		}
	}
	
	@Transient
	public void updateDiagnosisWithPrototype(DiagnosisPrototype diagnosisPrototype) {
		setDiagnosis(diagnosisPrototype.getDiagnosisText());
		setExtendedDiagnosisText(diagnosisPrototype.getExtendedDiagnosisText());
		setMalign(diagnosisPrototype.isMalign());
		setIcd10(diagnosisPrototype.getIcd10());
	}
		
	/********************************************************
	 * Transient
	 ********************************************************/
}
