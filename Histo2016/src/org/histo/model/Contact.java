package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.histo.config.enums.ContactRole;
import org.histo.model.util.LogAble;

@Entity
@SequenceGenerator(name = "contact_sequencegenerator", sequenceName = "contact_sequence")
public class Contact implements LogAble {

    public static final int ROLE_NONE = 0;
    public static final int ROLE_SURGEON = 1;
    public static final int ROLE_EXTERN = 2;
    public static final int ROLE_OTHER = 3;
    
    private long id;

    private Physician physician;

    private ContactRole role;
    
    private boolean primaryContact;

    private boolean usePhone;
    private boolean useFax;
    private boolean useEmail;

    private boolean performed;

    /**
     * Transient, wird für das Auswählen neuer Kontakte und das abwählen alter benötigt
     */
    private boolean selected;

    public Contact() {
    }

    public Contact(Physician physician) {
	this.physician = physician;
    }

    @Id
    @GeneratedValue(generator = "contact_sequencegenerator")
    @Column(unique = true, nullable = false)
    public long getId() {
	return id;
    }

    public void setId(long id) {
	this.id = id;
    }

    @OneToOne
    public Physician getPhysician() {
	return physician;
    }

    public void setPhysician(Physician physician) {
	this.physician = physician;
    }

    public ContactRole getRole() {
        return role;
    }

    public void setRole(ContactRole role) {
        this.role = role;
    }

    public boolean isUsePhone() {
	return usePhone;
    }

    public void setUsePhone(boolean usePhone) {
	this.usePhone = usePhone;
    }

    public boolean isUseFax() {
	return useFax;
    }

    public void setUseFax(boolean useFax) {
	this.useFax = useFax;
    }

    public boolean isUseEmail() {
	return useEmail;
    }

    public void setUseEmail(boolean useEmail) {
	this.useEmail = useEmail;
    }

    public boolean isPerformed() {
	return performed;
    }

    public void setPerformed(boolean performed) {
	this.performed = performed;
    }

    @Transient
    public boolean isSelected() {
	return selected;
    }

    public void setSelected(boolean selected) {
	this.selected = selected;
    }

	
    public boolean isPrimaryContact() {
		return primaryContact;
	}

	public void setPrimaryContact(boolean primaryContact) {
		this.primaryContact = primaryContact;
	}

    
}
