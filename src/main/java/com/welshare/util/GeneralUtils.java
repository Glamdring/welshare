package com.welshare.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

import com.welshare.service.impl.shortening.DefaultUrlShorteningService;

public final class GeneralUtils {
    private GeneralUtils() {
        // preventing instantiation
    }

    public static String generateShortKey() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            UUID uuid = UUID.randomUUID();
            dos.writeLong(uuid.getMostSignificantBits());

            String encoded = encode(baos.toByteArray());
            // returns the leftmost X characters
            String shortUrlKey = StringUtils.left(encoded, Constants.SHORT_URL_KEY_LENGTH);
            return shortUrlKey;
        } catch (IOException e) {
            DefaultUrlShorteningService.logger.warn("Problem shortening URL with default shortener", e);
            return null;
        }
    }

    private static String encode(byte[] bytes) {
        try {
            String encoded = new String(Base64.encodeBase64(bytes), "ISO-8859-1");
            encoded = encoded.replaceAll("([=\\-+/])*", "");
            return encoded;
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Can't happen", ex);
        }
    }

    public static String generateShortKey(int length) {
        byte[] randomBytes = new byte[length];
        for (int i = 0; i < length; i++) {
            randomBytes[i] = (byte) RandomUtils.nextInt(256);
        }
        return StringUtils.left(encode(randomBytes), length);
    }
}
