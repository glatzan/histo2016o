package org.histo.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.config.enums.ContactRole;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.interfaces.LogAble;

import lombok.Getter;
import lombok.Setter;

@Entity
@SequenceGenerator(name = "diagnosisPreset_sequencegenerator", sequenceName = "diagnosisPreset_sequence")
@Getter
@Setter
public class DiagnosisPreset implements  LogAble, ListOrder<DiagnosisPreset>, HasID {

	@Id
	@GeneratedValue(generator = "diagnosisPreset_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;
	@Column(columnDefinition = "VARCHAR")
	private String category;
	@Column(columnDefinition = "VARCHAR")
	private String icd10;
	@Column(columnDefinition = "VARCHAR")
	private boolean malign;
	@Column(columnDefinition = "text")
	private String diagnosis;
	@Column(columnDefinition = "text")
	private String extendedDiagnosisText;
	@Column(columnDefinition = "text")
	private String commentary;
	@Column
	private int indexInList;
	@ElementCollection(fetch = FetchType.EAGER)
	@Enumerated(EnumType.STRING)
	@Fetch(value = FetchMode.SUBSELECT)
	@Cascade(value = { org.hibernate.annotations.CascadeType.ALL })
	private List<ContactRole> diagnosisReportAsLetter;
	
	public DiagnosisPreset() {
	}

	public DiagnosisPreset(DiagnosisPreset diagnosisPreset) {
		this.id = diagnosisPreset.getId();
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


