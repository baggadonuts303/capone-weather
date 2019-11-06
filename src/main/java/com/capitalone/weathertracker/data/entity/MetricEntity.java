package com.capitalone.weathertracker.data.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 * MetricEntity - this class is used to persist a single metric.  A metric consists of
 * a name and a single floating point value.  Each metric is owned by a Measurement
 * which can consists of multiple metrics.
 * 
 * @author doug
 *
 */

@Entity
@Table(name="METRIC")
public class MetricEntity {


	/**
	 * Auto-generated id used as the primary key
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	/**
	 * name of metric
	 */
	@Column(name="NAME")
	private String name;
	
	/**
	 * value of metric
	 */
	@Column(name="VALUE")
	private double value;
	
	/**
	 * Link back to the Measurement to which this metric belongs
	 */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "OWNING_MEASUREMENT_ID")
	@JsonIgnore
    private MeasurementEntity owningMeasurement;

    
    // hidden default constructor
	protected MetricEntity() { 
	}
	
	/**
	 * Constructor
	 * @param name name of metric
	 * @param value value of metric
	 */
	public MetricEntity( String name, double value )
	{	
		super();
		this.name = name;
		this.value = value;
	}
	
	/**
	 * method to set the owning MeasurementEntity
	 * @param m reference to owning MeasureEntity
	 */
	public void setOwningMeasurement( MeasurementEntity m ) {
		this.owningMeasurement = m;
	}

	
	public String getName() {
		return name;
	}
	
	public double getValue() {
		return value;
	}
	
}

