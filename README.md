# Chain
The chain multi-project suite implements a set of SETL blockchain functionality 

## Development
IntelliJ is the only approved development environment. 

## Release process

Formal releases should always be done on a release branch release-n.m
All artifacts may be released by following these steps

1. Switch to the appropriate branch, e.g. release-1.0
2. Pull to ensure upto date.
3. Check no uncommitted changes are present in local branch
4. Identify branch feeding this release branch, e.g. develop-1.0, or master if lastest release. It's master!
5. Merge the feeding branch into the current (release) branch
6. Run git cherry -v to obtain the changes about to be released. Add these into top of release.notes.txt
7. Increase top level build.grade>subprojects>version to newVersion
8. Run full build
8. git commit with newVersion
9. git tag with newVersion
10. Now push including tags
11. Email the text added to release.notes.txt

A jenkins build job will build and make artifacts available.


## State versions

Current State and Block version is 3, representing the `current` version during the initial Java development.

Introduced in :

Version 4 :

1. Address UpdateTime and Metadata will be set by default.
2. Address Balance and Nonces are both always created.
...
4. MEntry implementation classes store the updateHeight during block commit so that it is easier to step back, for a given address, and to see what blocks 
contain transactions affected it.


## Git commits
Git commits for Taiga tasks should be of the form
```
TG-278 Amend Readme
```

or where additional detail is required,
```
TG-278 Amend Readme
Add project documentation to Readme.md to include
- Project purpose
- Command line parameters
Change format to be compliant with standards.
```

where TG-278 references the [Taiga](http://si-taiga01.dev.setl.io/project/paul-core-blockchain-project/) task
 
**All** commits should follow the commit guidelines of
+ Separate subject from body with a blank line
+ Limit the subject line to 50 characters
+ Capitalize the subject line
+ Do not end the subject line with a period
+ Use the imperative mood in the subject line
+ Wrap the body at 72 characters
+ Use the body to explain what and why vs. how

See [here](https://chris.beams.io/posts/git-commit/) for guidelines.

**Always commit against your own branch** - create a merge request in gitlab upon completion.

## Code style

The projects use the [google code style](https://google.github.io/styleguide/javaguide.html) with the following exceptions : 
+ Loosening of line length to 120 (160 in exceptional cases)
+ Loosening of local variable and method naming convention to require only a single leading lower-case character, rather than the standard two characters.
+ Loosening of Java import ordering requirements : now Static then all others.
+ Loosening of Generic ObjectType and MemberType naming requirements : Must be a single letter, or end in 'Type'.
+ AbbreviationAsWordInName : Allow up to three letter abbreviations.
+ Changed code block ordering so that all static items are together at the start of the file.
+ Inserted blank lines between fields to improve readability of associated JavaDoc.
+ Loosening of VariableDeclarationUsageDistance:allowedDistance from 3 to 10.

Compliance will be checked with [checkstyle](http://checkstyle.sourceforge.net/google_style.html)
The compliance report can be found [here](http://si-jenkins01.dev.setl.io:8080/job/JavaCoreBlockchain/76/checkstyleResult/)


## Code style Sonar changes
 Control flow statements "if", "for", "while", "switch" and "try" should not be nested too deeply
Change to 8 on orange way

## Code style IDE installation

[IDE installation guide](https://github.com/HPI-Information-Systems/Metanome/wiki/Installing-the-google-styleguide-settings-in-intellij-and-eclipse)
The intellij code style xml file can be found in /config

#### For  the intellij plugin, which gives compliance status within the IDE

+ Preferences>Plugins>Browse
+ Install plugin **checkstyle-idea**
+ Restart
+ Preference>Other settings>CheckStyle
+ Add SETL Google Checkstyle
+ Browse to ProjectHome/config/checkstyle/checkstyle.xml
 
Select any java file
Select checkstyle tab in bottom left corner
Click play button to give check style warnings.


## Testing and test coverage

It is expected that code coverage for this project will reach somewhere in the range 50% - 80%

It is important that
+ Testing should be considered **before** starting any major development.
+ Focus should be on the quality of tests rather than absolute coverage targets.
+ Unit test coverage should be considered on a case by case basis.
+ Tests should be sufficient that very few bugs escape into production
+ Tests should be sufficient that the developer feels confident to change almost any code, without fear of uncaught bugs being introduced.  

The test coverage report can be found [here](http://si-jenkins01.dev.setl.io:8080/job/JavaCoreBlockchain/jacoco/)

##Findbugs/FindSecurityBugs/PMD
Static code analysis is performed on every jenkins build. The report can be found [here](http://si-jenkins01.dev.setl.io:8080/job/JavaCoreBlockchain/76/findbugsResult/)

Every effort should be made to
+ Minimise the introduction of new warnings
+ Mark false positives with the SuppressFBWarnings annotation (This annotation will be automatically removed before production releaase)
+ Zero reported bugs should be the long term goal
 
 
##Warnings
Compiler warnings should be reviewed, and either removed or annotated as non-problematic - **before commit.**

## Support projects

+ chain-api.
+ chain-api-impl
+ chain-common
+ [chain-core](./chain-core)
+ chain-core-tx
+ chain-code-tx
+ chain-index-h2
+ chain-index-jdbc
+ chain-netty
+ chain-peerman
+ chain-peerman-netty
+ chain-store
+ chain-store-rocksdb

  
## Executable projects
+ chain-cmdtools

   A set of command line tools to query the block chain.
+ chain-node-validation

   A validation node
+ chain-node-wallet

   A wallet node
   
 
## Maven/repository deploy
All project components can be deployed to the nexus repository using the top level publishing/publish gradle task.
    
## Server deployment
 A Java 8 JRE is required.
 
 A prototype deploy.sh script can be found in the scripts folder.
 This uses the rcp/ssh etc. protocols to deploy the libraries and shell scripts to the target host.
 
## Docker
Two new docker tasks have been created, 

dockerBuild - build images of all modules

dockerPush - publish all images

Images will be tagged in the format modulename:version

with a repo of  dreg.ad.setl.io/setl

e.g.
dreg.ad.setl.io/setl/chain-cmdtools:1.0-SNAPSHOT

A container can then be started with a command

docker run -it --rm -v `pwd`:/wd  dreg.ad.setl.io/setl/chain-cmdtools:1.0-SNAPSHOT ltg

+ -it = interactive
+ --rm = remove on exit
+ -v = mount the local working directory as /wd on the container
+ All arguments after the image name will be passed directly to the container

## Performance metrics
To view streamed performance data via websocket, it requires this configuration to be switched on:
```
experimental.monitoring.ws.enabled=true
experimental.monitoring.rest.enabled=true
```
To view Spring Actuator values, it requires the following configurations:

```
health.user=${HEALTH_USER}
health.password=${HEALTH_PASSWORD}
management.endpoint.health.enabled=true
management.endpoint.health.show-details=always
management.endpoints.web.exposure.include=*
management.endpoints.enabled-by-default=true
management.endpoint.metrics.enabled=true
```
Use the below endpoint to view the available metrics:
```
http://localhost:13505/metrics/
```
Add the individual metric name to the above endpoint after the last ``/`` to view the metric details/value.
