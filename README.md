# vmware-ose-scality
## Abstract

VMware Cloud Director Object Storage Extension (for short, OSE) is a standalone middleware service installed in a private data center or public cloud to provide object storage capabilities to VMware Cloud Director users.

OSIS (Object Storage Interoperability Service) is proposed to extend OSE to support other object storage platforms by defining unified administrative interfaces for storage platforms.

For the platforms integrated with OSE via OSIS, the data channel is between OSE and the platform, but the control channel is between OSE and the OSIS implementation (REST services implementing OSIS).

This project is for OSIS, which integrates [Scality RING](https://www.scality.com/products/ring/) with vCloud Director OSE. 

## Running the Application

### Commands to Run Docker Image
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

1. Create an `application.properties` environment variables file.
    - A sample `application.properties` file can be found [here](src/main/resources/application.properties).
    - Update `server.ssl.key-store-password` and `server.ssl.key-alias` with the `.p12` file's key-store password and key-store alias, respectively. 

1. Create an `crypto.yml` environment variables file.
   - A sample `crypto.yml` file can be found [here](src/main/resources/crypto.yml).
   - Update `osis.security.keys.cipher` with a 32 byte password

1. To run the Docker image locally using a local build image, or from either the dev namespace (using the hash) or the production namespace (with the `application.properties` environment file):

   Local build image:
    ```sh
     $ docker run \
       -it \
       -p 8443:8443 \
       -v <absolute_path_to_.p12_file>:/app/lib/osis.p12:ro,z \
       -v <absolute_path_to_crypto.yml_file>:/conf/crypto.yml:ro,z \
       -v <absolute_path_to_application.properties_file>:/conf/application.properties:ro,z \
       <local_image_name>
    ```
   dev:
    ```sh
     $ docker run \
       --env-file application.properties \
       -it \
       -p 8443:8443 \
       -v <absolute_path_to_.p12_file>:/app/lib/osis.p12:ro,z \
       -v <absolute_path_to_crypto.yml_file>:/conf/crypto.yml:ro,z \
       -v <absolute_path_to_application.properties_file>:/conf/application.properties:ro,z \
       registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
    ```
   Production image:
    ```sh
     $ docker run \
       --env-file application.properties \
       -it \
       -p 8443:8443 \
       -v <absolute_path_to_.p12_file>:/app/lib/osis.p12:ro,z \
       -v <absolute_path_to_crypto.yml_file>:/conf/crypto.yml:ro,z \
       -v <absolute_path_to_application.properties_file>:/conf/application.properties:ro,z \
       registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
    ```


#### To generate a PKCS12 file for a self-signed SSL certificate
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

#### To view Tomcat access logs in the Docker container
* Run `docker exec -it <Container_ID> /bin/bash`
* Access log files can be found under `/tomcat/logs` 

### Run the Application as a Standalone Jar (Developers Only) 
1. `./gradlew clean build`
2. `./gradlew bootJar`
3. `java -jar -Dserver.tomcat.basedir=tomcat -Dserver.tomcat.accesslog.directory=logs -Dserver.tomcat.accesslog.enabled=true build/libs/osis-scality-[CURRENT_VERSION].jar`

### Scan for vulnerabilities

- `./gradlew dependencyCheckAnalyze`: Runs dependency-check against the project and generates a report under `build/reports`
- `./gradlew dependencyCheckUpdate`: Updates the local cache of the NVD data from NIST.
- `./gradlew dependencyCheckPurge`: Deletes the local copy of the NVD. This is used to force a refresh of the data.
- More configuration options: <https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/configuration.html>

## Verify the Implementation (Developers Only)

Install and run [vmware-ose-osis-verifier](https://github.com/vmware-samples/object-storage-extension-samples/tree/master/vmware-ose-osis-verifier) for each API.
