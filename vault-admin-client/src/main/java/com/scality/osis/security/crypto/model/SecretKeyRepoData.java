package com.scality.osis.security.crypto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SecretKeyRepoData {
    private String keyID;
    private CipherInformation cipherInfo;
    private byte[] encryptedBytes;
}
