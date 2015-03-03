package ch.entwine.utils;

import ch.entwine.wowza.EntwineStreamSecurityWowzaPlugin.Status;
import ch.entwine.wowza.Policy;
import ch.entwine.wowza.ResourceRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A utility class to transform ResourceRequests into query strings and back.
 */
public class ResourceRequestUtil {
  /**
   * Get a list of all of the query string parameters and their values.
   * 
   * @param queryString
   *          The query string to process.
   * @return A {@link List} of {@link NameValuePair} representing the query string parameters
   */
  protected static List<NameValuePair> parseQueryString(String queryString) {
    return URLEncodedUtils.parse(queryString.replaceFirst("\\?", ""), ResourceRequest.charSet);
  }

  /**
   * Get all of the necessary query string parameters.
   * 
   * @param queryParameters
   *          The query string parameters.
   * @return True if all of the mandatory query string parameters were provided.
   */
  private static boolean getQueryStringParameters(ResourceRequest resourceRequest, List<NameValuePair> queryParameters) {
    for (NameValuePair nameValuePair : queryParameters) {
      if (ResourceRequest.ENCRYPTION_ID_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getEncryptionKeyId())) {
          resourceRequest.setEncryptionKeyId(nameValuePair.getValue());
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          return false;
        }
      }
      if (ResourceRequest.POLICY_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getEncodedPolicy())) {
          resourceRequest.setEncodedPolicy(nameValuePair.getValue());
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          return false;
        }
      }
      if (ResourceRequest.SIGNATURE_KEY.equals(nameValuePair.getName())) {
        if (StringUtils.isBlank(resourceRequest.getSignature())) {
          resourceRequest.setSignature(nameValuePair.getValue());
        } else {
          resourceRequest.setStatus(Status.BadRequest);
          return false;
        }
      }
    }

    if (StringUtils.isBlank(resourceRequest.getEncodedPolicy())
            || StringUtils.isBlank(resourceRequest.getEncryptionKeyId())
            || StringUtils.isBlank(resourceRequest.getSignature())) {
      resourceRequest.setStatus(Status.BadRequest);
      return false;
    }
    return true;
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
   * Create a {@link ResourceRequest} from the necessary data encoded policy, encryptionKeyId and signature.
   * 
   * @param encodedPolicy
   *          The policy Base64 encoded.
   * @param encryptionKeyId
   *          The id of the encryption key used.
   * @param signature
   *          The policy encrypted using the key attached to the encryptionKeyId
   * @return A new {@link ResourceRequest} filled with the parameter data.
   */
  public static ResourceRequest createResourceRequest(String encodedPolicy, String encryptionKeyId, String signature) {
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.setEncodedPolicy(encodedPolicy);
    resourceRequest.setEncryptionKeyId(encryptionKeyId);
    resourceRequest.setSignature(signature);
    return resourceRequest;
  }

  /**
   * Transform a {@link Policy} into a {@link ResourceRequest} query string.
   * 
   * @param policy
   *          The {@link Policy} to use in the {@link ResourceRequest}
   * @param encryptionKeyId
   *          The id of the encryption key.
   * @param encryptionKey
   *          The actual encryption key.
   * @return A query string created from the policy.
   * @throws Exception
   *           Thrown if there is a problem encoding or encrypting the policy.
   */
  public static String policyToResourceRequestQueryString(Policy policy, String encryptionKeyId, String encryptionKey)
          throws Exception {
    ResourceRequest resourceRequest = new ResourceRequest();
    resourceRequest.setEncodedPolicy(PolicyUtils.toBase64EncodedPolicy(policy));
    resourceRequest.setEncryptionKeyId(encryptionKeyId);
    resourceRequest.setSignature(PolicyUtils.getPolicySignature(policy, encryptionKey));
    return resourceRequestToQueryString(resourceRequest);
  }

  /**
   * Transform a {@link ResourceRequest} into a query string.
   * 
   * @param resourceRequest
   *          The {@link ResourceRequest} to transform.
   * @return The query string version of the {@link ResourceRequest}
   */
  public static String resourceRequestToQueryString(ResourceRequest resourceRequest) {
    List<NameValuePair> queryStringParameters = new ArrayList<NameValuePair>();
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.POLICY_KEY, resourceRequest.getEncodedPolicy()));
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.ENCRYPTION_ID_KEY, resourceRequest
            .getEncryptionKeyId()));
    queryStringParameters.add(new BasicNameValuePair(ResourceRequest.SIGNATURE_KEY, resourceRequest.getSignature()));
    return URLEncodedUtils.format(queryStringParameters, ResourceRequest.charSet);
  }

  /**
   * @param queryString
   *          The query string for this request to determine its validity.
   * @param clientIP
   *          The IP of the client requesting the resource.
   * @param resourceUri
   *          The base uri for the resource.
   * @param encryptionKeys
   *          The available encryption key ids and their keys.
   */
  public static ResourceRequest resourceRequestfromQueryString(String queryString, String clientIp, String resourceUri,
          Properties encryptionKeys) {
    ResourceRequest resourceRequest = new ResourceRequest();
    List<NameValuePair> queryParameters = parseQueryString(queryString);

    if (!getQueryStringParameters(resourceRequest, queryParameters)) {
      return resourceRequest;
    }

    // Get the encryption key by its id.
    String encryptionKey = encryptionKeys.getProperty(resourceRequest.getEncryptionKeyId());
    if (StringUtils.isBlank(encryptionKey)) {
      resourceRequest.setStatus(Status.Forbidden);
      return resourceRequest;
    }

    // Get Policy
    Policy policy = PolicyUtils.fromBase64EncodedPolicy(resourceRequest.getEncodedPolicy());
    resourceRequest.setPolicy(policy);
    // Check to make sure that the Policy & Signature match when encrypted using the private key. If they don't match
    // return a Forbidden 403.
    if (!policyMatchesSignature(policy, resourceRequest.getSignature(), encryptionKey)) {
      resourceRequest.setStatus(Status.Forbidden);
      return resourceRequest;
    }
    // If the IP address is specified, check it against the requestor's ip, if it doesn't match return a Forbidden 403.
    if (StringUtils.isNotBlank(policy.getClientIpAddress()) && !policy.getClientIpAddress().equalsIgnoreCase(clientIp)) {
      resourceRequest.setStatus(Status.Forbidden);
      return resourceRequest;
    }
    // If the resource value in the policy doesn't match the requested resource return a Forbidden 403.
    if (!policy.getResource().equals(resourceUri)) {
      resourceRequest.setStatus(Status.Forbidden);
      return resourceRequest;
    }
    // Check the dates of the policy to make sure that it is still valid. If it is no longer valid give an Gone return
    // value of 410.
    if (new DateTime(DateTimeZone.UTC).isAfter(policy.getDateLessThan().getMillis())) {
      resourceRequest.setStatus(Status.Gone);
      return resourceRequest;
    }
    if (policy.getDateGreaterThan() != null
            && new DateTime(DateTimeZone.UTC).isBefore(policy.getDateGreaterThan().getMillis())) {
      resourceRequest.setStatus(Status.Gone);
      return resourceRequest;
    }
    // If all of the above conditions pass, then allow the video to be played.
    resourceRequest.setStatus(Status.Ok);
    return resourceRequest;
  }
}
