# vmware-ose-scality
## Abstract

VMware Cloud Director Object Storage Extension (for short, OSE) is a standalone middleware service installed in private data center or public cloud to provide object storage capabilities to the users of VMware Cloud Director.

OSIS (Object Storage Interoperability Service) is proposed to extend OSE to support other object storage platforms by defining unified administrative interfaces for storage platforms.

For the platforms integrated with OSE via OSIS, the data channel is between OSE and the platform, but the control channel is between OSE and OSIS implementation (REST services implementing OSIS).

This project is for OSIS which integrates [Scality RING](https://www.scality.com/products/ring/) with vCloud Director OSE. 

## Running the Application

### Command to run Docker Image
A user needs to be logged into docker using URL: registry.scality.com.
```sh
    $ docker login -u <username in registry.scality.com> registry.scality.com
```
Once logged images can be pulled and run locally from either dev namespace using the hash or production namespace.

Dev:
```sh
    $ docker pull registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
    $ docker run --env-file application.properties -it -p8443:8443 registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
```
Production image:
```sh
    $ docker pull registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
    $ docker run --env-file application.properties -it -p8443:8443 registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
```
Sample `application.properties` file can be found in GIT at `src/main/resources/application.properties`

#### To view Tomcat Access logs in the docker container
* Run `docker exec -it <Container_ID> /bin/bash`
* Access log files can be found under `/tomcat/logs` 

### Steps to run application as a standalone jar (Developers Only) 
1. `./gradlew clean build`
2. `./gradlew bootJar`
3. `java -jar -Dserver.tomcat.basedir=tomcat -Dserver.tomcat.accesslog.directory=logs -Dserver.tomcat.accesslog.enabled=true build/libs/osis-scality-[CURRENT_VERSION].jar`


## To Verify the implementation (Developers Only)

Install and run [vmware-ose-osis-verifier](https://github.com/vmware-samples/object-storage-extension-samples/tree/master/vmware-ose-osis-verifier) for each API
