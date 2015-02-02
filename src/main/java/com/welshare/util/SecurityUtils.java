package com.welshare.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

public final class SecurityUtils {

    private static final String ISO_8859_1 = "ISO-8859-1";

    private static SecretKeySpec key;

    static {
        try {
            InputStream is = SecurityUtils.class.getResourceAsStream("/properties/key.txt");
            String encoded = IOUtils.readLines(is).iterator().next();
            byte[] bytes = Base64.decodeBase64(encoded.getBytes(ISO_8859_1));
            key = new SecretKeySpec(bytes, "AES");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SecurityUtils() { }

    public static String encrypt(String token) {
        if (token == null) {
            return null;
        }
        try {
            byte[] encrypted = encryptOrDecrypt(token.getBytes(ISO_8859_1), Cipher.ENCRYPT_MODE);
            return new String(Base64.encodeBase64(encrypted), ISO_8859_1);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Problem encrypting token " + token, ex);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static String decrypt(String encryptedToken) {
        if (encryptedToken == null) {
            return null;
        }
        try {
            byte[] decrypted = encryptOrDecrypt(Base64.decodeBase64(encryptedToken.getBytes(ISO_8859_1)), Cipher.DECRYPT_MODE);
            return new String(decrypted, ISO_8859_1);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Problem decrypting token " + encryptedToken, ex);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] encryptOrDecrypt(byte[] token, int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(mode, key);
        byte[] output = cipher.doFinal(token);

        return output;
    }
}
