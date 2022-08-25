# API compatibility matrix

## Epics

Milestone 1 (DONE) - <https://scality.atlassian.net/browse/RING-35307>

Milestone 2 (DONE) - <https://scality.atlassian.net/browse/RING-35308>

Milestone 3 (DONE) - <https://scality.atlassian.net/browse/RING-35309>

Milestone 4 (DONE) - <https://scality.atlassian.net/browse/RING-35953>

Milestone 5 (IN PROGRESS) - <https://scality.atlassian.net/browse/RING-35197>

## Object Storage Interoperability Service

Reference API documentation - <https://code.vmware.com/apis/1034>

Legend

* `x` - implemented
* `.` - not implemented
* `o` - implemented with default stub
* `*` - Mandatory APIs to integrate with OSE for essential functions, like basic S3 bucket/object CRUD operations

| API                      | Milestone 1 | Milestone 2 | Milestone 3 | Milestone 4 | Milestone 4 |
|:-------------------------|:------------|:------------|-------------|-------------|-------------|
| headTenant *             | .           | .           | .           | x           | x           |
| getTenant                | .           | .           | .           | .           | x           |
| updateTenant *           | .           | .           | .           | x           | x           |
| deleteTenant             | .           | .           | .           | .           | .           |
| listTenants *            | x           | x           | x           | x           | x           |
| createTenant *           | x           | x           | x           | x           | x           |
| queryTenants *           | x           | x           | x           | x           | x           |
| queryUsers *             | .           | x           | x           | x           | x           |
| createUser *             | .           | x           | x           | x           | x           |
| listUsers *              | .           | x           | x           | x           | x           |
| getUserWithId *          | .           | x           | x           | x           | x           |
| headUser                 | .           | .           | .           | .           | x           |
| updateUserStatus *       | .           | .           | .           | x           | x           |
| deleteUser *             | .           | .           | .           | x           | x           |
| getUserWithCanonicalID * | .           | .           | x           | x           | x           |
| queryCredentials *       | .           | .           | x           | x           | x           |
| listCredentials *        | .           | x           | x           | x           | x           |
| createCredential *       | .           | x           | x           | x           | x           |
| getCredential *          | .           | .           | x           | x           | x           |
| updateCredentialStatus   | .           | .           | .           | .           | x           |
| deleteCredential         | .           | .           | .           | x           | x           |
| getUsage                 | o           | o           | o           | o           | .           |
| getBucketList            | .           | .           | .           | .           | o           |
| getBucketLoggingId       | .           | .           | .           | .           | .           |
| getAnonymousUser         | .           | .           | .           | .           | x           |
| getConsole               | o           | o           | o           | x           | x           |
| getS3Capabilities *      | o           | x           | x           | x           | x           |
| getInfo *                | o           | o           | o           | x           | x           |
| refreshToken *           | o           | o           | o           | x           | x           |

### Required APIs

Mandatory APIs to integrate with OSE for essential functions, like basic S3 bucket/object CRUD operations

* headTenant
* updateTenant
* listTenants
* createTenant
* queryTenants
* queryUsers
* createUser
* listUsers
* getUserWithId
* updateUserStatus
* deleteUser
* getUserWithCanonicalID
* queryCredentials
* listCredentials
* createCredential
* getCredential
* getS3Capabilities
* getInfo
* refreshToken

### Optional APIs

Optional APIs to integrate with OSE extension platform, including statistic, tenant off-board, etc. If the optional API is not implemented, it is required to add its operationId to Info resource's not_implemented array field

* getTenant
* deleteTenant
* headUser
* updateCredentialStatus
* deleteCredential
* getUsage
* getBucketList
* getBucketLoggingId
* getAnonymousUser
* getConsole
