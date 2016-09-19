package org.histo.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.histo.config.HistoSettings;
import org.histo.model.util.LogAble;
import org.histo.model.util.StainingStatus;
import org.histo.model.util.StainingTreeParent;
import org.histo.util.TimeUtil;

@Entity
@SequenceGenerator(name = "block_sequencegenerator", sequenceName = "block_sequence")
public class Block implements StainingTreeParent<Sample>, StainingStatus, LogAble {

	private long id;

	/**
	 * Parent of this block
	 */
	private Sample parent;

	/**
	 * ID in block
	 */
	private String blockID;

	/**
	 * Number increases with every staining
	 */
	private int stainingNumber = 1;

	/**
	 * staining array
	 */
	private List<Staining> stainings;

	/**
	 * Date of sample creation
	 */
	private Date generationDate;

	/**
	 * Wenn true wird dieser block nicht mehr angezeigt.
	 */
	private boolean archived;

	public void removeStaining(Staining staining) {
		getStainings().remove(staining);
	}

	public void incrementStainingNumber() {
		this.stainingNumber++;
	}

	public void decrementStainingNumber() {
		this.stainingNumber--;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "block_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("generationDate ASC, id ASC")
	public List<Staining> getStainings() {
		if (stainings == null)
			stainings = new ArrayList<>();
		return stainings;
	}

	public void setStainings(List<Staining> stainings) {
		this.stainings = stainings;
	}

	@Basic
	public int getStainingNumber() {
		return stainingNumber;
	}

	public void setStainingNumber(int stainingNumber) {
		this.stainingNumber = stainingNumber;
	}

	@Basic
	public String getBlockID() {
		return blockID;
	}

	public void setBlockID(String blockID) {
		this.blockID = blockID;
	}

	@Basic
	public Date getGenerationDate() {
		return generationDate;
	}

	public void setGenerationDate(Date generationDate) {
		this.generationDate = generationDate;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wrude
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(generationDate, new Date(System.currentTimeMillis())))
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück wenn alle Färbungen gemacht wurden, gibt fals zurück
	 * wenn mindestens eine Färbung aussteht.
	 */
	@Override
	@Transient
	public boolean isStainingPerformed() {
		
		boolean found = false;
		for (Staining staining : stainings) {

			if (staining.isArchived())
				continue;

			if (!staining.isStainingPerformed())
				return false;
			else
				found = true;
		}
		
		return found;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht.
	 */
	@Override
	@Transient
	public boolean isStainingNeeded() {
		if (!isStainingPerformed())
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht. Unterscheidet
	 * nicht zwischen Nachfärbung und Färbung. Dies geht erst auf Sample level.
	 */
	@Override
	@Transient
	public boolean isReStainingNeeded() {
		if (!isStainingPerformed())
			return true;
		return false;
	}

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@ManyToOne
	public Sample getParent() {
		return parent;
	}

	public void setParent(Sample parent) {
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
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Setzt alle Kinder
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;

		// setzt alle Kinder
		for (Staining staining : getStainings()) {
			staining.setArchived(archived);
		}
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die BlockID als identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getBlockID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zurück
	 */
	@Transient
	@Override
	public String getArchiveDialog() {
		return HistoSettings.dialog(HistoSettings.DIALOG_ARCHIV_BLOCK);
	}
	/******************************************************** ArchiveAble ********************************************************/
}
