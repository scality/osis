/**
 * Copyright 2020 VMware, Inc.
 * Copyright 2021 Scality, Inc.
 * SPDX-License-Identifier: Apache License 2.0
 */

package com.scality.osis.resource;

import com.scality.osis.annotation.NotImplement;
import com.scality.osis.model.*;
import com.scality.osis.model.exception.BadRequestException;
import com.scality.osis.model.exception.NotImplementedException;
import com.scality.osis.service.ScalityOsisService;
import com.scality.osis.validation.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Tag(name = "Users and tenants", description = "Users and tenants management")
@RestController
public class ScalityOsisController {

    private static final Logger logger = LoggerFactory.getLogger(com.scality.osis.resource.ScalityOsisController.class);

    @Autowired
    private ScalityOsisService osisService;

    /**
     * POST /api/v1/tenants/{tenantId}/users/{userId}/s3credentials : Create S3
     * credential for the platform user
     * Operation ID: createCredential&lt;br&gt; Create S3 credential for the
     * platform user
     *
     * @param tenantId The ID of the tenant which the user belongs to (required)
     * @param userId   The ID of the user which the created S3 credential belongs to
     *                 (required)
     * @return S3 credential is created for the user (status code 201)
     * or Bad Request (status code 400)
     */
    @Operation(summary = "Create S3 credential for the platform user", description = "Operation ID: createCredential<br> Create S3 credential for the platform user",
            responses = {@ApiResponse(responseCode = "201", description = "S3 credential is created for the user",
                    content = @Content(schema = @Schema(implementation = OsisS3Credential.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request")})
    @PostMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}/s3credentials", produces = "application/json")
    public OsisS3Credential createCredential(
            @Parameter(description = "The ID of the tenant which the user belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of the user which the created S3 credential belongs to", required = true) @PathVariable("userId") String userId) {
        return osisService.createS3Credential(tenantId, userId);
    }

    /**
     * POST /api/v1/tenants : Create a tenant in the platform
     * Operation ID: createTenant&lt;br&gt; Create a tenant in the platform. The
     * platform decides whether to adopt the cd_tenand_id in request body as
     * tenant_id. This means the platform could generate new tenant_id by itself for
     * the new tenant. The tenant_id in request body is ignored.
     *
     * @param osisTenant Tenant to create in the platform (required)
     * @return A tenant is created (status code 201)
     * or Bad Request (status code 400)
     */
    @Operation(summary = "Create a tenant in the platform", description = "Operation ID: createTenant<br> Create a tenant in the platform. The platform decides whether to adopt the cd_tenand_id in request body as tenant_id. This means the platform could generate new tenant_id by itself for the new tenant. The tenant_id in request body is ignored.",
            responses = {@ApiResponse(responseCode = "201", description = "A tenant is created",
                    content = @Content(schema = @Schema(implementation = OsisTenant.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request")})
    @PostMapping(value = "/api/v1/tenants", produces = "application/json", consumes = "application/json")
    public OsisTenant createTenant(@Parameter(description = "Tenant to create in the platform", required = true) @Valid @RequestBody OsisTenant osisTenant) {
        return osisService.createTenant(osisTenant);
    }

    /**
     * POST /api/v1/tenants/{tenantId}/users : Create a user in the platform tenant
     * Operation ID: createUser&lt;br&gt; Create a user in the platform. The
     * platform decides whether to adopt the cd_user_id in request body as canonical
     * ID. This means the platform could generate new user_id by itself for the new
     * user. The user_id in request body is ignored.
     *
     * @param tenantId The ID of the tenant which the created user belongs to
     *                 (required)
     * @param osisUser User to create in the platform tenant. canonical_user_id is
     *                 ignored. (required)
     * @return A user is created (status code 201)
     * or Bad Request (status code 400)
     */
    @Operation(summary = "Create a user in the platform tenant", description = "Operation ID: createUser<br> Create a user in the platform. The platform decides whether to adopt the cd_user_id in request body as canonical ID. This means the platform could generate new user_id by itself for the new user. The user_id in request body is ignored.",
            responses = {@ApiResponse(responseCode = "201", description = "A user is created",
                    content = @Content(schema = @Schema(implementation = OsisUser.class))),
                    @ApiResponse(responseCode = "400", description = "Bad Request")})
    @PostMapping(value = "/api/v1/tenants/{tenantId}/users", produces = "application/json", consumes = "application/json")
    public OsisUser createUser(
            @Parameter(description = "The ID of the tenant which the created user belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "User to create in the platform tenant. canonical_user_id is ignored.", required = true) @Valid @RequestBody OsisUser osisUser) {
        logger.info("create user");
        osisUser.setTenantId(tenantId);
        return osisService.createUser(osisUser);
    }

    /**
     * DELETE /api/v1/s3credentials/{accessKey} : Delete the S3 credential of the
     * platform user
     * Operation ID: deleteCredential&lt;br&gt; Delete the S3 credential of the
     * platform user. Parameters tenant_id and tenant_id are always in request; the
     * platform decides whehter to use them.
     * Returned HTTP status codes : the S3 credential is deleted (status code 204) or The optional API is not implemented (status code 501)
     *
     * @param tenantId  The ID of the tenant which the user belongs to (required)
     * @param userId    The ID of the user which the deleted S3 credential belongs
     *                  to (required)
     * @param accessKey The access key of the S3 credential to delete (required)
     */
    @Operation(summary = "Delete the S3 credential of the platform user", description = "Operation ID: deleteCredential<br> Delete the S3 credential of the platform user. Parameters tenant_id and tenant_id are always in request; the platform decides whether to use them.",
            responses = {@ApiResponse(responseCode = "204", description = "The S3 credential is deleted")},
            tags = {"s3credential", "optional"})
    @DeleteMapping(value = "/api/v1/s3credentials/{accessKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCredential(
            @Parameter(description = "The ID of the tenant which the user belongs to") @RequestParam(value = "tenant_id", required = false) String tenantId,
            @Parameter(description = "The ID of the user which the deleted S3 credential belongs to") @RequestParam(value = "user_id", required = false) String userId,
            @NotNull @Parameter(description = "The access key of the S3 credential to delete", required = true) @PathVariable("accessKey") String accessKey) {
        osisService.deleteS3Credential(tenantId, userId, accessKey);
    }

    /**
     * DELETE /api/v1/tenants/{tenantId} : Delete a tenant in the platform
     * Operation ID: deleteTenant&lt;br&gt; Delete a tenant in the platform
     * Returned HTTP status codes : The tenant is deleted (status code 204) or The optional API is not implemented (status code 501)
     *
     * @param tenantId  Tenant ID of the tenant to delete (required)
     * @param purgeData Purge data when the tenant is deleted (optional, default to false)
     */
    @Operation(summary = "Delete a tenant in the platform", description = "Operation ID: deleteTenant<br> Delete a tenant in the platform",
            responses = {
                    @ApiResponse(responseCode = "204", description = "The tenant is deleted"),
                    @ApiResponse(responseCode = "501", description = "The optional API is not implemented")
            })
    @DeleteMapping(value = "/api/v1/tenants/{tenantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @NotImplement(name = ScalityOsisConstants.DELETE_TENANT_API_CODE)
    public void deleteTenant(
            @Parameter(description = "Tenant ID of the tenant to delete", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "Purge data when the tenant is deleted") @Valid @RequestParam(value = "purge_data", required = false, defaultValue = "true") Boolean purgeData) {
        osisService.deleteTenant(tenantId, purgeData);
    }

    /**
     * DELETE /api/v1/tenants/{tenantId}/users/{userId} : Delete the user in the
     * platform tenant
     * Operation ID: deleteUser&lt;br&gt; Delete the user in the platform tenant
     * Returned HTTP status codes : The user is deleted (status code 204)
     *
     * @param tenantId  The ID of the tenant which the deleted user belongs to
     *                  (required)
     * @param userId    The ID of the user to delete (required)
     * @param purgeData Purge data when the user is deleted (optional, default to
     *                  false)
     */
    @Operation(summary = "Delete the user in the platform tenant", description = "Operation ID: deleteUser<br> Delete the user in the platform tenant",
            responses = {@ApiResponse(responseCode = "204", description = "The user is deleted")},
            tags = {"user", "required"})
    @DeleteMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
            @Parameter(description = "The ID of the tenant which the deleted user belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of the user to delete", required = true) @PathVariable("userId") String userId,
            @Parameter(description = "Purge data when the user is deleted") @Valid @RequestParam(value = "purge_data", required = false, defaultValue = "true") Boolean purgeData) {
        osisService.deleteUser(tenantId, userId, purgeData);
    }

    /**
     * GET /api/v1/bucket-list : Get the bucket list of the platform tenant
     * Operation ID: getBucketList&lt;br&gt; Get the bucket list of the platform
     * tenant
     *
     * @param tenantId The ID of the tenant to get its bueckt list (required)
     * @param offset   The start index of buckets to return (optional)
     * @param limit    The maximum number of buckets to return (optional)
     * @return The bucket list of the platform tenant is returned (status code 200)
     * or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Get the bucket list of the platform tenant", description = "Operation ID: getBucketList<br> Get the bucket list of the platform tenant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The bucket list of the platform tenant is returned",
                            content = @Content(schema = @Schema(implementation = PageOfOsisBucketMeta.class)))
            },
            tags = {"console", "optional"})
    @GetMapping(value = "/api/v1/bucket-list", produces = "application/json")
    public PageOfOsisBucketMeta getBucketList(
            @NotNull @Parameter(description = "The ID of the tenant to get its bucket list", required = true) @Valid @RequestParam(value = "tenant_id", required = true) String tenantId,
            @Parameter(description = "The start index of buckets to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") long offset,
            @Parameter(description = "The maximum number of buckets to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") long limit) {
        return osisService.getBucketList(tenantId, offset, limit);
    }

    /**
     * GET /api/v1/console : Get the console URI of the platform or platform tenant
     * Operation ID: getConsole&lt;br&gt; Get the console URI of the platform or
     * platform tenant if tenantId is specified
     *
     * @param tenantId The ID of the tenant to get its console URI (optional)
     * @return The console URI is returned (status code 200)
     * or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Get the console URI of the platform or platform tenant", description = "Operation ID: getConsole<br> Get the console URI of the platform or platform tenant if tenantId is specified",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The console URI is returned"),
                    @ApiResponse(responseCode = "501", description = "The optional API is not implemented"),
            },
            tags = {"console", "optional"})
    @GetMapping(value = "/api/v1/console", produces = "application/json")
    public String getConsole(@Parameter(description = "The ID of the tenant to get its console URI") @Valid @RequestParam(value = "tenant_id", required = false) Optional<String> tenantId) {
        if (tenantId.isPresent()) {
            return osisService.getTenantConsoleUrl(tenantId.get());
        } else {
            return osisService.getProviderConsoleUrl();
        }
    }

    /**
     * GET /api/v1/s3credentials/{accessKey} : Get S3 credential of the platform
     * user
     * Operation ID: createCredential&lt;br&gt; Get S3 credential of the platform
     * user. Parameters tenant_id and tenant_id are always in request; the platform
     * decides whehter to use them.
     *
     * @param tenantId  The ID of the tenant which the user belongs to (required)
     * @param userId    The ID of the user which the S3 credential belongs to
     *                  (required)
     * @param accessKey The access key of the S3 credential to get (required)
     * @return The S3 credenital is returned (status code 200)
     * or Not Found (status code 404)
     */
    @Operation(summary = "Get S3 credential of the platform user", description = "Operation ID: createCredential<br> Get S3 credential of the platform user. Parameters tenant_id and tenant_id are always in request; the platform decides whehter to use them",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The S3 credential is returned"),
                    @ApiResponse(responseCode = "404", description = "Not Found"),
            },
            tags = {"s3credential", "required"})
    @GetMapping(value = "/api/v1/s3credentials/{accessKey}", produces = "application/json")
    public OsisS3Credential getCredential(
            @Parameter(description = "The ID of the tenant which the user belongs to") @Valid @RequestParam(value = "tenant_id", required = false) String tenantId,
            @Parameter(description = "The ID of the user which the S3 credential belongs to") @Valid @RequestParam(value = "user_id", required = false) String userId,
            @NotNull @Parameter(description = "The access key of the S3 credential to get", required = true) @PathVariable("accessKey") String accessKey) {
        return osisService.getS3Credential(tenantId, userId, accessKey);
    }

    /**
     * GET /api/info : Get the REST servcies information
     * Operation ID: getInfo&lt;br&gt; &#39;Get the information of the REST
     * Services, including platform name, OSIS version and etc&#39;
     *
     * @return OK (status code 200)
     */
    @Operation(summary = "Get the REST services information", description = "Operation ID: getInfo<br> Get the information of the REST Services, including platform name, OSIS version and etc",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "404", description = "Not Found")
            },
            tags = {"info", "required"})
    @GetMapping(value = "/api/info", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Information getInfo(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String domain = url.substring(0, url.lastIndexOf(request.getRequestURI()));
        logger.info(domain);
        return osisService.getInformation(domain);
    }

    /**
     * GET /api/v1/s3capabilities : Get S3 capabilities of the platform
     * Operation ID: getS3Capabilities&lt;br&gt; Get S3 capabilities of the platform
     *
     * @return S3 capabilities of the platform (status code 200)
     */
    @Operation(summary = "Get S3 capabilities of the platform", description = "Operation ID: getS3Capabilities<br> Get S3 capabilities of the platform",
            responses = {
                    @ApiResponse(responseCode = "200", description = "S3 capabilities of the platform")
            },
            tags = {"usage", "required"})
    @GetMapping(value = "/api/v1/s3capabilities", produces = "application/json")
    public OsisS3Capabilities getS3Capabilities() {
        return osisService.getS3Capabilities();
    }

    /*
     * GET /api/v1/tenants/{tenantId} : Get the tenant
     * Operation ID: getTenant&lt;br&gt; Get the tenant with tenant ID. The
     * cd_tenant_id in the response indicates the mapping between Cloud Direct
     * tenant and platform tenant. \&quot;
     *
     * @param tenantId Tenant ID to get the tenant from the platform (required)
     * @return The tenant is returned (status code 200)
     *         or The tenant doesn&#39;t exist (status code 404)
     *         or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Get the tenant", description = "Operation ID: getTenant<br> Get the tenant with tenant ID. The cd_tenant_id in the response indicates the mapping between Cloud Direct tenant and platform tenant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The tenant is returned"),
                    @ApiResponse(responseCode = "404", description = "The tenant doesn't exist")
            },
            tags = {"usage", "required"})
    @GetMapping(value = "/api/v1/tenants/{tenantId}", produces = "application/json")
    public OsisTenant getTenant(
            @NotNull @Parameter(description = "Tenant ID to get the tenant from the platform", required = true) @PathVariable("tenantId") String tenantId) {
        return osisService.getTenant(tenantId);
    }

    /*
     * GET /api/v1/usage : Get the usage of the platform tenant or user
     * Operation ID: getUsage&lt;br&gt; Get the platform usage of global (without
     * query parameter), tenant (with tenant_id) or user (only with user_id).
     *
     * @param tenantId The ID of the tenant to get its usage. &#39;tenant_id&#39;
     *                 takes precedence over &#39;user_id&#39; to take effect if
     *                 both are specified. (optional)
     * @param userId   The ID of the user to get its usage. &#39;tenant_id&#39;
     *                 takes precedence over &#39;user_id&#39; to take effect if
     *                 both are specified. (optional)
     * @return The usage of the tenant or user is returned (status code 200)
     *         or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Get the usage of the platform tenant or user", description = "Operation ID: getUsage<br> Get the platform usage of global (without query parameter), tenant (with tenant_id) or user (only with user_id)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The usage of the tenant or user is returned"),
                    @ApiResponse(responseCode = "501", description = "The optional API is not implemented")
            },
            tags = {"usage", "optional"})
    @GetMapping(value = "/api/v1/usage", produces = "application/json")
    public OsisUsage getUsage(
            @Parameter(description = "The ID of the tenant to get its usage. 'tenant_id' takes precedence over 'user_id' to take effect if both are specified.") @Valid @RequestParam(value = "tenant_id", required = false) Optional<String> tenantId,
            @Parameter(description = "The ID of the user to get its usage. 'tenant_id' takes precedence over 'user_id' to take effect if both are specified.") @Valid @RequestParam(value = "user_id", required = false) Optional<String> userId) {
        if (!tenantId.isPresent() && userId.isPresent()) {
            throw new BadRequestException("userId must be specified with associated tenantId!");
        }
        return osisService.getOsisUsage(tenantId, userId);
    }

    /*
     * GET /api/v1/users/{canonicalUserId} : Get the user with user canonical ID
     * Operation ID: getUserWithCanonicalID&lt;br&gt; Get the user with the user
     * canonical ID
     *
     * @param canonicalUserId The canonical ID of the user to get (required)
     * @return The user is returned (status code 200)
     *         or The tenant doesn&#39;t exist (status code 404)
     */
    @Operation(summary = "Get the user with user canonical ID", description = "Operation ID: getUserWithCanonicalID<br> Get the user with the user canonical ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The user is returned"),
                    @ApiResponse(responseCode = "404", description = "The tenant doesn't exist")
            },
            tags = {"user", "required"})
    @GetMapping(value = "/api/v1/users/{canonicalUserId}", produces = "application/json")
    public OsisUser getUserWithCanonicalID(
            @Parameter(description = "The canonical ID of the user to get", required = true) @PathVariable("canonicalUserId") String canonicalUserId) {
        return osisService.getUser(canonicalUserId);
    }

    /*
     * GET /api/v1/tenants/{tenantId}/users/{userId} : Get the user with user ID of
     * the tenant
     * Operation ID: getUserWithId&lt;br&gt; Get the user with the user ID in the
     * tenant. The cd_user_id in the response indicates the mapping between Cloud
     * Direct user and platform user.
     *
     * @param tenantId The ID of the tenant which the user belongs to (required)
     * @param userId   The ID of the user to get (required)
     * @return The user is returned (status code 200)
     *         or The tenant doesn&#39;t exist (status code 404)
     */
    @Operation(summary = "Get the user with user ID of the tenant", description = "Operation ID: getUserWithId<br> Get the user with the user ID in the tenant. The cd_user_id in the response indicates the mapping between Cloud Direct user and platform user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The user is returned"),
                    @ApiResponse(responseCode = "404", description = "The tenant doesn't exist")
            },
            tags = {"user", "required"})
    @GetMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}", produces = "application/json")
    public OsisUser getUserWithId(
            @Parameter(description = "The ID of the tenant which the user belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of the user to get", required = true) @PathVariable("userId") String userId) {
        return osisService.getUser(tenantId, userId);
    }

    /*
     * HEAD /api/v1/tenants/{tenantId} : Check whether the tenant exists
     * Operation ID: headTenant&lt;br&gt; Check whether the tenant exists
     *
     * @param tenantId Tenant ID to check on the platform (required)
     * @return The tenant exists (status code 200)
     *         or The tenant doesn&#39;t exist (status code 404)
     */
    @Operation(summary = "Check whether the tenant exists", description = "Operation ID: headTenant<br> Check whether the tenant exists",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The tenant exists"),
                    @ApiResponse(responseCode = "404", description = "The tenant doesn't exist")
            },
            tags = {"tenant", "required"})
    @RequestMapping(value = "/api/v1/tenants/{tenantId}", method = RequestMethod.HEAD)
    public Void headTenant(@Parameter(description = "Tenant ID to check on the platform", required = true) @PathVariable("tenantId") String tenantId) {
        osisService.headTenant(tenantId);
        return null;
    }

    /*
     * HEAD /api/v1/tenants/{tenantId}/users/{userId} : Check whether the user
     * exists
     * Operation ID: headUser&lt;br&gt; Check whether the user exists in the
     * platform tenant
     *
     * @param tenantId The ID of the tenant which the user belongs to (required)
     * @param userId   The ID of the user to check (required)
     * @return The user exists (status code 200)
     *         or The user doesn&#39;t exist (status code 404)
     *         or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Check whether the user exists", description = "Operation ID: headUser<br> Check whether the user exists in the platform tenant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The user exists"),
                    @ApiResponse(responseCode = "404", description = "The user doesn't exist")
            },
            tags = {"user", "required"})
    @RequestMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}", method = RequestMethod.HEAD)
    public boolean headUser(
            @Parameter(description = "The ID of the tenant which the user belongs to", required = true)
            @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of the user to check", required = true)
            @PathVariable("userId") String userId) {
        return osisService.headUser(tenantId, userId);
    }

    /*
     * GET /api/v1/tenants/{tenantId}/users/{userId}/s3credentials : List S3
     * credentials of the platform user
     * Operation ID: listCredentials&lt;br&gt; List S3 credentials of the platform
     * user
     *
     * @param tenantId The ID of the tenant which the user belongs to (required)
     * @param userId   The ID of user which the S3 credenitials belong to (required)
     * @param offset   The start index of credentials to return (optional)
     * @param limit    Maximum number of credentials to return (optional)
     * @return S3 credentials of the platform user are returned (status code 200)
     */
    @Operation(summary = "List S3 credentials of the platform user", description = "Operation ID: listCredentials<br> List S3 credentials of the platform user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "S3 credentials of the platform user are returned",
                            content = @Content(schema = @Schema(implementation = PageOfS3Credentials.class))
                    )
            },
            tags = {"s3credential", "required"})
    @GetMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}/s3credentials", produces = "application/json")
    public PageOfS3Credentials listCredentials(
            @Parameter(description = "The ID of the tenant which the user belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of user which the S3 credenitials belong to", required = true) @PathVariable("userId") String userId,
            @Parameter(description = "The start index of credentials to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") Long offset,
            @Parameter(description = "Maximum number of credentials to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") Long limit) {

        return osisService.listS3Credentials(tenantId, userId, offset, limit);

    }

    /*
     * GET /api/v1/tenants : List tenants of platform
     * Operation ID: listTenants&lt;br&gt; List tenants of the platform
     *
     * @param offset The start index of tenants to return (optional)
     * @param limit  Maximum number of tenants to return (optional)
     * @return Tenants of the platform are returned (status code 200)
     */
    @Operation(summary = "List tenants of platform", description = "Operation ID: listTenants<br> List tenants of the platform",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tenants of the platform are returned",
                            content = @Content(schema = @Schema(implementation = PageOfTenants.class))
                    )
            },
            tags = {"tenant", "required"})
    @GetMapping(value = "/api/v1/tenants", produces = "application/json")
    public PageOfTenants listTenants(
            @Parameter(description = "The start index of tenants to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") Long offset,
            @Parameter(description = "Maximum number of tenants to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") Long limit) {
        return osisService.listTenants(offset, limit);
    }

    /*
     * GET /api/v1/tenants/{tenantId}/users : List users of the platform tenant
     * Operation ID: listUsers&lt;br&gt; List users of the platform tenant
     *
     * @param tenantId The ID of the tenant which the listed users belongs to
     *                 (required)
     * @param offset   The start index of users to return (optional)
     * @param limit    Maximum number of users to return (optional)
     * @return Users of the platform tenant are returned (status code 200)
     */
    @Operation(summary = "List users of the platform tenant", description = "Operation ID: listUsers<br> List users of the platform tenant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users of the platform tenant are returned",
                            content = @Content(schema = @Schema(implementation = PageOfUsers.class))
                    )
            },
            tags = {"user", "required"})
    @GetMapping(value = "/api/v1/tenants/{tenantId}/users", produces = "application/json")
    public PageOfUsers listUsers(
            @Parameter(description = "The ID of the tenant which the listed users belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The start index of users to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") long offset,
            @Parameter(description = "Maximum number of users to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") long limit) {
        return osisService.listUsers(tenantId, offset, limit);
    }

    /*
     * PATCH /api/v1/s3credentials/{accessKey} : Enable or disable S3 credential for
     * the platform user
     * Operation ID: updateCredentialStatus&lt;br&gt; Enabled or disable S3
     * credential for the platform user. Parameters tenant_id and tenant_id are
     * always in request; the platform decides whehter to use them.
     *
     * @param tenantId         The ID of the tenant which the user belongs to
     *                         (required)
     * @param userId           The ID of the user which the status updated S3
     *                         credential belongs to (required)
     * @param accessKey        The access key of the S3 credential to update status
     *                         (required)
     * @param osisS3Credential The S3 credential containing the status to update.
     *                         Only property &#39;active&#39; takes effect
     *                         (required)
     * @return The status of the S3 credential is updated (status code 200)
     *         or Bad Request (status code 400)
     *         or The optional API is not implemented (status code 501)
     */
    @Operation(summary = "Enable or disable S3 credential for the platform user", description = "Operation ID: updateCredentialStatus<br> Enabled or disable S3 credential for the platform user. Parameters tenant_id and tenant_id are always in request; the platform decides whether to use them",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The status of the S3 credential is updated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request",
                            content = @Content(schema = @Schema(implementation = Error.class)))
            },
            tags = {"user", "required"})
    @PatchMapping(value = "/api/v1/s3credentials/{accessKey}", produces = "application/json", consumes = "application/json")
    public OsisS3Credential updateCredentialStatus(
            @Parameter(description = "The ID of the tenant which the user belongs to") @RequestParam(value = "tenant_id", required = false) String tenantId,
            @Parameter(description = "The ID of the user which the status updated S3 credential belongs to") @RequestParam(value = "user_id", required = false) String userId,
            @NotNull @Parameter(description = "The access key of the S3 credential to update status", required = true) @PathVariable("accessKey") String accessKey,
            @Parameter(description = "The S3 credential containing the status to update. Only property 'active' takes effect", required = true) @RequestBody OsisS3Credential osisS3Credential) {
        return osisService.updateCredentialStatus(tenantId, userId, accessKey, osisS3Credential);
    }

    /*
     * PATCH /api/v1/tenants/{tenantId} : Enable or disable tenant of the platform
     * Operation ID: updateTenantStatus&lt;br&gt; Update status of the tenant in the
     * platform
     *
     * @param tenantId   Tenant ID of the tenant to update status (required)
     * @param osisTenant Tenant status to update in the platform. Only property
     *                   &#39;active&#39; takes effect (required)
     * @return The tenant status is updated (status code 200)
     *         or Bad Request (status code 400)
     */
    @Operation(summary = "Enable or disable tenant of the platform", description = "Operation ID: updateTenantStatus<br> Update status of the tenant in the platform",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The tenant status is updated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request",
                            content = @Content(schema = @Schema(implementation = Error.class)))
            },
            tags = {"tenant", "required"})
    @PatchMapping(value = "/api/v1/tenants/{tenantId}", produces = "application/json", consumes = "application/json")
    public OsisTenant updateTenantStatus(
            @Parameter(description = "Tenant ID of the tenant to update status", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "Tenant status to update in the platform. Only property 'active' takes effect", required = true) @Valid @RequestBody OsisTenant osisTenant) {
        return osisService.updateTenant(tenantId, osisTenant);
    }

    /*
     * PATCH /api/v1/tenants/{tenantId}/users/{userId} : Enable or disable status in
     * the tenant
     * Operation ID: updateUserStatus&lt;br&gt; Update status of the user in the
     * platform tenant
     *
     * @param tenantId The ID of the tenant which the user to update belongs to
     *                 (required)
     * @param userId   The ID of the user to update (required)
     * @param osisUser User status to update in the platform tenant. Only property
     *                 &#39;active&#39; takes effect (required)
     * @return The user status is updated (status code 201)
     *         or Bad Request (status code 400)
     */
