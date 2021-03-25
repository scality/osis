/**
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.utils;

import com.vmware.osis.model.OsisTenant;
import com.vmware.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.scality.osis.utils.ScalityConstants.*;

/**
 * Convert between OSIS and Scality models
 */
public final class ScalityModelConverter {


    private ScalityModelConverter() {
    }

    /* All Converter methods below converts OSIS model objects to Vault/Scality request formats */

    /**
     * Converts OSIS Tenant object to Vault Create Account request
     * @param osisTenant the osis tenant
     *
     * @return the create account request dto
     */
    public static CreateAccountRequestDTO toScalityCreateAccountRequest(OsisTenant osisTenant) {
        if(!osisTenant.getActive()){
//            Scality does not support inactive tenant creation
            throw new BadRequestException("Creating inactive tenant is not supported");
        }
        CreateAccountRequestDTO createAccountRequestDTO = new CreateAccountRequestDTO();

        if (!StringUtils.isEmpty(osisTenant.getTenantId())) {
            createAccountRequestDTO.setExternalAccountId(osisTenant.getTenantId());
        }
        createAccountRequestDTO.setName(osisTenant.getName());

        createAccountRequestDTO.setEmailAddress(generateTenantEmail(osisTenant.getName()));

        createAccountRequestDTO.setCustomAttributes(toScalityCustomAttributes(osisTenant.getCdTenantIds()));

        return createAccountRequestDTO;
    }

    /**
     * Generates tenant email string using tenant name.
     *  example email address: tenant.name@osis.scality.com
     *
     * @param name the name
     * @return the string
     */
    public static String generateTenantEmail(String name) {
        return name.replaceAll(" ", SEPARATOR) + TENANT_EMAIL_SUFFIX;
    }

    /**
     * Converts OSIS cdTenantIDs list to Vault Account customAttributes Map Format
     *
     * @param cdTenantIds the cd tenant ids list. Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     * @return the customAttributes map.
     *          Example: <code>{("cd_tenant_id%3D%3D9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *                          ("cd_tenant_id%3D%3D7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     */
    public static Map<String,String> toScalityCustomAttributes(List<String> cdTenantIds) {
        return cdTenantIds.stream()
                .collect(Collectors.toMap(str-> CD_TENANT_ID_PREFIX + str, str -> ""));
    }

    /* Converter methods below will convert Vault/Scality responses to OSIS model objects @param accountResponse the account response */


    /**
     * Converts Vault Create Account response to OSIS Tenant object
     *
     * @return the osis tenant
     */
    public static OsisTenant toOsisTenant(CreateAccountResponseDTO accountResponse) {
        AccountData account = accountResponse.getAccount().getData();
        return new OsisTenant()
                .name(account.getName())
                .active(true)
                .cdTenantIds(toOsisCDTenantIds(account.getCustomAttributes()))
                .tenantId(account.getId());
    }

    /**
     * Converts Vault Account customAttributes Map Format to OSIS cdTenantIDs list.
     *
     * @param customAttributes the customAttributes map.
     *      *          Example: <code>{("cd_tenant_id%3D%3D9b7e3259-aace-414c-bfd8-94daa0efefaf", ""),
     *      *                          ("cd_tenant_id%3D%3D7b7e3259-aace-414c-bfd8-94daa0efefaf", "")}</code>
     * @return the cd tenant ids list. Example:<code>["9b7e3259-aace-414c-bfd8-94daa0efefaf","7b7e3259-aace-414c-bfd8-94daa0efefaf"]</code>
     */
    public static List<String> toOsisCDTenantIds(Map<String,String> customAttributes) {
        if(customAttributes.size() == 0)
            return new ArrayList<>();

        List<String> cdTenantIds = new ArrayList<String>(customAttributes.keySet()).stream().collect(Collectors.toList());
        cdTenantIds.replaceAll(x -> StringUtils.removeStart(x, CD_TENANT_ID_PREFIX));
        return cdTenantIds;
    }
}
