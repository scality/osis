package com.scality.osis.vaultadmin.impl;

import com.amazonaws.Response;
import com.amazonaws.http.HttpResponse;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.scality.vaultclient.dto.ListAccountsRequestDTO;
import com.scality.vaultclient.dto.ListAccountsResponseDTO;
import org.mockito.stubbing.Answer;
import java.io.IOException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @Test
    public void listAccounts() throws Exception {

        ListAccountsRequestDTO listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        assertNotEquals(0, response.getAccounts().size());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith("cd_tenant_id%3D%3D"));
    }

    @Test
    public void listAccounts400ServiceResponse() throws Exception {

        ListAccountsRequestDTO listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    HttpResponse httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(400);
                    httpResponse.setStatusText("WrongFormat");
                    return new Response<>(null,httpResponse);
                });

        VaultServiceException e = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(400, e.status());
        assertEquals("WrongFormat", e.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void listAccounts500ServiceResponse() throws Exception {

        ListAccountsRequestDTO listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    HttpResponse httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(500);
                    httpResponse.setStatusText("ServiceFailure");
                    return new Response<>(null,httpResponse);
                });

        VaultServiceException e = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(500, e.status());
        assertEquals("ServiceFailure", e.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void VaultAdminImplInvalidEndpointTest() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, "dummy_endpoint"),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }
}
