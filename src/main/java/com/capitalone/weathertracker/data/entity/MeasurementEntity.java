package com.capitalone.weathertracker.data.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * This class is a repository entity for storing Measurements.  Each measurement contains a timestamp when the measurement
 * was made and zero or more metrics. Each metric consists of a name and floating point value.
 * 
 * The metrics are stored as {@link MetricEntity} objects.
 * 
 * @author doug
 *
 */


@Entity
@Table(name="MEASUREMENTS")
public class MeasurementEntity {
	
	/**
	 * Auto-generated ID column used as primary key
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	/**
	 * timestamp - formated in ISO-8061 format
	 */
	@Column(name="TIMESTAMP")
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-ddTHH:mm:ss.fffZ", timezone="UTC")
	private ZonedDateTime timestamp;
      
	/**
	 * join to METRICS table - MeasurementEntities may link to one or more MetricEntities
	 */
    @OneToMany(
			fetch = FetchType.EAGER,
        mappedBy = "owningMeasurement", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true
    )
    private List<MetricEntity> metrics = new ArrayList<>();

    
    // hidden default constructor
	protected MeasurementEntity() { 
	}
	
	/**
	 * constructor
	 * @param timestamp date/time when measurement was taken
	 */
	public MeasurementEntity( ZonedDateTime timestamp ) {
		super();
		this.timestamp = timestamp;
	}
	
	/**
	 * sets the metrics associated with this measurement
	 * @param newMetrics
	 */
	public void setMetrics( List<MetricEntity> newMetrics ) {
		this.metrics = newMetrics;
	}
	
	/**
	 * Adds a single metric to the collection of metrics associated with this measurement
	 * @param newMetric new metric to add
	 */
    public void addMetric(MetricEntity newMetric) {
    	
        this.metrics.add(newMetric);
        newMetric.setOwningMeasurement(this);
    }

    // accessors
    
    
	public ZonedDateTime getTimestamp() { 
		return this.timestamp;
	}

	public List<MetricEntity> getMetrics() {
		return this.metrics;
	}
	
   
}

