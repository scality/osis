# Vmware-ose-scality Release Plan

## Docker Image generation

Docker images are hosted on registry.scality.com.
It has two names spaces for Vmware-ose-scality:

* Production Namespace: registry.scality.com/vmware-ose-scality
* Dev Namespace: registry.scality.com/vmware-ose-scality-dev

Production namespace provides write access to a few admins and CI while read
access is given to all the developers. Dev namespace provides write access
only for the CI. The CI will push images with every CI build tagging the
content with the developer’s branch short SHA-1 commit hash. This allows those
images to be used by developers, CI builds, build chain and so on.

## How to pull docker images

```sh
    docker pull registry.scality.com/vmware-ose-scality-dev/vmware-ose-scality:<short SHA-1 commit hash>
    docker pull registry.scality.com/vmware-ose-scality/vmware-ose-scality:<tag>
```

## Release Process

To release a production image:

* Decide an appropriate tag which will be used to tag the repository and
the docker image.
* Update the version in `build.gradle` file with the same release tag.
* Create a PR and merge the `build.gradle` change.
* Tag the repository using the same tag with the following command.
  ```shell
  git tag --annotate $tag
  ```
* With the below parameters, [force a build](https://eve.devsca.com/github/scality/vmware-ose-scality/#/builders/bootstrap/force/force):
  * A given branch that ideally match the tag.
  * Use the `release` stage.
  * Extra property with name as `tag` and the value as the actual tag.
