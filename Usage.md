# Usage #

Provided you have followed the [Installation](Installation.md) and [Configuration](Configuration.md) steps you should be able to publish and retrieve files to and from a Subversion repository using the standard Ivy Ant tasks. You will obviously need to set up your own Ivy repository in Subversion and add files and folders containing whatever your projects depend on. For publishing you will also need write access to the repository.

The examples below assume that the Subversion server is on the host "svn.acme.com" with the Subversion root set to the path "svn". Artifacts published and retrieved by Ivy are stored on this server under the "svn" root path in the folder "java/repository". The Ivy pattern for artifacts is
```
[organisation]/[module]/[revision]/[artifact].[ext]
```
So, for example, the full Subversion path to the artifact "widgetlib.jar" for the organisation "acme", module "widgets" and revision "0.13." would be:
```
svn+ssh://svn.acme.com/svn/java/repository/acme/widgets/0.13/widgetlib.jar
```
The examples also assume that the repository is accessed using Subversion username and password authentication with the user name "testuser" and password "testpassword".

## Example ivysettings.xml for svn+ssh:// access ##
This example shows how to access the repository via svn+ssh, there are more examples below which show other forms of access like svn:// and http(s)://.

```
<ivysettings>
  
  <typedef name="svn" classname="fm.last.ivy.plugins.svnresolver.SvnResolver"/>
  
  <resolvers>
    <svn name="ivysvn" repositoryRoot="svn+ssh://svn.acme.com/svn/java/repository" 
         userName="testuser" userPassword="testpassword">
      <ivy pattern="[organisation]/[module]/[revision]/ivy.xml"/>
      <artifact pattern="[organisation]/[module]/[revision]/[artifact].[ext]"/>
    </svn>
  </resolvers>
  
  <settings defaultResolver="ivysvn"/>
  
  <modules>
    <module organisation="acme" name="ivysvnresolver" resolver="ivysvn"/>
  </modules>
  
</ivysettings>
```

---


## Using a Proxy server ##
If you require the use of an HTTP proxy to access your repository you can specify this using the System properties "http.proxyHost", "http.proxyPort" (defaults to 3128 if not specified), "http.proxyUser", and "http.proxyPassword". The property "java.net.useSystemProxies" is set to false by IvySvn.

---


## Retrieving artifacts ##

If a project requires the aforementioned "widgetslib.jar" you should set Ivy up to depend on it following the steps below.

First ensure that `ivy.xml` contains an "info" element like so:
```
<info organisation="acme" module="anothermodule"/>
```

To retrieve the 0.13 release of "widgetlib.jar", add the following element to `ivy.xml`  within the "dependencies" element:
```
<dependency org="acme" name="widgets" rev="0.13" />
```

Perfoming an Ivy retrieve at this point should retrieve the "widgetslib.jar" file from the specified location in Subversion.

### Retrieving artifacts using a specific Subversion revision ###

