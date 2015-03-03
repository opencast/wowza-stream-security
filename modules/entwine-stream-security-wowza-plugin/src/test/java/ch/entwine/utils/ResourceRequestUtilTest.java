package ch.entwine.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import ch.entwine.wowza.EntwineStreamSecurityWowzaPlugin.Status;
import ch.entwine.wowza.Policy;
import ch.entwine.wowza.ResourceRequest;

public class ResourceRequestUtilTest {
  private static final String keyId = "default";
  private static final String key = "0123456789abcdef";
  private static final String clientIp = "10.0.0.1";
  private Properties properties;

  @Before
  public void setUp() {
    properties = new Properties();
    properties.put(keyId, key);
  }

  @Test
  public void testQueryStringParsing() {
    String policyValue = "{policy:'value'}";
    String signatureValue = "randomString";

    String queryString = "?" + ResourceRequest.POLICY_KEY + "=" + policyValue + "&" + ResourceRequest.SIGNATURE_KEY
            + "=" + signatureValue + "&" + ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId;

    List<NameValuePair> parameters = ResourceRequestUtil.parseQueryString(queryString);

    boolean foundOrg = false;
    boolean foundPolicy = false;
    boolean foundSignature = false;

    for (NameValuePair nameValuePair : parameters) {
      if (ResourceRequest.ENCRYPTION_ID_KEY.equals(nameValuePair.getName())) {
        assertEquals(keyId, nameValuePair.getValue());
        foundOrg = true;
      }
      if (ResourceRequest.POLICY_KEY.equals(nameValuePair.getName())) {
        assertEquals(policyValue, nameValuePair.getValue());
        foundPolicy = true;
      }
      if (ResourceRequest.SIGNATURE_KEY.equals(nameValuePair.getName())) {
        assertEquals(signatureValue, nameValuePair.getValue());
        foundSignature = true;
      }
    }

    assertTrue("Didn't find the organization value.", foundOrg);
    assertTrue("Didn't find the policy value.", foundPolicy);
    assertTrue("Didn't find the signature value.", foundSignature);
  }

  @Test
  public void testAuthenticateDuplicateProperties() {
    // Test duplicate query properties.
    String twoOrgs = ResourceRequest.ENCRYPTION_ID_KEY + "=org1&" + ResourceRequest.ENCRYPTION_ID_KEY + "=org2";
    // assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(twoOrgs, clientIp, null, null,
    // properties));
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(twoOrgs, clientIp, null, properties).getStatus());

