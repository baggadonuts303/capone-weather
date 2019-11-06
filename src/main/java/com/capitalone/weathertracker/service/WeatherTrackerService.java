package com.capitalone.weathertracker.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capitalone.weathertracker.data.entity.MeasurementEntity;
import com.capitalone.weathertracker.data.entity.MetricEntity;
import com.capitalone.weathertracker.data.repository.MeasurementRepository;
import com.capitalone.weathertracker.measurements.Measurement;
import com.capitalone.weathertracker.measurements.MeasurementQueryService;
import com.capitalone.weathertracker.measurements.MeasurementStore;
import com.capitalone.weathertracker.statistics.AggregateResult;
import com.capitalone.weathertracker.statistics.MeasurementAggregator;
import com.capitalone.weathertracker.statistics.Statistic;


/**
 * WeatherTrackerService - this class implements the query service, measurement store, and measurement
 * aggregator interfaces. It is implemented as a Spring {@link org.springframework.stereotype.Service}
 * 
 * A measurement is a collection of metrics associated with a particular timestamp. Metrics are key/value
 * pairs where the name is the type of metric (e.g. temperature, dewPoint, etc) and the value is double
 * floating point number that is the value of the metric at the specified timestamp.
 * 
 * Measurements are persisted by storing each metric key/value in a METRICS table.  Each metric also
 * stores the associated timestamp.  This allows for searching for metrics of a given type between
 * a range of timestamps.  This is required for some of the supported statistics.
 * 
 * 
 * @author doug
 *
 */

@Service
class WeatherTrackerService implements MeasurementQueryService, MeasurementStore, MeasurementAggregator {
	
	// add logger for writing debugging and exception information to the log
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private MeasurementRepository		measurementRepositoty;

	
	
  // ----------------------------------------------------------------------------------------------------
  // MeasurementStore implementation methods

  @Override
  /**
   * Persists a Measurement by creating a {@link MeasurementEntity} and then adding MetricEntities for each
   * metric contained in the measurement
   */
  public void add(Measurement measurement) {
	  
	  logger.info("  adding measurement: ts=" + measurement.getTimestamp() + ", metrics: " + measurement.getMetrics().toString());
	  
	  // create the measurement entity 
	  MeasurementEntity me = new MeasurementEntity(measurement.getTimestamp());
	  
	  Map<String, Double> 	metrics = measurement.getMetrics();
	  
	  // create a MetricEntity for each metric contained with in the measurement 
	  metrics.forEach((k,v) -> {
		  me.addMetric( new MetricEntity(k,v));
	  });
	  	  
	  // save measurement in the repository
	  measurementRepositoty.save(me);

  }


  /**
   * This private helper method is used to convert from database MeasurementEntities to Measurement objects
   * 
   * @param me MeasurementEntity to convert
   * @return Measurement object
   */
  private Measurement measurementFromEntity( MeasurementEntity me ) {
	  
	  // create a builder to create the Measurement to return to the caller
	  Measurement.Builder	builder = new Measurement.Builder();
	  
	  // set the Measurement timestamp
	  builder.withTimestamp(me.getTimestamp());
	  
	  // get the metrics collection for the first measurement.  As mentioned above we currently assume that
	  // there is only ever one measurement for a particular timestamp.
	  List<MetricEntity> 	metrics = me.getMetrics();
	  
	  // add each metric to the Measurement
	  metrics.forEach( m -> {
		  builder.withMetric(m.getName(), m.getValue());  
	  });
	  
	  // return Measurement to the caller
	  return builder.build();	  
  }
  
  
  
  /**
   * Returns a {@link Measurement} with the specified time stamp.
   * 
   * NOTE: there should only ever be one measurement with the specified timestamp.
   * We could support multiple measurements with the same timestamp by allowing this method to return a collection.
   * 
   */
  
