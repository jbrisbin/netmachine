package com.jbrisbin.netmachine.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Base64;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public abstract class HashUtils {

  public static String str(byte[] b) {
    return Base64.encodeBase64String(b);
  }

  public static byte[] sha1(String... toHash) {
    return hash("SHA-1", toHash);
  }

  public static byte[] md5(String... toHash) {
    return hash("MD5", toHash);
  }

  public static byte[] hash(String algorithm, String... toHash) {
    try {
      MessageDigest md5 = MessageDigest.getInstance(algorithm);
      for (String s : toHash) {
        if (null != s)
          md5.update(s.getBytes());
      }
      return md5.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

}
