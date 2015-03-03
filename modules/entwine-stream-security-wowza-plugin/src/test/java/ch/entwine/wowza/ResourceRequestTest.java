package ch.entwine.wowza;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ch.entwine.utils.PolicyUtils;
import ch.entwine.wowza.EntwineStreamSecurityWowzaPlugin.Status;

import org.apache.http.NameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;

public class ResourceRequestTest {
  private static final String key = "0123456789abcdef";
  private static final String clientIp = "10.0.0.1";

  @Test
  public void testQueryStringParsing() {
    String organizationValue = "mh_default_org";
    String policyValue = "{policy:'value'}";
    String signatureValue = "randomString";

    String queryString = "?" + ResourceRequest.POLICY_KEY + "=" + policyValue + "&" + ResourceRequest.SIGNATURE_KEY
            + "=" + signatureValue + "&" + ResourceRequest.ORGANIZATION_KEY + "=" + organizationValue;

    List<NameValuePair> parameters = ResourceRequest.parseQueryString(queryString);

    boolean foundOrg = false;
    boolean foundPolicy = false;
    boolean foundSignature = false;

    for (NameValuePair nameValuePair : parameters) {
      if (ResourceRequest.ORGANIZATION_KEY.equals(nameValuePair.getName())) {
        assertEquals(organizationValue, nameValuePair.getValue());
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
    String twoOrgs = ResourceRequest.ORGANIZATION_KEY + "=org1&" + ResourceRequest.ORGANIZATION_KEY + "=org2";
    assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(twoOrgs, clientIp, null, null));

    String twoPolicies = ResourceRequest.POLICY_KEY + "=policy1&" + ResourceRequest.POLICY_KEY + "=policy2";
    assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(twoPolicies, clientIp, null, null));

    String twoSignatures = ResourceRequest.SIGNATURE_KEY + "=signature1&" + ResourceRequest.SIGNATURE_KEY
            + "=signature1";
    assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(twoSignatures, clientIp, null, null));
  }

  @Test
  public void testAuthenticateMissingProperties() {
    // Test Missing query properties
    String missingOrg = ResourceRequest.POLICY_KEY + "=policy&" + ResourceRequest.SIGNATURE_KEY + "=signature";
    assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(missingOrg, clientIp, null, null));

    String missingPolicy = ResourceRequest.ORGANIZATION_KEY + "=organization&" + ResourceRequest.SIGNATURE_KEY
            + "=signature";
    assertEquals(Status.BadRequest, EntwineStreamSecurityWowzaPlugin.authenticate(missingPolicy, clientIp, null, null));

    String missingSignature = ResourceRequest.ORGANIZATION_KEY + "=organization&" + ResourceRequest.POLICY_KEY
            + "=policy";
    assertEquals(Status.BadRequest,
            EntwineStreamSecurityWowzaPlugin.authenticate(missingSignature, clientIp, null, null));
  }

  @Test
  public void testAuthenticatePolicyMatchesSignature() throws Exception {
    DateTime before = new DateTime(DateTimeZone.UTC);
    System.out.println(before);
    before = before.plus(2 * 60 * 60 * 1000L);
    System.out.println(before);
    String nonMatchingResource = "http://other.com";
    Policy nonMatchingPolicy = new Policy(nonMatchingResource, before);
    String matchingResource = "http://mh-allinone/";
    Policy matchingPolicy = new Policy(matchingResource, before);
    String signature = PolicyUtils.getPolicySignature(matchingPolicy, key);

    // Test non matching results is forbidden.
    String nonMatching = ResourceRequest.ORGANIZATION_KEY + "=organization&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(nonMatchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "="
            + signature;
    System.out.println("Non '" + nonMatching + "'");
    assertEquals(Status.Forbidden, new ResourceRequest(nonMatching, clientIp, matchingResource, key).getStatus());

    // Test matching results in ok.
    String matching = ResourceRequest.ORGANIZATION_KEY + "=organization&" + ResourceRequest.POLICY_KEY + "="
            + PolicyUtils.toBase64EncodedPolicy(matchingPolicy) + "&" + ResourceRequest.SIGNATURE_KEY + "=" + signature;
    assertEquals(Status.Ok, new ResourceRequest(matching, clientIp, matchingResource, key).getStatus());
  }
}
