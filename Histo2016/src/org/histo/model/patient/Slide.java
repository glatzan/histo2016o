package org.histo.model.patient;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.util.TaskUtil;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "slide_sequencegenerator", sequenceName = "slide_sequence")
@Getter
@Setter
public class Slide implements Parent<Block>, LogAble, DeleteAble, PatientRollbackAble<Block>, IdManuallyAltered, HasID {

	@Id
	@GeneratedValue(generator = "slide_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	@Column
	private int uniqueIDinTask;

	@Column
	private String slideID = "";

	@Column
	private boolean idManuallyAltered;

	@Column
	private long creationDate;

	@Column
	private long completionDate;
	
	@Column
	private boolean stainingCompleted;

	@Column
	private boolean reStaining;
	
	@Column(columnDefinition = "VARCHAR")
	private String commentary = "";

	@OneToOne
	@NotAudited
	private StainingPrototype slidePrototype;

	@ManyToOne
	private Block parent;

	@Transient
	public void updateAllNames() {
		updateNameOfSlide(getTask().isUseAutoNomenclature(), false);
	}
	
	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		updateNameOfSlide(useAutoNomenclature, ignoreManuallyNamedItems);
	}

	@Transient
	public boolean updateNameOfSlide(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		if (!isIdManuallyAltered() || (isIdManuallyAltered() && ignoreManuallyNamedItems)) {
			StringBuilder name = new StringBuilder();

			if (useAutoNomenclature) {

				// generating block id
				name.append(parent.getParent().getSampleID());
				name.append(parent.getBlockID());
				if (name.length() > 0)
					name.append(" ");

				name.append(slidePrototype.getName());

				int stainingsInBlock = TaskUtil.getNumerOfSameStainings(this);

				if (stainingsInBlock > 1)
					name.append(String.valueOf(stainingsInBlock));

				if (getSlideID() == null || !getSlideID().equals(name)) {
					setSlideID(name.toString());
					setIdManuallyAltered(false);
					return true;
				}
			} else if (getSlideID() == null || getSlideID().isEmpty()) {
				// only setting the staining and the number of the stating
				name.append(slidePrototype.getName());

				int stainingsInBlock = TaskUtil.getNumerOfSameStainings(this);

				if (stainingsInBlock > 1)
					name.append(String.valueOf(stainingsInBlock));

				setSlideID(name.toString());
				setIdManuallyAltered(false);
				return true;
			}
		}

		return false;
	}

	@Override
	@Transient
	public String toString() {
		return "Slide: " + getSlideID() + (getId() != 0 ? ", ID: " + getId() : "");
	}


	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/**
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return getParent().getTask();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt die Objekttr�gerID als identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Objekttr�ger " + getSlideID();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble <br>
	 * Gibt den Dialog zum archivieren zur�ck
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.DELETE_TREE_ENTITY;
	}

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
}
