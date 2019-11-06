package com.capitalone.weathertracker.data.repository;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.capitalone.weathertracker.data.entity.MeasurementEntity;


/**
 * This interface defines a CRUD repository for persisting Measurements. The default interface defines findAll().
 * We define two custom methods for seaching by timestamps.
 * 
 * @author doug
 *
 */

@Repository
public interface MeasurementRepository extends CrudRepository<MeasurementEntity, Long> {

	/**
	 * This method returns one or more {@link MeasurementEntities} that have the specified timestamp. 
	 * @param date specific timestamp to search for.
	 * @return collection of MeasurementEntities that match the timestamp.
	 */
	List<MeasurementEntity> findByTimestamp(ZonedDateTime date);
	
	/**
	 * This method returns one or more {@link MeasurementEntities} whose timestamps are between the
	 * specified start and end dates.  
	 * 
	 * NOTE: The start date is inclusive while the end date is exclusive.
	 * 
	 * @param start specifies the start of the range to search
	 * @param end specifies the end of the range to search
	 * @return collection of MeasurementEntities whose timestamps lie in the specified range
	 */
	
	List<MeasurementEntity> findByTimestampGreaterThanEqualAndTimestampLessThan(ZonedDateTime start, ZonedDateTime end);
	
}
