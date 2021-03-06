# Authentication library for Project EPIC

This contains the library to do authentication and authorization for all Project EPIC microservices.

## Release new version

- Configure your local maven installation to use GitHub to push the package (you can learn how to do so [here](https://help.github.com/en/articles/configuring-apache-maven-for-use-with-github-package-registry#authenticating-to-github-package-registry))
- Update version in `pom.xml`
- `mvn deploy`




## Install on service

- Add dependency on `pom.xml`.
```xml
        <dependency>
            <groupId>edu.colorado.cs.epic</groupId>
            <artifactId>authlib</artifactId>
            <version>1.1.0</version>
        </dependency>
```
- To install, add the following code on your Application run method:
```java
AddAuthToEnv.register(environment);
```
Where `environment` is your Environment parameter. To make testing easier, you can add a production variable on your configuration file such that it can be turned on and off without needing to recompile. Example:

```java
AddAuthToEnv.register(environment, configuration.getProduction());
```
### Authentificating a request from a client

Include an Authorization header to the request with the following format: `Bearer ACCESS_TOKEN`. Where `ACCESS_TOKEN` is the jwt obtained when logged in on Firebase.


### CORS specification

Make sure the Authorization header is allowed in your CORS configuration.

```java
cors.setInitParameter("allowedOrigins", "*");
cors.setInitParameter("allowedHeaders", "X-Requested-With,Authorization,Content-Type,Accept,Origin");
cors.setInitParameter("allowedMethods", "OPTIONS,GET,PUT,POST,DELETE,HEAD");
```

### Protect resources

Accepted annotations to protect your resources:

- `@RolesAllowed("ADMIN")`: Protects method against users logged in but not authorized by an internal member.
- `@PermitAll`: Checks if user exists and is logged in. Any user logged in is allowed to access the resource.

### Get User on resource method

To access the logged in user from a resource method, you can add the following parameter:

```java
@Auth Optional<FirebaseUser> user
```

More information available: https://www.dropwizard.io/1.3.9/docs/manual/auth.html
