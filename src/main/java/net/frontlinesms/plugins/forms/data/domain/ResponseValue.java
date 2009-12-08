/**
 * 
 */
package net.frontlinesms.plugins.forms.data.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Class wrapping {@link String} as an {@link Entity} 
 * @author Alex
 */
@Entity
public class ResponseValue {
	
//> INSTANCE PROPERTIES
	/** Unique id for this entity.  This is for hibernate usage. */
	@SuppressWarnings("unused")
	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(unique=true,nullable=false,updatable=false)
	private long id;
	/** the value of this String */
	private String value;

//> CONSTRUCTORS
	/** Empty constructor for hibernate */
	public ResponseValue() {}
	
	/**
	 * Create a new {@link ResponseValue}
	 * @param value value of this object
	 */
	public ResponseValue(String value) {
		this.value = value;
	}
	
//> INSTANCE METHODS
	/** @return the value of this response */
	@Override
	public String toString() {
		return this.value;
	}
}
