# vmware-ose-scality
## Abstract

VMware Cloud Director Object Storage Extension (for short, OSE) is a standalone middleware service installed in private data center or public cloud to provide object storage capabilities to the users of VMware Cloud Director.

OSIS (Object Storage Interoperability Service) is proposed to extend OSE to support other object storage platforms by defining unified administrative interfaces for storage platforms.

For the platforms integrated with OSE via OSIS, the data channel is between OSE and the platform, but the control channel is between OSE and OSIS implementation (REST services implementing OSIS).

This project is for OSIS which integrates [Scality RING](https://www.scality.com/products/ring/) with vCloud Director OSE. 

## Running the Application

### Commands to run Docker Image
1. Docker images can be built locally or pulled locally from either dev namespace using the hash or production namespace.

    Local build:
    ```sh
        $ docker build -t <local_image_name> .
    ```
    Dev:
    ```sh
        $ docker pull registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
    ```
    Production image:
    ```sh
        $ docker pull registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
    ```
1. Generate a self-signed SSL certificate and store it in a binary PKCS#12 format file with extension `.p12` file. (Refer [here](#To-generate-PKCS12-file-for-self-signed-SSL-certificate))

1. Create an environment variables file `application.properties`
    - Sample `application.properties` file can be found [here](src/main/resources/application.properties).
    - Update `server.ssl.key-store-password` and `server.ssl.key-alias` with key-store password and key-store alias of the `.p12` file respectively.

1. To run the docker image locally using local build image or from either dev namespace using the hash or production namespace with the environment file `application.properties`

   Local build image:
    ```sh
     $ docker run \
       --env-file application.properties \
       -it \
       -p 8443:8443 \
       --mount type=bind,source=<absolute_path_to_.p12_file>,target=/app/lib/osis.p12 \
       <local_image_name>
    ```
   Dev:
    ```sh
     $ docker run \
       --env-file application.properties \
       -it \
       -p 8443:8443 \
       --mount type=bind,source=<absolute_path_to_.p12_file>,target=/app/lib/osis.p12 \
       registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
    ```
    Production image:
    ```sh
     $ docker run \
       --env-file application.properties \
       -it \
       -p 8443:8443 \
       --mount type=bind,source=<absolute_path_to_.p12_file>,target=/app/lib/osis.p12 \
       registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
    ```


#### To generate PKCS12 file for self-signed SSL certificate
**Generate a `.p12` format file with self-signed SSL certificate using Keytool.**
```shell
keytool \
  -genkeypair \
  -keyalg RSA \
  -alias <alias_name> \
  -keystore <key-store_file>.p12 \
  -storepass <key-store_password> \
  -validity <days> \
  -keysize <size_in_bytes> \
  -dname "CN=<common_name>, OU=<organization_unit>, O=<organization>, L=<city>, ST=<state>, C=<country>" \
  -storetype pkcs12
```
**Example:**
```shell
keytool \
  -genkeypair \
  -keyalg RSA \
  -alias osis.dev.eng.scality.com \
  -keystore keyStore.p12 \
  -storepass scality \
  -validity 3650 \
  -keysize 2048 \
  -dname "CN=scality-osis-app, OU=Scality, O=Scality, L=SanFrancisco, ST=California, C=US" -storetype pkcs12
```

#### To view Tomcat Access logs in the docker container
* Run `docker exec -it <Container_ID> /bin/bash`
* Access log files can be found under `/tomcat/logs` 

### Steps to run application as a standalone jar (Developers Only) 
1. `./gradlew clean build`
2. `./gradlew bootJar`
3. `java -jar -Dserver.tomcat.basedir=tomcat -Dserver.tomcat.accesslog.directory=logs -Dserver.tomcat.accesslog.enabled=true build/libs/osis-scality-[CURRENT_VERSION].jar`


## To Verify the implementation (Developers Only)

Install and run [vmware-ose-osis-verifier](https://github.com/vmware-samples/object-storage-extension-samples/tree/master/vmware-ose-osis-verifier) for each API