By default, retrieve operations as explained above retrieve artifacts using the latest ("HEAD") revision from Subversion. To specify a different revision to use for retrieve operations, add a "retrieveRevision" attribute to the "svn" element. For example, to retrieve artifacts with the Subversion [revision 3389](https://code.google.com/p/ivysvn/source/detail?r=3389) you would add the following:
```
retrieveRevision="3389"
```
**Note:** If you have previously retrieved the artifact you **must** remove it from the local Ivy cache before calling retrieve, otherwise Ivy will use the cached version (which might have a different revision) instead of retrieving the revision requested from Subversion.

---


## Publishing artifacts ##

If you are the maintainer of the "widgets" package and you would like to publish the new, 0.14, release you would follow the steps below.

First ensure that `ivy.xml` contains an "info" element like so:
```
<info organisation="acme" module="widgets"/>
```

Next make sure that `ivy.xml` references the artifact you want to publish within the "publications" element:
```
<artifact name="widgetslib" ext="jar"/>
```

Your build process should build "widgetslib.jar" and use the Ant Ivy publish target with "pubrevision=0.14" to publish it. The file will be published to this location in the Subversion repository:

```
svn+ssh://svn.acme.com/svn/java/repository/acme/widgets/0.14/widgetlib.jar
```

### Overwriting previous publications ###

If you set Ivy's "overwrite" property to "false" and try to publish an artifact with a revision that has already been published IvySvn will notify you of this fact in the Ant output and will ignore the publish request. The Ant build will not fail but nothing will be published.

If you set "overwrite" to "true" then any existing artifacts will be updated (i.e. a Subversion diff takes place). If binaryDiff is true then the folder being published to will be deleted and the files from the intermediate diff folder will be copied over to a folder with the same name.

### Publishing with binaryDiff ###

If you need to publish large (or a large number) of artifacts and/or you publish often with a different revision each time you can reduce the space used in the Subversion repository with IvySvn's "binaryDiff" feature. This is set to true by default but can be disabled by adding the attribute "binaryDiff" to the "svn" element and setting it to "false":
```
    <svn name="ivysvn" userName="testuser" userPassword="testpassword" binaryDiff="false">
```
If binaryDiff is not enabled then IvySvn publishes a new revision of the artifacts without doing any "diff'ing" with existing revisions which means that the Subversion repository grows by the size of the published artifacts for each publish operation that occurs. However, with binaryDiff enabled, IvySvn takes advantage of Subversion's binary diff mechanism during publish operations. The first time you do a publish, say of version 0.14, the following two folders will be created in Subversion, containing your published artifacts:
```
acme/widgets/LATEST/
acme/widgets/0.14/
```
In this case IvySvn first publishes the artifacts to LATEST, and then does a Subversion copy from LATEST to 0.14. If you now release version 0.15, IvySvn will perform a Subversion diff between the 0.15 artifacts and the artifacts in LATEST (reducing the size of this commit operation) and will then copy LATEST to 0.15. If you have big binary files which change very little between publish operations this offers a significant space reduction in the Subversion repository.

**Note:** Binary diffs require the "ivy" and "artifact" patterns to contain `[revision]` _once only_.

If you would like to use a folder name other than LATEST for storing the binaryDiff artifacts you can set the attribute "binaryDiffFolderName":
```
    <svn name="ivysvn" userName="testuser" userPassword="testpassword" binaryDiff="true"
         binaryDiffFolderName="SOMEOTHERFOLDER">
```

### Cleaning up previously published artifacts ###
By default, when publishing with binaryDiff set to false, any artifacts in the folder being published to are left as is. However, if you want to clean up the folder being published to you may set the attribute "cleanupPublishFolder" on the "svn" element. This will delete the contents of the folder being published to as part of the publish operation. This can be useful if you repeatedly publish the same revision and the artifacts that are part of the publish change between publish operations - the cleanup will ensure that artifacts which are no longer part of the publication are removed. Setting binaryDiff to true implies this behaviour as the binary diff publish operation always deletes the final destination folder and copies the contents of LATEST to this location. If binaryDiff is set to true you cannot set "cleanupPublishFolder" to false, attempting this will cause the build to fail.


---


## Example ivysettings.xml for svn:// access ##

This example `ivysettings.xml` file shows how to set up access to a repository using direct svn access:
```
<ivysettings>
  
  <typedef name="svn" classname="fm.last.ivy.plugins.svnresolver.SvnResolver"/>
  
  <resolvers>
    <svn name="ivysvn" repositoryRoot="svn://svn.acme.com/svn/java/repository" 
         userName="testuser" userPassword="testpassword">
      <ivy pattern="[organisation]/[module]/[revision]/ivy.xml"/>
      <artifact pattern="[organisation]/[module]/[revision]/[artifact].[ext]"/>
    </svn>
  </resolvers>
  
  <settings defaultResolver="ivysvn"/>
  
  <modules>
    <module organisation="acme" name="ivysvnresolver" resolver="ivysvn"/>
  </modules>
  
</ivysettings>
```

---


## Example ivysettings.xml for http:// (WebDav) access ##

This example `ivysettings.xml` file shows how to set up access to a repository over http and assumes you have setup http authentication.

```
<ivysettings>
  
  <typedef name="svn" classname="fm.last.ivy.plugins.svnresolver.SvnResolver"/>
  
  <resolvers>
    <svn name="ivysvn" repositoryRoot="http://svn.acme.com/svn/java/repository" 
         userName="testuser" userPassword="testpassword" 
         binaryDiff="true">
      <ivy pattern="[organisation]/[module]/[revision]/ivy.xml"/>
      <artifact pattern="[organisation]/[module]/[revision]/[artifact].[ext]"/>
    </svn>
  </resolvers>
  
  <settings defaultResolver="ivysvn"/>
  
  <modules>
    <module organisation="acme" name="ivysvnresolver" resolver="ivysvn"/>
  </modules>
  
</ivysettings>
```

---
