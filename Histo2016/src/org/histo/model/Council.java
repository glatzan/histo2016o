package org.histo.model;

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
import org.histo.util.latex.TextToLatexConverter;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "council_sequencegenerator", sequenceName = "council_sequence")
@Getter
@Setter
public class Council implements HasID, HasDataList {

	@Id
	@GeneratedValue(generator = "council_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	@ManyToOne(fetch = FetchType.LAZY)
	private Task task;

	/**
	 * Name of the council
	 */
	@Column(columnDefinition = "VARCHAR")
	private String name;

	/**
	 * Council physician
	 */
	@OneToOne
	private Physician councilPhysician;

	/**
	 * Physician to sign the council
	 */
	@OneToOne
	private Physician physicianRequestingCouncil;

	/**
	 * Text of council
	 */
	@Column(columnDefinition = "text")
	private String councilText;

	/**
	 * Attached slides of the council
	 */
	@Column(columnDefinition = "text")
	private String attachment;

	/**
	 * Date of request
	 */
	@Column
	private long dateOfRequest;

	/**
	 * State of the council
	 */
	@Enumerated(EnumType.ORDINAL)
	private CouncilState councilState;

	/**
	 * Pdf attached to this council
	 */
	@OneToMany(fetch = FetchType.LAZY)
	@OrderBy("creationDate DESC")
	private List<PDFContainer> attachedPdfs;

	public Council() {
	}

	public Council(Task task) {
		this.task = task;
	}

	@Transient
	public String getCouncilTextAsLatex() {
		return (new TextToLatexConverter()).convertToTex(getCouncilText());
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
