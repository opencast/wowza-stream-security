package ch.entwine.wowza;

import ch.entwine.wowza.EntwineStreamSecurityWowzaPlugin.Status;

import java.nio.charset.Charset;

/**
 * Represents a request for a streaming resource whose signed url must be validated.
 */
public class ResourceRequest {
  /** The query string parameter key of the organization used to request resource. */
  public static String ENCRYPTION_ID_KEY = "keyId";
  /** The query string key representing the conditions to allow the resource to be seen. */
  public static String POLICY_KEY = "policy";
  /** The query string key representing the encrypted policy. */
  public static String SIGNATURE_KEY = "signature";
  /** The charset to encode with */
  public static Charset charSet = Charset.forName("UTF-8");

  /** The policy encoded in Base64 from the query string value. */
  private String encodedPolicy;
  /** The query string value that is an id to use to retrieve the encryption key from. */
  private String encryptionKeyId;
  /** The policy to determine if this resource should be allowed. */
  private Policy policy;
  /** The encrypted policy used to verify this is a valid requset. */
  private String signature;
  /** The status of whether this resource should be allowed to be shown. */
  private Status status = Status.Forbidden;

  /**
   * Create a new resource request
   */
  public ResourceRequest() {

  }

  public String getEncodedPolicy() {
    return encodedPolicy;
  }

  public void setEncodedPolicy(String encodedPolicy) {
    this.encodedPolicy = encodedPolicy;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public Policy getPolicy() {
    return policy;
  }

  public void setPolicy(Policy policy) {
    this.policy = policy;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
