# Milestone 1

## Context

VMWare's Cloud Director product has a feature called **Object Storage
Extension** (OSE), which allows you to work with S3-compatible object storage
from the Cloud Director interfaces. Currently, ECS, AWS and Cloudian have
integrations for this extension, and Scality decided to work on such integration
(with S3C, and later on XDM).

## Requirements

### Stories

**As a** vCloud Director Administrator

**I want** to provision a storage tenant to a VMware Cloud Director tenant
organizations using vCloud Director UI

**So that** I can provide object storage to VMware Cloud Director tenants.

### Acceptance Criteria

The acceptance criteria are:

- The CI produces a Docker image and stores it in the Scality registry.
- The image can be run and can contain the OSE.
- The OSE can be integrated with a Vault instance.
- The OSE integration with a Vault instance is documented.
- The OSE can be integrated with a vCloud Director instance.
- The OSE integration with a vCloud Director instance is documented.
- The OSE allows a vCloud Director Administrator to provision a
  Storage Tenant, mapped with a Vault Account, to a VMware Cloud Director
  tenant organization.

## Milestone-1 Validation

* Scality Object Storage Interoperability Service (**Scality OSIS**)
* VMware Cloud Director Object Storage Extension (**vCloud Director OSE**)

### Prerequisites

1. Run Vault (check [How to Run Vault](https://github.com/scality/Vault/blob/development/7.10/TESTING.md#how-to-run-1))

1. Run CloudServer (check [running CloudServer here](https://github.com/scality/cloudserver/tree/development/7.10#installation)) with env variable: `S3VAULT=scality` so that it can use Vault as an auth backend.

1. Update the `application.properties` environment file with the Vault target URL (before starting the Scality OSIS application)
    * Set the Vault URL in the `osis.scality.vault.endpoint` property (see `application.properties` [here](../src/main/resources/application.properties))
    * Update the Vault super-admin credentials in the `osis.scality.vault.access-key` and `osis.scality.vault.secret-key` properties.

1. Run the Scality OSIS application (Refer [here](../README.md#running-the-application)). Milestone-1 release tag `0.1.0-SNAPSHOT`

1. vCloud Director OSE and OSE UI must be installed and running on the OSE machine.

1. Integrate Scality OSIS with vCloud Director OSE on the OSE machine. (Refer [here](#OSIS-integration-with-vCloud-Director-OSE))

### Validation Steps

1. Log in as a vCloud Director Administrator on vCloud Director UI.
1. Navigate to the `Resources` tab.
1. Create a new `Organization` (vCloud Director tenant) with required fields.
1. Navigate to the `Object Storage` tab.
1. The `Tenants` pane is activated and lists all the vCloud Director tenants with an associated `Storage Tenant ID` (if any exist).
1. Click the newly created tenant's name in the `Tenants` pane.
1. Enable the toggle button beside the Tenant name.
1. Click `Enable`. (This creates a Vault account in the background)
1. `Storage Tenant ID`, which is also the associated Vault Account's Account ID, is generated on the UI.

### OSIS Integration with vCloud Director OSE:

To integrate vCloud Director OSE and the Scality OSIS application:

1. Configure a connection between vCloud Director OSE and the **admin** service of the Scality OSIS instance.
  * When prompted for admin URL, provide OSIS URL with port number.
  * When prompted for credentials, enter Vault superadmin credentials.
  
```shell
[root@vcdose centos]# ose osis admin set
OSIS Compliant Platform Name : Scality
URL      : https://localhost:8443
Username : D4IT2AWSB588GO5J9T00
Secret   : ****************************************
? The given password does not meet the complexity criteria. Do you still want to proceed? Yes
Changed the config Admin successfully
[ Scality Admin ]
URL                : https://localhost:8443
username           : D4IT2AWSB588GO5J9T00
```
2. Configure a connection between the Scality OSIS **S3** service and vCloud Director OSE with OSIS URL and port number.
```shell
[root@vcdose centos]# ose osis s3 set
OSIS Compliant Platform Name : Scality
URL      : https://localhost:8443
Changed the config S3 successfully
[ Scality S3 ]
URL                : https://localhost:8443
```
3. Enable vCloud Director OSE to work with Scality OSIS platform.
```shell
[root@vcdose centos]# ose platforms enable osis --name Scality
[ Platforms ]
Scality (OSIS compliant)         : [ Y ]
amazon                           : [ N ]
cloudian                         : [ N ]
ecs                              : [ N ]
Please restart OSE service to take effect.
```
4. Restart `voss-keeper`.
```shell  
 [root@vcdose centos]# systemctl restart voss-keeper 
```
5. Restart the vCloud Director OSE.
```shell
[root@vcdose centos]# ose service restart
Stop OSE middleware successfully
Start OSE Service:    16s [====================================================================] 100%
Started OSE middleware successfully!
```
6. Validate the configuration of vCloud Director OSE to check if the **Scality** platform is enabled and running.
```shell
[root@vcdose centos]# ose config validate
+-----------------------+-------------+-----------------+-----------+
|          Name         |   Required  |   Connectivity  |   Detail  |
+=======================+=============+=================+===========+
|        Database       |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
|      Certificate      |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
|     Cloud Director    |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
|   Platform - Scality  |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
|         Admin         |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
|           S3          |      Y      |      Normal     |           |
+-----------------------+-------------+-----------------+-----------+
```
