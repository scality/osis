# Milestone 2

## Context

In the previous milestone, we have been able to provision a Storage Tenant to
a VMware vCloud Director Tenant Organization.

The goal of this milestone is to provide the basic storage operations to
a VMware vCloud Director Tenant Organization.

## Requirements

### Stories

Basic buckets operations:

**As a** vCloud Organization Administrator

**I want** to list, create, and delete buckets using vCloud Director Tenant UI

**So that** I will be able to have basic buckets operations on vCloud Director UI

Basic objects operations:

**As a** vCloud Organization Administrator

**I want** to list, upload, and download objects using vCloud Director Tenant UI

**So that** I will be able to have basic objects operations on vCloud Director UI

### Acceptance criteria

The acceptance criteria are:

- The OSE can be integrated with a cloudserver instance
- The OSE configuration required to integrate with a cloudserver instance must
  be documented
- The vCloud Director Tenant UI can list 100 buckets
- The vCloud Director Tenant UI can list 2000 objects
- The vCloud Director UI supports 512KB object upload
- The vCloud Director UI supports 1GB object upload
- The vCloud Director UI allows uploading 20 objects in parallel
