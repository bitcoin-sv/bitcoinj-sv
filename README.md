# BitcoinJ-SV

The `BitcoinJ-SV` library is a Java implementation of the original Bitcoin (BSV) protocol. This library is a fork of Mike Hearn's original bitcoinj 
library aimed at supporting the rapidly expanding and scalable Bitcoin SV eco-system.

Release notes are [here](docs/Releases.md).

## Technologies

* Java 11
* [Maven 3+](http://maven.apache.org) - for building the project
* [Google Protocol Buffers](https://github.com/google/protobuf) - for use with serialization and hardware communications

## Getting started

To get started, it is best to have the latest JDK and Maven installed. 

The `master` branch contains the latest production release, with feature branches containing other developments and functionality. 

## Building from the command line

To perform a full build use
```
mvn clean package
```
You can also run
```
mvn site:site
```
to generate a website with useful information like JavaDocs.

The outputs are under the `target` directory.

#### Building from an IDE

Alternatively, just import the project using your IDE. [IntelliJ](http://www.jetbrains.com/idea/download/) has Maven 
integration built-in and has a free Community Edition. Simply use `File | Import Project` and locate the `pom.xml` in the 
root of the cloned project source tree.

### Example applications

These are found in the `examples` module.
