package ch.entwine.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AESUtilTest {
  @Test
  public void testEncryptDecrypt() throws Exception {
    String plaintext = "test text 123 this is of a random length of size"; /* Note null padding */
    String encryptionKey = "0123456789abcdef";
    byte[] encrypted = AESUtil.encrypt(plaintext, encryptionKey);
    String decrypted = AESUtil.decrypt(encrypted, encryptionKey);
    assertEquals(plaintext, decrypted);
  }
}
