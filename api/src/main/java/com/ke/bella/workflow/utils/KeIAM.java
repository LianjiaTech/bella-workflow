package com.ke.bella.workflow.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class KeIAM {
    private static final String ALGORITHM = "HmacSHA256";

    public static String generateAuthorization(String accessKeyId, String accessKeySecret,
            String nonce, String method, String path,
            String host, String query) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;

            List<String> stringToSignArray = new ArrayList<>();
            stringToSignArray.add("accessKeyId=" + accessKeyId);
            stringToSignArray.add("nonce=" + nonce);
            stringToSignArray.add("timestamp=" + timestamp);
            stringToSignArray.add("method=" + method);
            stringToSignArray.add("path=" + path);
            stringToSignArray.add("host=" + host);

            if(query != null && !query.isEmpty()) {
                String[] queryParts = query.split("&");
                List<String> sortedQuery = new ArrayList<>();
                Collections.addAll(sortedQuery, queryParts);
                Collections.sort(sortedQuery);
                stringToSignArray.add("query=" + String.join("&", sortedQuery));
            }

            Collections.sort(stringToSignArray);
            String stringToSign = String.join("&", stringToSignArray);

            String signature = hmacSha256(stringToSign, accessKeySecret);

            return "LJ-HMAC-SHA256 " + String.join("; ",
                    "accessKeyId=" + accessKeyId,
                    "nonce=" + nonce,
                    "timestamp=" + timestamp,
                    "signature=" + signature);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ke-IAM加签错误", e);
        }
    }

    private static String hmacSha256(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256Hmac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        sha256Hmac.init(secretKey);
        byte[] hmacData = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacData);
    }
}
