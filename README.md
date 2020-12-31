# vmware-ose-scality
## Abstract

VMware Cloud Director Object Storage Extension (for short, OSE) is a standalone middleware service installed in private data center or public cloud to provide object storage capabilities to the users of VMware Cloud Director.
OSE supports three object storage platforms out of box: Cloudian, Dell EMC ECS and Amazon S3.

OSIS (Object Storage Interoperability Service) is proposed to extend OSE to support other object storage platforms by defining unified administrative interfaces for storage platforms.

For the platforms integrated with OSE via OSIS, the data channel is between OSE and the platform, but the control channel is between OSE and OSIS implementation (REST services implementing OSIS).

This project is for OSIS which integrates [Scality RING](https://www.scality.com/products/ring/) with vCloud Director OSE. 