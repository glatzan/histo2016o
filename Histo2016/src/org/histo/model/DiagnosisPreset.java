package org.histo.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.config.enums.ContactRole;
import org.histo.model.interfaces.EditAbleEntity;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.interfaces.LogAble;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "diagnosisPreset_sequencegenerator", sequenceName = "diagnosisPreset_sequence")
public class DiagnosisPreset implements EditAbleEntity<DiagnosisPreset>, LogAble, ListOrder<DiagnosisPreset>, HasID {

	@Expose
	private long id;
	@Expose
	private String category;
	@Expose
	private String icd10;
	@Expose
	private boolean malign;
	@Expose
	private String diagnosis;
	@Expose
	private String extendedDiagnosisText;
	@Expose
	private String commentary;
	@Expose
	private int indexInList;
	
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
	@GeneratedValue(generator = "diagnosisPreset_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
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
	public String getDiagnosis() {
		return diagnosis;
	}

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = diagnosis;
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
	
	/********************************************************
	 * Interface ListOrder
	 ********************************************************/
	@Column
	public int getIndexInList() {
		return indexInList;
	}

	public void setIndexInList(int indexInList) {
		this.indexInList = indexInList;
	}
	/********************************************************
	 * Interface ListOrder
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
		this.category = diagnosisPreset.getCategory();
		this.icd10 = diagnosisPreset.getIcd10();
		this.malign = diagnosisPreset.isMalign();
		this.diagnosis = diagnosisPreset.getDiagnosis();
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


