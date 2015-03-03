package ch.entwine.utils;

import ch.entwine.security.AESUtil;
import ch.entwine.wowza.Policy;
import ch.entwine.wowza.ResourceRequest;

import com.wowza.util.Base64;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;
import java.util.TreeMap;

/**
 * A Utility class to encode / decode Policy files from and to Base 64 and Json.
 */
public class PolicyUtils {
  /** The JSON key for object that contains the date and ip conditions for the resource. */
  private static final String CONDITION_KEY = "Condition";
  /** The JSON key for the date and time the resource will become available. */
  private static final String DATE_GREATER_THAN_KEY = "DateGreaterThan";
  /** The JSON key for the date and time the resource will no longer be available. */
  private static final String DATE_LESS_THAN_KEY = "DateLessThan";
  /** The JSON key for the IP address of the acceptable client. */
  private static final String IP_ADDRESS_KEY = "IpAddress";
  /** The JSON key for the base url for the resource. */
  private static final String RESOURCE_KEY = "Resource";
  /** The JSON key for the main object of the policy. */
  private static final String STATEMENT_KEY = "Statement";

  /** Parser used to parse policy content */
  private static JSONParser jsonParser = new JSONParser();

  /**
   * Encode a {@link String} into Base 64 encoding
   * 
   * @param value
   *          The {@link String} to encode into base 64 encoding
   * @return The {@link String} encoded into base 64.
   */
  public static String base64Encode(String value) {
    return Base64.encodeBytes(value.getBytes(), Base64.URL_SAFE + Base64.DONT_BREAK_LINES);
  }

  /**
   * Decode a {@link String} from Base 64 encoding
   *
   * @param value
   *          The {@link String} to encode into Base 64
   * @return The {@link String} decoded from base 64.
   */
  public static String base64Decode(String value) {
    byte[] decodedBytes = Base64.decode(value, (Base64.URL_SAFE + Base64.DONT_BREAK_LINES));
    return new String(decodedBytes, ResourceRequest.charSet);
  }

  /**
   * Encode a byte[] into Base 64 encoding
   * 
   * @param value
   *          The byte[] to encode into base 64 encoding
   * @return The {@link String} encoded into base 64.
   */
  public static String base64Encode(byte[] value) {
    return Base64.encodeBytes(value, (Base64.URL_SAFE + Base64.DONT_BREAK_LINES));
  }

  /**
   * Get a {@link Policy} from JSON data.
   * 
   * @param policyJson
   *          The {@link String} representation of the json.
   * @return A new {@link Policy} object populated from the JSON.
   */
  public static Policy fromJson(String policyJson) {
    JSONObject jsonPolicy = null;
    try {
      jsonPolicy = (JSONObject) jsonParser.parse(policyJson);
    } catch (ParseException e) {
      e.printStackTrace();
    }
    JSONObject statement = (JSONObject) jsonPolicy.get(STATEMENT_KEY);
    String resource = statement.get(RESOURCE_KEY).toString();
    JSONObject condition = (JSONObject) statement.get(CONDITION_KEY);
    String lessThanString = condition.get(DATE_LESS_THAN_KEY).toString();
    DateTime dateLessThan = new DateTime(Long.parseLong(lessThanString), DateTimeZone.UTC);
    Policy policy = new Policy(resource, dateLessThan);

    Object greaterThanString = condition.get(DATE_GREATER_THAN_KEY);
    if (greaterThanString != null) {
      policy.setDateGreaterThan(new DateTime(Long.parseLong(greaterThanString.toString()), DateTimeZone.UTC));
    }

    Object ipAddress = condition.get(IP_ADDRESS_KEY);
    if (ipAddress != null) {
      policy.setClientIpAddress(ipAddress.toString());
    }
    return policy;
  }

  /**
   * Render a {@link Policy} into JSON.
   *
   * @param policy
   *          The {@link Policy} to render into JSON.
   * @return The {@link JSONObject} representation of the {@link Policy}.
   */
  @SuppressWarnings("unchecked")
  public static JSONObject toJson(Policy policy) {
    JSONObject policyJSON = new JSONObject();

    Map<String, Object> conditions = new TreeMap<String, Object>();
    conditions.put(DATE_LESS_THAN_KEY, new Long(policy.getDateLessThan().getMillis()));
    if (policy.getDateGreaterThan() != null) {
      conditions.put(DATE_GREATER_THAN_KEY, new Long(policy.getDateGreaterThan().getMillis()));
    }
    if (StringUtils.isNotBlank(policy.getClientIpAddress())) {
      conditions.put(IP_ADDRESS_KEY, policy.getClientIpAddress());
    }
    JSONObject conditionsJSON = new JSONObject();
    conditionsJSON.putAll(conditions);

    JSONObject statement = new JSONObject();
    statement.put(RESOURCE_KEY, policy.getResource());
    statement.put(CONDITION_KEY, conditions);

    policyJSON.put(STATEMENT_KEY, statement);

    return policyJSON;
  }

  /**
   * Create a {@link Policy} in Json format and Base 64 encoded.
   * 
   * @param encodedPolicy
   *          The String representation of the {@link Policy} in Json format and encoded into Base 64
   * @return The {@link Policy} data
   */
  public static Policy fromBase64EncodedPolicy(String encodedPolicy) {
    String decodedPolicyString = base64Decode(encodedPolicy);
    return fromJson(decodedPolicyString);
  }

  /**
   * Create a {@link Policy} in Json format and Base 64 encoded.
   * 
   * @param encodedPolicy
   *          The String representation of the {@link Policy} in Json format and encoded into Base 64
   * @return The {@link Policy} data
   */
  public static String toBase64EncodedPolicy(Policy policy) {
    return base64Encode(PolicyUtils.toJson(policy).toJSONString());
  }

  /**
   * Get an encrypted version of a {@link Policy} to use as a signature.
   * 
   * @param policy
   *          {@link Policy} that needs to be encrypted.
   * @param encryptionKey
   *          The key to use to encrypt the {@link Policy}.
   * @return An encrypted version of the {@link Policy} that is also Base64 encoded to make it safe to transmit as a
   *         query parameter.
   * @throws Exception
   *           Thrown if there is a problem encrypting or encoding the {@link Policy}
   */
  public static String getPolicySignature(Policy policy, String encryptionKey) throws Exception {
    return base64Encode(AESUtil.encrypt(PolicyUtils.toJson(policy).toJSONString(), encryptionKey));
  }
}
