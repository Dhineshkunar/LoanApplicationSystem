package com.loanapp.util;

import org.apache.logging.log4j.LogManager;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.logging.Logger;

public class StringEncrypter {

    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    public static final String DES_ENCRYPTION_SCHEME = "DES";
    public static final String DEFAULT_ENCRYPTION_KEY = "SolvEdgeKey@123SolvEdgeKey@123";

    private KeySpec keySpec;
    private SecretKeyFactory keyFactory;
    private Cipher cipher;

    public StringEncrypter(String encryptionScheme) throws EncryptionException {
        this(encryptionScheme, DEFAULT_ENCRYPTION_KEY);
    }

    public StringEncrypter(String encryptionScheme, String encryptionKey) throws EncryptionException {

        if (encryptionKey == null)
            throw new IllegalArgumentException("encryption key was null");
        if (encryptionKey.trim().length() < 24)
            throw new IllegalArgumentException("encryption key was less than 24 characters");

        try {
            byte[] keyAsBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);

            if (encryptionScheme.equals(DESEDE_ENCRYPTION_SCHEME)) {
                keySpec = new DESedeKeySpec(keyAsBytes);
            } else if (encryptionScheme.equals(DES_ENCRYPTION_SCHEME)) {
                keySpec = new DESKeySpec(keyAsBytes);
            } else {
                throw new IllegalArgumentException("Encryption scheme not supported: " + encryptionScheme);
            }

            keyFactory = SecretKeyFactory.getInstance(encryptionScheme);
            cipher = getCipher(encryptionScheme);

        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new EncryptionException(e);
        }
    }

    public String
    encrypt(String unencryptedString) throws EncryptionException {
        if (unencryptedString == null || unencryptedString.trim().length() == 0)
            throw new IllegalArgumentException("unencrypted string was null or empty");

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(unencryptedString);
            return new String(decodedBytes, StandardCharsets.UTF_8);
            /**SecretKey key = keyFactory.generateSecret(keySpec);
             cipher.init(Cipher.ENCRYPT_MODE, key);
             byte[] cleartext = unencryptedString.getBytes(StandardCharsets.UTF_8);
             byte[] ciphertext = cipher.doFinal(cleartext);
             return new String(Base64.getMimeEncoder().encode(ciphertext), StandardCharsets.UTF_8);**/
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    public String encryptt(String unencryptedString) throws EncryptionException {
        if (unencryptedString == null || unencryptedString.trim().length() == 0)
            throw new IllegalArgumentException("unencrypted string was null or empty");

        try {
            SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cleartext = unencryptedString.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(cleartext);
            return new String(Base64.getMimeEncoder().encode(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }


    public String decrypt(String encryptedString) throws EncryptionException {
        if (encryptedString == null || encryptedString.trim().length() <= 0)
            throw new IllegalArgumentException("encrypted string was null or empty");

        try {
            SecretKey key = keyFactory.generateSecret(keySpec);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] cleartext = Base64.getMimeDecoder().decode(encryptedString.trim());
            byte[] ciphertext = cipher.doFinal(cleartext);

            return bytes2String(ciphertext);
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    private static String bytes2String(byte[] bytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            stringBuffer.append((char) bytes[i]);
        }
        return stringBuffer.toString();
    }

    public static Cipher getCipher(String encryptionScheme) throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance(encryptionScheme);
    }

    @SuppressWarnings("serial")
    public static class EncryptionException extends Exception {
        public EncryptionException(Throwable t) {
            super(t);
        }
    }

    public static void main(String[] arg) throws Exception {
        StringEncrypter se = new StringEncrypter("DES");

        String userPassCode = "bgIK3QDBnMWPJPlGDlPARA==";
        String decUserName = se.decrypt(userPassCode);
        String decUserPass = se.encryptt("Demo@123");
        String encrypt = se.encrypt("RGVtb0AxMjM=");
        System.out.println(decUserName);
        System.out.println(decUserPass);
        System.out.println(encrypt);

    }

}
