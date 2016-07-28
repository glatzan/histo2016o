package org.histo.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

@Entity
@SequenceGenerator(name = "history_sequencegenerator", sequenceName = "history_sequence")
public class History {

    public static final int LEVEL_SYSTEM = 5;
    public static final int LEVEL_DEBUG = 7;
    public static final int LEVEL_INFO = 10;
    public static final int LEVEL_ERROR = 20;
    
    private long id;
    
    private long date;
    
    private Patient patient;
    
    private UserAcc userAcc;
    
    private String messages;
    
    private int level;

    @Id
    @GeneratedValue(generator = "history_sequencegenerator")
    @Column(unique = true, nullable = false)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @OneToOne
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    @OneToOne
    public UserAcc getUserAcc() {
        return userAcc;
    }

    public void setUserAcc(UserAcc userAcc) {
        this.userAcc = userAcc;
    }

    @Column(columnDefinition = "text")
    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }
    
    @Basic
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
    
    @Basic
    public long getDate() {
	return date;
    }

    public void setDate(long date) {
	this.date = date;
    }
    
}
