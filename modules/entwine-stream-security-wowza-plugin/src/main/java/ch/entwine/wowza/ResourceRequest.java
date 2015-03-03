package ch.entwine.wowza;

import ch.entwine.utils.PolicyUtils;
import ch.entwine.wowza.EntwineStreamSecurityWowzaPlugin.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.nio.charset.Charset;
import java.util.List;

/**
 * Represents a request for a streaming resource whose signed url must be validated.
 */
public class ResourceRequest {
  /** The query string parameter key of the organization used to request resource. */
  public static String ORGANIZATION_KEY = "organization";
  /** The query string key representing the conditions to allow the resource to be seen. */
  public static String POLICY_KEY = "policy";
  /** The query string key representing the encrypted policy. */
  public static String SIGNATURE_KEY = "signature";
  /** The charset to encode with */
  public static Charset charSet = Charset.forName("UTF-8");

  /** The policy encoded in Base64 from the query string value. */
  private String encodedPolicy;
  /** The organization from the query string value. */
  private String organization;
  /** The policy to determine if this resource should be allowed. */
  private Policy policy;
  /** The encrypted policy used to verify this is a valid requset. */
  private String signature;
  /** The status of whether this resource should be allowed to be shown. */
  private Status status = Status.Forbidden;

  /**
   * Get a list of all of the query string parameters and their values.
   * 
   * @param queryString
   *          The query string to process.
   * @return A {@link List} of {@link NameValuePair} representing the query string parameters
   */
  protected static List<NameValuePair> parseQueryString(String queryString) {
    return URLEncodedUtils.parse(queryString.replaceFirst("\\?", ""), charSet);
  }

  /**
   * Determine if the policy matches the encrypted signature.
   * 
   * @param policy
   *          The policy to compare to the encrypted signature.
   * @param signature
   *          The encrypted policy that was sent.
   * @param encryptionKey
   *          The encryption key to use to encrypt the policy.
   * @return If the policy encrypted matches the signature.
   */
  protected static boolean policyMatchesSignature(Policy policy, String signature, String encryptionKey) {
    try {
      String encryptedPolicy = PolicyUtils.getPolicySignature(policy, encryptionKey);
      if (signature.equals(encryptedPolicy)) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      System.out.println("Unable to encrypt policy because " + ExceptionUtils.getStackTrace(e));
      return false;
    }
  }

  /**
   * Create a new resource request
   * 
   * @param queryString
   *          The query string for this request to determine its validity.
   * @param clientIP
   *          The IP of the client requesting the resource.
   * @param encryptionKey
   *          The encryption key to use to make sure this request is valid.
   */
  public ResourceRequest(String queryString, String clientIp, String resourceUri, String encryptionKey) {
    List<NameValuePair> queryParameters = parseQueryString(queryString);

    if(!getQueryStringParameters(queryParameters)) {
      return;
    }

    // Get Policy
    policy = PolicyUtils.fromBase64EncodedPolicy(encodedPolicy);

    // Check to make sure that the Policy & Signature match when encrypted using the private key. If they don't match
    // return a Forbidden 403.
    if (!policyMatchesSignature(policy, signature, encryptionKey)) {
      status = Status.Forbidden;
      return;
    }
    // If the IP address is specified, check it against the requestor's ip, if it doesn't match return a Forbidden 403.
    if (StringUtils.isNotBlank(policy.getClientIpAddress()) && !policy.getClientIpAddress().equalsIgnoreCase(clientIp)) {
      status = Status.Forbidden;
      return;
    }
    // If the resource value in the policy doesn't match the requested resource return a Forbidden 403.
    if (!policy.getResource().equals(resourceUri)) {
      status = Status.Forbidden;
      return;
    }
    // Check the dates of the policy to make sure that it is still valid. If it is no longer valid give an Gone return
    // value of 410.
    if (new DateTime(DateTimeZone.UTC).isAfter(policy.getDateLessThan().getMillis())) {
      status = Status.Gone;
      return;
    }
    if (policy.getDateGreaterThan() != null
            && new DateTime(DateTimeZone.UTC).isBefore(policy.getDateGreaterThan().getMillis())) {
      status = Status.Gone;
      return;
    }
    // If all of the above conditions pass, then allow the video to be played.
    status = Status.Ok;
  }

  /**
   * Get all of the necessary query string parameters.
   * 
   * @param queryParameters
   *          The query string parameters.
   * @return True if all of the mandatory query string parameters were provided.
   */
  private boolean getQueryStringParameters(List<NameValuePair> queryParameters) {
    for (NameValuePair nameValuePair : queryParameters) {
      if (ORGANIZATION_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(organization)) {
          organization = nameValuePair.getValue();
        } else {
          status = Status.BadRequest;
          return false;
        }
      }
      if (POLICY_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(encodedPolicy)) {
          encodedPolicy = nameValuePair.getValue();
        } else {
          status = Status.BadRequest;
          return false;
        }
      }
      if (SIGNATURE_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(signature)) {
          signature = nameValuePair.getValue();
        } else {
          status = Status.BadRequest;
          return false;
        }
      }
    }

    if (StringUtils.isBlank(organization) || StringUtils.isBlank(encodedPolicy) || StringUtils.isBlank(signature)) {
      status = Status.BadRequest;
      return false;
    }
    return true;
  }

  public Status getStatus() {
    return status;
  }
}
