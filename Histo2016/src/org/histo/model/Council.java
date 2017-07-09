package org.histo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.CouncilState;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.HasID;
import org.histo.model.patient.Task;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "council_sequencegenerator", sequenceName = "council_sequence")
public class Council implements HasID, HasDataList {
	private long id;

	private long version;

	private Task task;

	/**
	 * Name of the council
	 */
	private String name;

	/**
	 * Council physician
	 */
	private Physician councilPhysician;

	/**
	 * Physician to sign the council
	 */
	private Physician physicianRequestingCouncil;

	/**
	 * Text of council
	 */
	private String councilText;

	/**
	 * Attached slides of the council
	 */
	private String attachment;

	/**
	 * Date of request
	 */
	private long dateOfRequest;

	/**
	 * State of the council
	 */
	private CouncilState councilState;

	/**
	 * Pdf attached to this council
	 */
	private List<PDFContainer> attachedPdfs;

	public Council() {
	}

	public Council(Task task) {
		this.task = task;
	}

	@Id
	@GeneratedValue(generator = "council_sequencegenerator")
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

	@OneToOne
	public Physician getCouncilPhysician() {
		return councilPhysician;
	}

	public void setCouncilPhysician(Physician councilPhysician) {
		this.councilPhysician = councilPhysician;
	}

	@OneToOne
	public Physician getPhysicianRequestingCouncil() {
		return physicianRequestingCouncil;
	}

	public void setPhysicianRequestingCouncil(Physician physicianRequestingCouncil) {
		this.physicianRequestingCouncil = physicianRequestingCouncil;
	}

	@Column(columnDefinition = "text")
	public String getCouncilText() {
		return councilText;
	}

	public void setCouncilText(String councilText) {
		this.councilText = councilText;
	}

	@Column(columnDefinition = "text")
	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}

	@Column
	public long getDateOfRequest() {
		return dateOfRequest;
	}

	public void setDateOfRequest(long dateOfRequest) {
		this.dateOfRequest = dateOfRequest;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Enumerated(EnumType.ORDINAL)
	public CouncilState getCouncilState() {
		return councilState;
	}

	public void setCouncilState(CouncilState councilState) {
		this.councilState = councilState;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	public List<PDFContainer> getAttachedPdfs() {
		if (attachedPdfs == null)
			attachedPdfs = new ArrayList<PDFContainer>();
		return attachedPdfs;
	}

	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	@Transient
	public Date getDateOfRequestAsDate() {
		return new Date(dateOfRequest);
	}

	public void setDateOfRequestAsDate(Date dateOfRequestAsDate) {
		this.dateOfRequest = dateOfRequestAsDate.getTime();
	}

	@Transient
	public boolean isCouncilState(CouncilState... councilStates) {
		for (CouncilState councilState : councilStates) {
			if (getCouncilState() == councilState)
				return true;
		}

		return false;
	}

	@Override
	@Transient
	public String getDatalistIdentifier() {
		return "interface.hasDataList.council";
	}

}
