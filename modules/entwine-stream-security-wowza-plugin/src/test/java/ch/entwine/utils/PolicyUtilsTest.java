package ch.entwine.utils;

import static org.junit.Assert.assertEquals;
import ch.entwine.wowza.Policy;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class PolicyUtilsTest {
  private static final String EXAMPLE_IP = "10.0.0.1";

  @Test
  public void testToJson() {
    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    Policy policy = new Policy("http://mh-allinone/", before);
    assertEquals("{\"Statement\":{\"Condition\":{\"DateLessThan\":" + before.getMillis()
            + "},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}", PolicyUtils.toJson(policy).toJSONString());
    // With optional parameters
    policy = new Policy("http://mh-allinone/", before);
    policy.setClientIpAddress(EXAMPLE_IP);
    policy.setDateGreaterThan(new DateTime(2015, 02, 28, 00, 46, 19, 0, DateTimeZone.UTC));
    assertEquals(
            "{\"Statement\":{\"Condition\":{\"DateGreaterThan\":1425084379000,\"DateLessThan\":1425170777000,\"IpAddress\":\"10.0.0.1\"},\"Resource\":\"http:\\/\\/mh-allinone\\/\"}}",
            PolicyUtils.toJson(policy).toJSONString());
  }
  
  @Test
  public void testFromJson() throws UnsupportedEncodingException {
    String policyJson = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}";
    Policy policy = PolicyUtils.fromJson(policyJson);
    assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.getResource());
    assertEquals(EXAMPLE_IP, policy.getClientIpAddress());

    DateTime after = new DateTime(2015, 02, 28, 00, 46, 19, 0, DateTimeZone.UTC);
    after = after.withSecondOfMinute(19);
    assertEquals(after, policy.getDateGreaterThan());

    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    assertEquals(before, policy.getDateLessThan());
  }
  
  @Test
  public void testBase64Decoding() throws UnsupportedEncodingException {
    String policyValue = "{policy:'The Policy'}";
    String result = PolicyUtils.base64Decode(PolicyUtils.base64Encode(policyValue));
    assertEquals(policyValue, result);
  }
  
  @Test
  public void testFromBase64EncodedPolicy() throws UnsupportedEncodingException {
    String examplePolicy = "{\"Statement\": {\"Resource\":\"http://mh-allinone/engage/url/to/resource.mp4\",\"Condition\":{\"DateLessThan\":1425170777000,\"DateGreaterThan\":1425084379000,\"IpAddress\": \"10.0.0.1\"}}}";
    Policy policy = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.base64Encode(examplePolicy));
    assertEquals("http://mh-allinone/engage/url/to/resource.mp4", policy.getResource());
    assertEquals(EXAMPLE_IP, policy.getClientIpAddress());

    DateTime after = new DateTime(2015, 02, 28, 00, 46, 19, 0, DateTimeZone.UTC);
    after = after.withSecondOfMinute(19);
    assertEquals(after, policy.getDateGreaterThan());

    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    assertEquals(before, policy.getDateLessThan());
  }
  
  @Test
  public void testToBase64EncodedPolicy() throws UnsupportedEncodingException {
    String resource = "http://mh-allinone/";
    DateTime before = new DateTime(2015, 03, 01, 00, 46, 17, 0, DateTimeZone.UTC);
    Policy policy = new Policy(resource, before);
    
    Policy result = PolicyUtils.fromBase64EncodedPolicy(PolicyUtils.toBase64EncodedPolicy(policy));
    assertEquals(resource, result.getResource());
    assertEquals(before, result.getDateLessThan());
  }
}
