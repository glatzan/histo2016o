package org.histo.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

/**
 * Diagnose
 * NAchbefundung
 * Revision
 * 
 * @author andi
 *
 */
@Entity
@SequenceGenerator(name = "diagnosis_sequencegenerator", sequenceName = "diagnosis_sequence")
public class Diagnosis {

    public static final int TYPE_DIAGNOSIS = 0;
    public static final int TYPE_FOLLOW_UP_DIAGNOSIS = 1;
    public static final int TYPE_DIAGNOSIS_REVISION = 2;
    
    private long id;

    private long generationDate;
    
    private long finalizedDate;
    
    private boolean finalized;
    
    private String name; 
    
    private String diagnosis;
    
    private boolean malign;

    private String extendedDiagnosisText;

    private String commentary; 
    
    private int type;
    
    private int diagnosisOrder;
    
    /******************************************************** Getter/Setter ********************************************************/
    @Id
    @GeneratedValue(generator = "diagnosis_sequencegenerator")
    @Column(unique = true, nullable = false)
    public long getId() {
	return id;
    }
    
    public void setId(long id){
	this.id = id;
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
    /******************************************************** Getter/Setter ********************************************************/
    
    @Transient
    public String getDiagnosisTypAsName(){
	switch (getType()) {
	case TYPE_DIAGNOSIS:
	    return "Diagnose";
	case TYPE_FOLLOW_UP_DIAGNOSIS:
	    return "Nachbefundung";
	default:
	    return "Revision"; 
	}
    }
}
