package org.histo.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.InformedConsentType;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Task;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "bioBank_sequencegenerator", sequenceName = "bioBank_sequence")
public class BioBank implements HasDataList{

	private long id;
	
	private long version;
	
	private Task task;
	
	private InformedConsentType informedConsentType;
	
	private List<PDFContainer> attachedPdfs;

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "bioBank_sequencegenerator")
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
	
	@OneToOne(fetch = FetchType.LAZY)
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	@Enumerated(EnumType.STRING)
	public InformedConsentType getInformedConsentType() {
		return informedConsentType;
	}

	public void setInformedConsentType(InformedConsentType informedConsentType) {
		this.informedConsentType = informedConsentType;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface HasDataList
	 ********************************************************/
	@OneToMany(fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@Override
	public List<PDFContainer> getAttachedPdfs() {
		// TODO Auto-generated method stub
		return attachedPdfs;
	}

	@Override
	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
		
	}

	/********************************************************
	 * Interface HasDataList
	 ********************************************************/

	@Override
	@Transient
	public String getDatalistIdentifier() {
		return "interface.hasDataList.biobank";
	}
}
