/**
 *Copyright 2020 VMware, Inc.
 *SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.mockito.stubbing.Answer;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("ConstantConditions")
public class VaultAdminImplTest extends BaseTest {
    @Test
    public void createAccount() throws Exception {

        //vault specific email address format for ose-scality
        String email_address = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(email_address)
                .name(name)
                .build();

        CreateAccountResponseDTO response = vaultAdminImpl.createAccount(createAccountRequestDTO);
        assertEquals(email_address, response.getAccount().getData().getEmailAddress());
        assertEquals(name, response.getAccount().getData().getName());
        assertNotNull(response.getAccount().getData().getArn());
        assertNotNull(response.getAccount().getData().getCreateDate());
        assertNotNull(response.getAccount().getData().getId());
        assertNotNull(response.getAccount().getData().getCanonicalId());
    }

    @Test
    public void createAccountErrorExistingEntity() throws Exception {

        //vault specific email address format for ose-scality
        String email_address = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";

        CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(email_address)
                .name(name)
                .build();

        loadExistingAccountErrorMocks();

        VaultServiceException e = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(409, e.status());
        assertEquals("EntityAlreadyExists", e.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountIOException() throws Exception {

        //vault specific email address format for ose-scality
        String email_address = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(email_address)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    IOException e = new IOException("Invalid URL");
                    throw e;
                });

        VaultServiceException e = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(500, e.status());
        assertEquals("Exception", e.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountErrServiceResponse() throws Exception {

        //vault specific email address format for ose-scality
        String email_address = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(email_address)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    HttpResponse httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(403);
                    httpResponse.setStatusText("Forbidden");
                    return new Response<>(null,httpResponse);
                });

        VaultServiceException e = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(403, e.status());
        assertEquals("Forbidden", e.getMessage());

        //reinit the default mocks
        initMocks();
    }
}
