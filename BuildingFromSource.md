# Introduction #

This page describes the steps needed to get IvySvn set up for development and covers checking out the code and building it from source.

# Details #

## Prerequisites ##
  * Java >=5.0 installed.
  * Ant >= 1.6 installed.

## Getting the source ##
### Download a source release ###
If you want to modify the source code of an official IvySvn release then you should download the "Source release" from the [downloads](http://code.google.com/p/ivysvn/downloads/list) page and unpack it.

### Checkout the latest code from SVN ###
If you want to modify the source code based on the current trunk version you should follow the instructions on the [source](http://code.google.com/p/ivysvn/source/checkout) page. If you do not have SVN commit rights you can still check out the code and create and submit a patch with your changes.

## Building from source ##
Once you have checked out or downloaded and unpacked the source code you can build IvySvn by issuing the command:
```
ant build
```
which will build the jar file:
```
build/dist/ivysvnresolver.jar
```

## Installing a new build ##
  * Copy ivysettings.xml.example to ivysettings.xml and modify it to suit your Subversion setup.
  * Then you can run:
```
ant -Dskip.retrieve=true -Dversion=REPLACE_WITH_YOUR_VERSION install
```
which will compile the Java source code, build `ivysvnresolver.jar` into `build/dist` and then copy this, `ivysettings.xml`, `ivy-common-targets.xml` and the required 3rd party libraries to your `ANT_HOME/lib` folder. Alternatively you can just copy the files mentioned here to wherever you want IvySvn setup.

## Testing that it works ##
  * Edit `ivy.xml` so that it contains dependencies to artifacts in your repository.
  * Run the following command and the dependent files should be downloaded to the `lib` folder.
```
ant retrieve
```
  * Make sure the "version" property is set and run
```
ant publish
```
and the current version of `ivysvnresolver.jar`
should be published to your repository. NOTE: Every time you do a publish you will need to use a different version number (or set the property "publish.overwrite" to true).