    String twoPolicies = ResourceRequest.POLICY_KEY + "=policy1&" + ResourceRequest.POLICY_KEY + "=policy2";
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(twoPolicies, clientIp, null, properties).getStatus());

    String twoSignatures = ResourceRequest.SIGNATURE_KEY + "=signature1&" + ResourceRequest.SIGNATURE_KEY
            + "=signature1";
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(twoSignatures, clientIp, null, properties).getStatus());
  }

  @Test
  public void testAuthenticateMissingProperties() {
    // Test Missing query properties
    String missingOrg = ResourceRequest.POLICY_KEY + "=policy&" + ResourceRequest.SIGNATURE_KEY + "=signature";
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(missingOrg, clientIp, null, properties).getStatus());

    String missingPolicy = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.SIGNATURE_KEY
            + "=signature";
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(missingPolicy, clientIp, null, properties).getStatus());

    String missingSignature = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY
            + "=policy";
    assertEquals(Status.BadRequest,
            ResourceRequestUtil.resourceRequestfromQueryString(missingSignature, clientIp, null, properties)
                    .getStatus());
  }

  @Test
  public void testAuthenticatePolicyMatchesSignature() throws Exception {
    DateTime after = new DateTime(DateTimeZone.UTC);
    after = after.minus(2 * 60 * 60 * 1000L);
    DateTime before = new DateTime(DateTimeZone.UTC);
    before = before.plus(2 * 60 * 60 * 1000L);
    String nonMatchingResource = "http://other.com";
    Policy nonMatchingPolicy = new Policy(nonMatchingResource, before);
    String matchingResource = "http://mh-allinone/";
    Policy matchingPolicy = new Policy(matchingResource, before);
    String signature = PolicyUtils.getPolicySignature(matchingPolicy, key);

    // Test non-existant encryption key is forbidden.
    String wrongEncryptionKeyId = ResourceRequest.ENCRYPTION_ID_KEY + "=" + "WrongId" + "&"
            + ResourceRequest.POLICY_KEY + "=" + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&"
            + ResourceRequest.SIGNATURE_KEY + "=" + signature;
    assertEquals(
            Status.Forbidden,
            ResourceRequestUtil.resourceRequestfromQueryString(wrongEncryptionKeyId, clientIp, matchingResource,
                    properties).getStatus());

    // Test non matching resource results is forbidden.
    String nonMatching = ResourceRequest.ENCRYPTION_ID_KEY + "=organization&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(nonMatchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
            + signature;
    assertEquals(Status.Forbidden,
            ResourceRequestUtil.resourceRequestfromQueryString(nonMatching, clientIp, matchingResource, properties)
                    .getStatus());

    // Test non-matching client ip results in forbidden.
    Policy wrongClientPolicy = new Policy(matchingResource, before);
    wrongClientPolicy.setClientIpAddress("10.0.0.255");
    String wrongClient = ResourceRequestUtil.policyToResourceRequestQueryString(wrongClientPolicy, keyId, key);
    assertEquals(Status.Forbidden,
            ResourceRequestUtil.resourceRequestfromQueryString(wrongClient, clientIp, matchingResource, properties)
                    .getStatus());

    // Test matching client ip results in ok.
    Policy rightClientPolicy = new Policy(matchingResource, before);
    rightClientPolicy.setClientIpAddress(clientIp);
    String rightClient = ResourceRequestUtil.policyToResourceRequestQueryString(rightClientPolicy, keyId, key);
    assertEquals(Status.Ok,
            ResourceRequestUtil.resourceRequestfromQueryString(rightClient, clientIp, matchingResource, properties)
                    .getStatus());

    // Test not yet DateGreaterThan results in gone
    Policy wrongDateGreaterThanPolicy = new Policy(matchingResource, before);
    wrongDateGreaterThanPolicy.setDateGreaterThan(before);
    String wrongDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateGreaterThanPolicy,
            keyId, key);
    assertEquals(
            Status.Gone,
            ResourceRequestUtil.resourceRequestfromQueryString(wrongDateGreaterThan, clientIp, matchingResource,
                    properties).getStatus());

    // Test after DateGreaterThan results in ok
    Policy rightDateGreaterThanPolicy = new Policy(matchingResource, before);
    rightClientPolicy.setDateGreaterThan(after);
    String rightDateGreaterThan = ResourceRequestUtil.policyToResourceRequestQueryString(rightDateGreaterThanPolicy,
            keyId, key);
    assertEquals(
            Status.Ok,
            ResourceRequestUtil.resourceRequestfromQueryString(rightDateGreaterThan, clientIp, matchingResource,
                    properties).getStatus());

    // Test before DateLessThan results in gone
    Policy wrongDateLessThanPolicy = new Policy(matchingResource, after);
    String wrongDateLessThan = ResourceRequestUtil.policyToResourceRequestQueryString(wrongDateLessThanPolicy, keyId,
            key);
    assertEquals(
            Status.Gone,
            ResourceRequestUtil.resourceRequestfromQueryString(wrongDateLessThan, clientIp, matchingResource,
                    properties).getStatus());

    // Test matching results in ok.
    String matching = ResourceRequest.ENCRYPTION_ID_KEY + "=" + keyId + "&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "=" + signature;
    assertEquals(Status.Ok,
            ResourceRequestUtil.resourceRequestfromQueryString(matching, clientIp, matchingResource, properties)
                    .getStatus());
  }
}
