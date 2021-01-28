/**
 * Copyright 2020 VMware, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.vmware.osis.scality.utils;

import com.scality.vaultclient.dto.AccountData;
import com.scality.vaultclient.dto.CreateAccountRequestDTO;
import com.scality.vaultclient.dto.CreateAccountResponseDTO;
import com.vmware.osis.model.*;
import com.vmware.osis.model.exception.BadRequestException;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.vmware.osis.scality.utils.ScalityConstants.*;

public final class ModelConverter {


    private ModelConverter() {
    }

    public static CreateAccountRequestDTO toScalityAccountRequest(OsisTenant osisTenant) {
        if(!osisTenant.getActive()){
//            Scality does not support inactive tenant/account creation
            throw new BadRequestException("Creating inactive tenant is not supported");
        }
        CreateAccountRequestDTO createAccountRequestDTO = new CreateAccountRequestDTO();
        // account name == tenant.Name__tenantid
        // account email address == [cdTenantIds]@osis.account.com ; max cdTenantIds accepted is 6
        // *** example email address 9b7e3259-aace-414c-bfd8-94daa0efefaf_9b7e3259-aace-414c-bfd8-94daa0efefaf@osis.account.com"
        if (StringUtils.isBlank(osisTenant.getTenantId())) {
            createAccountRequestDTO.setName(toScalityTenantId(osisTenant.getName(), osisTenant.getCdTenantIds().get(0)));
        } else {
            createAccountRequestDTO.setName(toScalityTenantId(osisTenant.getName(), osisTenant.getTenantId()));
        }

        createAccountRequestDTO.setEmailAddress(toScalityAccountEmail(osisTenant.getCdTenantIds()));

        return createAccountRequestDTO;
    }

    public static OsisTenant toOsisTenant(CreateAccountResponseDTO accountResponse) {
        AccountData account = accountResponse.getAccount().getData();
        return new OsisTenant()
                .name(toOsisTenantName(account.getName()))
                .active(true)
                .cdTenantIds(toOsisCDTenantIds(account.getEmailAddress()))
                .tenantId(toOsisTenantId(account));
    }

    public static String toOsisTenantName(String accountName) {
        String  name = accountName.substring(0, accountName.indexOf(DOUBLE_UNDER_SCORE));
        return name.replace(DOT, " ");
    }

    public static List<String> toOsisCDTenantIds(String email) {
        String ids = email.substring(0, email.indexOf(TENANT_EMAIL_SUFFIX));
        return Arrays.asList(ids.split(ARRAY_SEPARATOR));
    }

    public static String toScalityTenantId(String osisTenantName, String cdTenantId) {
        return toScalityTenantName(osisTenantName) + DOUBLE_UNDER_SCORE + ScalityUtil.normalize(cdTenantId);
    }

    public static String toOsisTenantId(AccountData account) {
        return account.getName().substring(account.getName().indexOf(DOUBLE_UNDER_SCORE)+2);
    }

    public static String toScalityAccountEmail(List<String> cdTenantIds) {
//        extracting only first 6 ids
        List<String> extractedCdTenantIds = cdTenantIds.stream().limit(MAX_ALLOWED_CD_TENANTID_COUNT).collect(Collectors.toList());
        //TODO add condition for character count

        return String.join(ARRAY_SEPARATOR, extractedCdTenantIds)
                + "@osis.account.com";
    }

    private static String toScalityTenantName(String name) {
        return name.replaceAll(" ", DOT);
    }
}
