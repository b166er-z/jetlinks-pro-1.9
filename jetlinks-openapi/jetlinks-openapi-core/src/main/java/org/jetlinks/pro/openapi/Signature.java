package org.jetlinks.pro.openapi;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;

public enum Signature {
    MD5 {
        @Override
        public String sign(byte[] payload) {
            return DigestUtils.md5Hex(payload);
        }
        @Override
        public MessageDigest getMessageDigest() {
            return DigestUtils.getMd5Digest();
        }
    },
    SHA256 {
        @Override
        public String sign(byte[] payload) {
            return DigestUtils.sha256Hex(payload);
        }

        @Override
        public MessageDigest getMessageDigest() {
            return DigestUtils.getSha256Digest();
        }
    };

   public abstract MessageDigest getMessageDigest();

   public abstract String sign(byte[] payload);
}
