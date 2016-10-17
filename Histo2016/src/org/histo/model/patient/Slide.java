package org.histo.model.patient;

import javax.persistence.Basic;
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
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.model.StainingPrototype;
import org.histo.model.util.LogAble;
import org.histo.model.util.TaskTree;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "slide_sequencegenerator", sequenceName = "slide_sequence")
public class Slide implements TaskTree<Block>, LogAble {

	private long id;
	
	private long version;

	private long generationDate;

	private String slideID = "";

	private boolean stainingPerformed;

	private boolean reStaining;

	private String commentary = "";

	private StainingPrototype slidePrototype;

	/**
	 * Eltern Block des Stainings
	 */
	private Block parent;

	/**
	 * Wenn true wird dieser Objektträger nicht mehr angezeigt
	 */
	private boolean archived;

	@Id
	@GeneratedValue(generator = "slide_sequencegenerator")
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

	
	@Basic
	public boolean isStainingPerformed() {
		return stainingPerformed;
	}

	public void setStainingPerformed(boolean stainingPerformed) {
		this.stainingPerformed = stainingPerformed;
	}

	@OneToOne
	@NotAudited
	public StainingPrototype getSlidePrototype() {
		return slidePrototype;
	}

	public void setSlidePrototype(StainingPrototype slidePrototype) {
		this.slidePrototype = slidePrototype;
	}
	
	@Basic
	public long getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(long generationDate) {
		this.generationDate = generationDate;
	}

	@Basic
	public String getSlideID() {
		return slideID;
	}

	public void setSlideID(String slideID) {
		this.slideID = slideID;
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
		return getSlideID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zurück
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.SLIDE_ARCHIV;
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
}
