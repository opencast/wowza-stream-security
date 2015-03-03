package ch.entwine.wowza;

import org.joda.time.DateTime;

/**
 * Represents a policy for a signed resource that looks like:
 * 
 * {
 *   "Statement": {
 *     "Resource":"URL or stream name of the object",
 *     "Condition":{
 *        "DateLessThan":1425170777000,
 *        "DateGreaterThan":1425084379000, // Optional
 *        "IpAddress": 10.0.0.1 // Optional
 *     }
 *   }
 * }
 * 
 */
public class Policy {
  /** An optional client ip address that made the original request. */
  private String clientIpAddress;
  /** The date and time when the resource expires. */
  private DateTime dateLessThan;
  /** The date and time when the resource will become available. */
  private DateTime dateGreaterThan;
  /** The base URL for the resource being requested. */
  private String resource;
  
  public Policy(String resource, DateTime dateLessThan) {
    if (resource == null || dateLessThan == null) {
      throw new IllegalStateException("A policy must have both a non-null resource and date less than.");
    }
    this.resource = resource;
    this.dateLessThan = dateLessThan;
  }

  public String getClientIpAddress() {
    return clientIpAddress;
  }
  
  public void setClientIpAddress(String clientIpAddress) {
    this.clientIpAddress = clientIpAddress;
  }
  
  public DateTime getDateLessThan() {
    return dateLessThan;
  }

  public void setDateLessThan(DateTime dateLessThan) {
    this.dateLessThan = dateLessThan;
  }
  
  public DateTime getDateGreaterThan() {
    return dateGreaterThan;
  }

  public void setDateGreaterThan(DateTime dateGreaterThan) {
    this.dateGreaterThan = dateGreaterThan;
  }
  
  public String getResource() {
    return resource;
  }
  
  public void setResource(String resource) {
    this.resource = resource;
  }
}
