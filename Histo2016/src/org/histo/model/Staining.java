package org.histo.model;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.histo.config.HistoSettings;

import histo.model.util.StainingTreeParent;

@Entity
@SequenceGenerator(name = "staining_sequencegenerator", sequenceName = "staining_sequence")
public class Staining implements StainingTreeParent<Block> {

	private long id;

	private Date generationDate;

	private String stainingID;

	private boolean stainingPerformed;

	private boolean reStaining;

	private String commentary;

	private StainingPrototype stainingType;

	/**
	 * Eltern Block des Stainings
	 */
	private Block parent;

	/**
	 * Wenn true wird dieser Objektträger nicht mehr angezeigt
	 */
	private boolean archived;

	@Id
	@GeneratedValue(generator = "staining_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Column
	public boolean isStainingPerformed() {
		return stainingPerformed;
	}

	public void setStainingPerformed(boolean stainingPerformed) {
		this.stainingPerformed = stainingPerformed;
	}

	@OneToOne
	public StainingPrototype getStainingType() {
		return stainingType;
	}

	public void setStainingType(StainingPrototype stainingType) {
		this.stainingType = stainingType;
	}

	@Basic
	public Date getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(Date generationDate) {
		this.generationDate = generationDate;
	}

	@Basic
	public String getStainingID() {
		return stainingID;
	}

	public void setStainingID(String stainingID) {
		this.stainingID = stainingID;
	}

	@Basic
	public boolean isReStaining() {
		return reStaining;
	}

	public void setReStaining(boolean reStaining) {
		this.reStaining = reStaining;
	}

	@Basic
	@Type(type = "text")
	public String getCommentary() {
		return commentary;
	}

	public void setCommentary(String commentary) {
		this.commentary = commentary;
	}

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@ManyToOne
	public Block getParent() {
		return parent;
	}

	public void setParent(Block parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die ObjektträgerID als identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getStainingID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zurück
	 */
	@Transient
	@Override
	public String getArchiveDialog() {
		return HistoSettings.dialog(HistoSettings.DIALOG_ARCHIV_STAINING);
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
}