  @Override
  public Measurement fetch(ZonedDateTime timestamp) {
	  
	  logger.info("  fetching measurement: ts=" + timestamp );


	  // search for all measurements with the specified time stamp
	  List<MeasurementEntity>	measurements = this.measurementRepositoty.findByTimestamp(timestamp);
	  
	  // if no measurements match the specified timestamp then return null 	  
	  if ( measurements.isEmpty()) {
		  logger.warn("     no measurements for ts=" + timestamp );
		  return null;
	  }	  
	  
	  // we should only have one measurement for a single timestamp; issue warning if we find more.
	  if ( measurements.size() > 1) {
		  logger.warn("     multiple measurements ts=" + timestamp );
	  }	  
	  
	  // get the metrics collection for the first measurement.  As mentioned above we currently assume that
	  // there is only ever one measurement for a particular timestamp.
	  MeasurementEntity me = measurements.get(0);
	  
	  logger.info("     found metrics: " + me.getMetrics().toString() );

	  // convert from entity to Measurement
	  return measurementFromEntity(me);
  }

  
  
  
  // ----------------------------------------------------------------------------------------------------
  // MeasurementQueryService implementation methods
  
  
  /**
   * queryDateRange() - this method returns all measurements whose timestamp falls within the date/time
   * range specified as a from and to {@link ZonedDateTime).
   */
  @Override
  public List<Measurement> queryDateRange(ZonedDateTime from, ZonedDateTime to) {
	  
	  logger.info("  query for date range: from=" + from + " (inclusive), to=" + to + " (exclusive)" );

	  // create list to hold matching measurements
	  List<Measurement>	measurements = new ArrayList<Measurement>();
	  
	  // query the respository for all matching measurement entities
	  List<MeasurementEntity> entities = measurementRepositoty.findByTimestampGreaterThanEqualAndTimestampLessThan(from, to);
	  
	  logger.info("     found " + entities.size() + " measurement(s)" );
	  
	  // convert each entity to measurement
	  entities.forEach( m -> {
		  
		  measurements.add(measurementFromEntity(m));
	  });
	  
	  return measurements; 
  }

  
  
  
  // ----------------------------------------------------------------------------------------------------
  // MeasurementAggregator implementation methods
  
  
  /**
   * This method generates a list of result objects that contain statistical data on the supplied measurements.
   * 
   * This method allows multiple stats (min,max,etc.) and multiple metrics to be specified.  In cases where multiple
   * stats and/or metrics are supplied, this method returns all permutations of stats and metrics. 
   * 
   * e.g. if stats "min", "max", and "average" are specified, and metrics "temp", "dewPoint", and "precip" are specified,
   * this method will generate NINE entries in the list of results.
   * 
   * If any metric is not contained in the list of measurements, then the result list will not include that metric.
   * 
   * If no aggregated results are found, this method will return an empty list.
   * 
   * 
   */
  @Override
  public List<AggregateResult> analyze(List<Measurement> measurements, List<String> metrics, List<Statistic> stats) {

	  // IMPLEMENTATION NOTE:  We have already been to the repository to gather the list of measurements to analyze.
	  //	We could create a repository on for the stored METRICS and use the database to compute the aggredated stats.
	  //	For simplicity we will hand calculate the stats here programatically rather than make the database more complicated.
	  
	  List<AggregateResult>	results = new ArrayList<AggregateResult>();
	  
	  // for each metric specified, collect the values for that metric
	  metrics.forEach( metric -> {
	  
		  List<Double>	values = getMetricValuesByName(measurements, metric );
		  if ( !values.isEmpty() ) {
			  calculateStats( metric, stats, values, results );
		  }
	  });

  
	  return results;
  }
  
  
  
  
  
  
  // ------------------------------------------------------------------------------------------------------------------------
  // private helper methods
  // ------------------------------------------------------------------------------------------------------------------------
  
  // compute the minimum value in the list by sorting ascending and taking the first element
  private Double minValue( List<Double> values ) {
	  values.sort(null);
	  return values.get(0);
  }
  
  // calculate the maximum value in the list by sorting ascending and taking the last element
  private Double maxValue( List<Double> values ) {
	  values.sort(null);
	  return values.get(values.size()-1);
  }
  
  // private helper method to compute the average of a list of values.  If the list is empty, this method returns zero.
  private Double averageValue( List<Double> values ) {
	  
	  // check for empty values list to avoid divide by zero
	  if ( values.size() > 0  ) {
		  Double sum = 0.0;
		  ListIterator<Double>	iter = values.listIterator();
		  while(iter.hasNext()){
			  sum += iter.next();
		  }
		  // return the average as the sum divided by the number of elements
		  
		  // round up to 2 decimal digits
		  Double avg = Math.round(sum / values.size() * 100D) / 100D;
		  
		  return avg;
		  
	  } else {
		  return 0.0;		  
	  }	 		
  }
  
  /**
   * This private helper method calculates the all of the requested stats for a single metric. 
   * @param metric name of metric being processed
   * @param stats list of stats to compute for this metric
   * @param values list of values to use to compute the stat
   * @param results result set to add calculated values to
   */
  private void calculateStats( String metric, List<Statistic> stats, List<Double> values, List<AggregateResult> results ) {
	  
	  // for each statistics we want to compute
	  stats.forEach( s -> {
		  
		  // if the value list is not empty
		  if ( !values.isEmpty() ) {
			  
			  // determine which stat we are being asked to compute and call the appropriate helper method
			  
			  if ( s.equals(Statistic.MIN) ) {
				  
				  results.add( new AggregateResult(metric, s, minValue( values ) ) );
				  
			  } else if ( s.equals( Statistic.MAX ) ) {

				  results.add( new AggregateResult(metric, s, maxValue( values ) ) );
				 
			  } else if ( s.equals( Statistic.AVERAGE) ) {

				  results.add( new AggregateResult(metric, s, averageValue( values ) ) );
				  
			  } else {
				  // unknown stat.  Ideally we should have done better input validation before we get here.
				  throw new IllegalArgumentException("unknown statistic specified");
			  }
		  }
	  });
  }
  
  /**
   * This private helper method extracts the list of values for a specified metric from the collection
   * of measurements.
   * 
   * 
   * 
   * @param measurements collection of measurements from which to extract the metric values
   * @param metricName name of metric
   * @return collection of values of specified metric
   */
  private List<Double>getMetricValuesByName( List<Measurement> measurements, String metricName ) {
	  
	  // create empty result collection
	  List<Double>	results = new ArrayList<Double>();
	  
	  // for every measurement in the collection...
	  measurements.forEach( m -> {
		  
		  // grab the metric value for the specified metric name - if it exists
		  Double v = m.getMetric(metricName);
		  
		  // add to the results if metric value was captured in the current measurement
		  if ( v != null ) {
			  results.add(v); 
		  }
		  
	  });
	  
	  return results;
  }
  
  
  
  
  
  
  
  
}
