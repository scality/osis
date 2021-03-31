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
    public void createAccount() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        final CreateAccountResponseDTO response = vaultAdminImpl.createAccount(createAccountRequestDTO);
        assertEquals(emailAddress, response.getAccount().getData().getEmailAddress());
        assertEquals(name, response.getAccount().getData().getName());
        assertNotNull(response.getAccount().getData().getArn());
        assertNotNull(response.getAccount().getData().getCreateDate());
        assertNotNull(response.getAccount().getData().getId());
        assertNotNull(response.getAccount().getData().getCanonicalId());
    }

    @Test
    public void createAccountErrorExistingEntity() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";

        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        loadExistingAccountErrorMocks();

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(409, exception.status());
        assertEquals("EntityAlreadyExists", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountIOException() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    final IOException ioException = new IOException("Invalid URL");
                    throw ioException;
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(500, exception.status());
        assertEquals("Exception", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void createAccountErrServiceResponse() {

        //vault specific email address format for ose-scality
        final String emailAddress = UUID.randomUUID().toString() +"_"+ UUID.randomUUID().toString() + "@osis.account.com";
        //vault specific name format for ose-scality
        final String name = "tenant.name__bfc0d4a51e06481cbc917e9d96e52d81";
        final CreateAccountRequestDTO createAccountRequestDTO = CreateAccountRequestDTO.builder()
                .emailAddress(emailAddress)
                .name(name)
                .build();

        when(accountServicesClient.createAccount(any(CreateAccountRequestDTO.class)))
                .thenAnswer((Answer<Response<CreateAccountResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(403);
                    httpResponse.setStatusText("Forbidden");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.createAccount(createAccountRequestDTO);
        });
        assertEquals(403, exception.status());
        assertEquals("Forbidden", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void listAccounts() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        final ListAccountsResponseDTO response = vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        assertNotEquals(0, response.getAccounts().size());
        assertFalse(response.getAccounts().get(0).getCustomAttributes().isEmpty());
        assertTrue(response.getAccounts().get(0)
                .getCustomAttributes().keySet()
                .stream().findFirst().get()
                .startsWith("cd_tenant_id%3D%3D"));
    }

    @Test
    public void listAccounts400ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(400);
                    httpResponse.setStatusText("WrongFormat");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(400, exception.status());
        assertEquals("WrongFormat", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void listAccounts500ServiceResponse() {

        final ListAccountsRequestDTO   listAccountsRequestDTO = ListAccountsRequestDTO.builder()
                .marker("0")
                .maxItems(5)
                .filterKeyStartsWith("cd_tenant_id%3D%3D")
                .build();

        when(accountServicesClient.listAccounts(any(ListAccountsRequestDTO.class)))
                .thenAnswer((Answer<Response<ListAccountsResponseDTO>>) invocation -> {
                    final HttpResponse   httpResponse = new HttpResponse(null, null);
                    httpResponse.setStatusCode(500);
                    httpResponse.setStatusText("ServiceFailure");
                    return new Response<>(null,httpResponse);
                });

        final VaultServiceException exception = assertThrows(VaultServiceException.class, () -> {
            vaultAdminImpl.listAccounts(listAccountsRequestDTO);
        });
        assertEquals(500, exception.status());
        assertEquals("ServiceFailure", exception.getMessage());

        //reinit the default mocks
        initMocks();
    }

    @Test
    public void testVaultAdminImplInvalidEndpoint() {
        assertThrows(IllegalArgumentException.class,
                () -> new VaultAdminImpl(accountServicesClient, "dummy_endpoint"),
                "VaultAdminImpl constructor should throw IllegalArgumentException for null endpoint");
    }
}
