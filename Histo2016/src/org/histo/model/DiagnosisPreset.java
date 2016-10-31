package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.histo.model.util.EditAbleEntity;
import org.histo.model.util.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "standardDiagnosis_sequencegenerator", sequenceName = "standardDiagnosis_sequence")
public class DiagnosisPreset implements EditAbleEntity<DiagnosisPreset>, LogAble {

	@Expose
	private long id;
	@Expose
	private String name;
	@Expose
	private String icd10;
	@Expose
	private boolean malign;
	@Expose
	private String diagnosisText;
	@Expose
	private String extendedDiagnosisText;
	@Expose
	private String commentary;
	
	public DiagnosisPreset() {
	}

	public DiagnosisPreset(DiagnosisPreset diagnosisPreset) {
		this.id = diagnosisPreset.getId();
		update(diagnosisPreset);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "standardDiagnosis_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIcd10() {
		return icd10;
	}

	public void setIcd10(String icd10) {
		this.icd10 = icd10;
	}

	public boolean isMalign() {
		return malign;
	}

	public void setMalign(boolean malign) {
		this.malign = malign;
	}

	@Column(columnDefinition = "text")
	public String getDiagnosisText() {
		return diagnosisText;
	}

	public void setDiagnosisText(String diagnosisText) {
		this.diagnosisText = diagnosisText;
	}

	@Column(columnDefinition = "text")
	public String getExtendedDiagnosisText() {
		return extendedDiagnosisText;
	}

	public void setExtendedDiagnosisText(String extendedDiagnosisText) {
		this.extendedDiagnosisText = extendedDiagnosisText;
	}

	@Column(columnDefinition = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	@Transient
	@Override
	public String asGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

	@Transient
	@Override
	public void update(DiagnosisPreset diagnosisPreset) {
		this.name = diagnosisPreset.getName();
		this.icd10 = diagnosisPreset.getIcd10();
		this.malign = diagnosisPreset.isMalign();
		this.diagnosisText = diagnosisPreset.getDiagnosisText();
		this.extendedDiagnosisText = diagnosisPreset.getExtendedDiagnosisText();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof DiagnosisPreset){
			if(getId() == ((DiagnosisPreset)obj).getId()){
				return true;
			}
		}
		
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return (int)getId();
	}
	
	
}

