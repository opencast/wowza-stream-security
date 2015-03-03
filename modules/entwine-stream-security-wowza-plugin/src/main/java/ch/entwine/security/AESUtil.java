package ch.entwine.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utlity class to encrypt and decrypt data using AES.
 */
public class AESUtil {
  // TODO have dynamic initial vertices pass as a parameter
  private static String IV = "AAAAAAAAAAAAAAAA";

  /**
   * Encrypt a {@link String} with AES.
   * 
   * @param plainText
   *          The {@link String} to encrypt.
   * @param encryptionKey
   *          The key to use to encrypt the {@link String}
   * @return The encrypted {@link String} as a {@link byte[]}
   * @throws Exception
   *           Thrown if there was a problem encrypting the {@link String}
   */
  public static byte[] encrypt(String plainText, String encryptionKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
    SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
    return cipher.doFinal(plainText.getBytes("UTF-8"));
  }

  /**
   * Decrypt from a {@link byte[]} the original {@link String}
   * 
   * @param cipherText
   *          The encrypted bytes
   * @param encryptionKey
   *          The key used to encrypt the bytes
   * @return The plaintext {@link String}.
   * @throws Exception
   *           Thrown if there was a problem decrypting the bytes.
   */
  public static String decrypt(byte[] cipherText, String encryptionKey) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
    SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));
    return new String(cipher.doFinal(cipherText), "UTF-8");
  }
}
