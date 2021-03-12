/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.platform.utils;

import com.scality.osis.model.OsisTenant;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.scality.osis.platform.utils.ScalityConstants.*;

/**
 * Convert between OSIS and Scality models
 */
public final class ModelConverter {


    private ModelConverter() {
    }

    public static CreateAccountRequestDTO toScalityAccountRequest(OsisTenant osisTenant) {
        if(!osisTenant.getActive()){
//            Scality does not support inactive tenant creation
            throw new BadRequestException("Creating inactive tenant is not supported");
        }
        CreateAccountRequestDTO createAccountRequestDTO = new CreateAccountRequestDTO();

        if (!StringUtils.isEmpty(osisTenant.getTenantId())) {
            createAccountRequestDTO.setExternalAccountId(osisTenant.getTenantId());
        }
        createAccountRequestDTO.setName(osisTenant.getName());

        // account email address == [cdTenantIds]@osis.account.com ; max cdTenantIds accepted is 6
        // *** example email address 9b7e3259-aace-414c-bfd8-94daa0efefaf_9b7e3259-aace-414c-bfd8-94daa0efefaf@osis.account.com"
        createAccountRequestDTO.setEmailAddress(toScalityAccountEmail(osisTenant.getCdTenantIds()));

        return createAccountRequestDTO;
    }

    public static OsisTenant toOsisTenant(CreateAccountResponseDTO accountResponse) {
        AccountData account = accountResponse.getAccount().getData();
        return new OsisTenant()
                .name(account.getName())
                .active(true)
                .cdTenantIds(toOsisCDTenantIds(account.getEmailAddress()))
                .tenantId(account.getId());
    }

    public static List<String> toOsisCDTenantIds(String email) {
        String ids = email.substring(0, email.indexOf(TENANT_EMAIL_SUFFIX));
        return Arrays.asList(ids.split(ARRAY_SEPARATOR));
    }

    public static String toScalityAccountEmail(List<String> cdTenantIds) {
//        extracting only first 6 ids
        List<String> extractedCdTenantIds = cdTenantIds.stream().limit(MAX_ALLOWED_CD_TENANTID_COUNT).collect(Collectors.toList());
        //TODO add condition for character count

        return String.join(ARRAY_SEPARATOR, extractedCdTenantIds)
                + "@osis.account.com";
    }
}
