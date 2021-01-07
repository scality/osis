# Object Storage Extension Requirements

## Context

VMWare's Cloud Director product has a feature called
**Object Storage Extension**(OSE) which allows to work with S3-compatible object
storage from the Cloud Director interfaces. Currently, ECS, AWS and Cloudian
have integrations for this extension, and Scality decided to work on such
integration (with S3C, and later on XDM).

## Requirements

### Storie

**As a** vCloud Director Administrator

**I want** to provision a Storage Tenant to a VMware Cloud Director tenant
organizations using vCloud Director UI

**So that** I can provide object storage to VMware Cloud Director tenants

### Acceptance criteria

The acceptance criteria are:

- The CI produces a docker image and stores it in the Scality registry
- The image can be run and contain the OSE
- The OSE can be integrated with a vault instance
- The OSE integration with a vault instance is documented
- The OSE can be integrated with a vCloud Director instance
- The OSE integration with a vCloud Director instance is documented
- The OSE allows a vCloud Director Administrator to provision a
  Storage Tenant, mapped with Vault Account, to a VMware Cloud Director
  tenant organization