//    @ApiOperation(value = "Enable or disable status in the tenant", nickname = "updateUserStatus", notes = "Operation ID: updateUserStatus<br> Update status of the user in the platform tenant ", response = OsisUser.class, authorizations = {
//            @Authorization(value = "basicAuth")
//    }, tags = { "user", "required", })
//    @ApiResponses(value = {
//            @ApiResponse(code = 201, message = "The user status is updated", response = OsisUser.class),
//            @ApiResponse(code = 400, message = "Bad Request", response = Error.class) })
//    @ApiImplicitParams({
//    })

    @Operation(summary = "Enable or disable status in the tenant", description = "Operation ID: updateUserStatus<br> Update status of the user in the platform tenant",
            responses = {
                    @ApiResponse(responseCode = "201", description = "The user status is updated"),
                    @ApiResponse(responseCode = "400", description = "Bad Request",
                            content = @Content(schema = @Schema(implementation = Error.class)))
            },
            tags = {"user", "required"})
    @PatchMapping(value = "/api/v1/tenants/{tenantId}/users/{userId}", produces = "application/json", consumes = "application/json")
    public OsisUser updateUserStatus(
            @Parameter(description = "The ID of the tenant which the user to update belongs to", required = true) @PathVariable("tenantId") String tenantId,
            @Parameter(description = "The ID of the user to update", required = true) @PathVariable("userId") String userId,
            @Parameter(description = "User status to update in the platform tenant. Only property 'active' takes effect", required = true) @Validated(value = Update.class) @RequestBody OsisUser osisUser) {
        return osisService.updateUser(tenantId, userId, osisUser);
    }

    /*
     * GET /api/v1/tenants/query : Query tenants of platform
     * Operation ID: queryTenants&lt;br&gt; Query tenants of the platform
     *
     * @param offset The start index of tenants to return (optional)
     * @param limit  Maximum number of tenants to return (optional)
     * @param filter The conditions to query platform tenants (optional)
     * @return Tenants of the platform are returned (status code 200)
     */
    @Operation(summary = "Query tenants of platform", description = "Operation ID: queryTenants<br> Query tenants of the platform",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tenants of the platform are returned")
            },
            tags = {"tenant", "required"})
    @GetMapping(value = "/api/v1/tenants/query", produces = "application/json")
    public PageOfTenants queryTenants(
            @Parameter(description = "The start index of tenants to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") long offset,
            @Parameter(description = "Maximum number of tenants to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") long limit,
            @Parameter(description = "The conditions to query platform tenants") @Valid @RequestParam(value = "filter", required = false) String filter) {
        return osisService.queryTenants(offset, limit, filter);
    }

    /*
     * GET /api/v1/users/query : Query users of the platform tenant
     * Operation ID: queryUsers&lt;br&gt; Query users of the platform tenant
     *
     * @param offset The start index of users to return (optional)
     * @param limit  Maximum number of users to return (optional)
     * @param filter The conditions to query platform users (optional)
     * @return Users of the platform tenant are returned (status code 200)
     */
    @Operation(summary = "Query users of the platform tenant", description = "Operation ID: queryUsers<br> Query users of the platform tenant",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users of the platform tenant are returned")
            },
            tags = {"user", "required"})
    @GetMapping(value = "/api/v1/users/query", produces = "application/json")
    public PageOfUsers queryUsers(
            @Parameter(description = "The start index of users to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") long offset,
            @Parameter(description = "Maximum number of users to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") long limit,
            @Parameter(description = "The conditions to query platform users") @Valid @RequestParam(value = "filter", required = false) String filter) {

        return osisService.queryUsers(offset, limit, filter);
    }

    /*
     **
     * GET /api/v1/s3credentials/query : Query S3 credentials of the platform user
     * Operation ID: queryCredentials&lt;br&gt; Query S3 credentials of the platform
     * user
     *
     * @param offset The start index of credentials to return (optional)
     * @param limit  Maximum number of credentials to return (optional)
     * @param filter The conditions to query platform users (optional)
     * @return S3 credentials of the platform user are returned (status code 200)
     *
     */
    @Operation(summary = "Query S3 credentials of the platform user", description = "Operation ID: queryCredentials<br> Query S3 credentials of the platform user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "S3 credentials of the platform user are returned")
            },
            tags = {"s3credential", "required"})
    @GetMapping(value = "/api/v1/s3credentials/query", produces = "application/json")
    public PageOfS3Credentials queryCredentials(
            @Parameter(description = "The start index of credentials to return") @Valid @RequestParam(value = "offset", required = false, defaultValue = "0") long offset,
            @Parameter(description = "Maximum number of credentials to return") @Valid @RequestParam(value = "limit", required = false, defaultValue = "100") long limit,
            @Parameter(description = "The conditions to query platform users") @Valid @RequestParam(value = "filter", required = false) String filter) {
        return this.osisService.queryS3Credentials(offset, limit, filter);
    }

    @Operation(summary = "get bucket logging id", description = "",
            responses = {
                    @ApiResponse(responseCode = "501", description = "The optional API is not implemented")
            })
    @GetMapping(value = "/api/v1/bucket-logging-id", produces = "application/json")
    @NotImplement(name = ScalityOsisConstants.GET_BUCKET_ID_LOGGING_API_CODE)
    public OsisBucketLoggingId getBucketLoggingId() {
        throw new NotImplementedException();
    }

    @Operation(summary = "get anonymous user", description = "Operation ID: getAnonymousUser<br> get anonymous user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The anonymous user is returned")
            })
    @GetMapping(value = "/api/v1/anonymous-user", produces = "application/json")
    public AnonymousUser getAnonymousUser() {
        return this.osisService.getAnonymousUser();
    }

    @Operation(summary = "update Osis caps", description = "Operation ID: updateOsisCaps<br> get anonymous user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The ScalityOsisCaps has been updated"),
                    @ApiResponse(responseCode = "501", description = "The optional API is not implemented")
            })
    @PostMapping(value = "/api/admin-apis", produces = "application/json")
    public ScalityOsisCaps updateOsisCaps(@RequestBody ScalityOsisCaps osisCaps) {
        return this.osisService.updateOsisCaps(osisCaps);
    }
}