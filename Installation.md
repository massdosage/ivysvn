# Installing IvySvn from a binary release #

The following steps describe how to install IvySvn after you have downloaded and unpacked the latest binary release.

  1. Copy all the third party library jar files (Ivy, Trilead and SVNKit) from the `lib` folder to `ANT_HOME/lib/` or to somewhere on your CLASSPATH. If you are upgrading from a previous version of IvySvn please remove any older versions of these libraries.
  1. Copy `ivysvnresolver.jar` into `ANT_HOME/lib/` or make sure it is on your CLASSPATH.
  1. Create a standard `ivy.xml` file for your project declaring your dependencies, publications etc.
  1. Create an `ivysettings.xml` file containing a "svn" resolver element and set various properties on it as described in [Configuration](Configuration.md).

## Upgrading from 2.0.0 to 2.1.0 ##
A major change in 2.1.0 is the meaning of the `repositoryRoot` attribute in `ivysettings.xml` - this is now taken to mean the root location of your **Ivy** repository in Subversion and not the root of the Subversion repository as it was previously. A follow on from this is that the `ivy` and `artifact` patterns need to be changed to be relative to this Ivy root repository location instead of the root of the entire Subversion repository. If your Ivy and Subversion repository roots are identical then you can skip this step.

In practise this usually means moving part of the leading folder structure from the patterns and appending them to the existing `repositoryRoot`. An example should make this clearer. The following shows a 2.0.0 `ivysettings.xml` file:
```
    <svn name="ivysvn" repositoryRoot="svn://localhost/opt/svntest/" 
         userName="${svn.user.name}" userPassword="${svn.user.password}">
      <ivy pattern="ivy/repository/[organisation]/[module]/[revision]/ivy-[revision].xml"/>
      <artifact pattern="ivy/repository/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"/>
    </svn>
```
The root of the Subversion repository is
```
svn://localhost/opt/svntest/
```
while the root of the Ivy repository is
```
svn://localhost/opt/svntest/ivy/repository/
```
So, in this case the the 2.1.0 ivy settings file would need to be changed so that `repositoryRoot` contains the path to the Ivy repository by adding `ivy/repository` to it and removing this from the patterns like so:
```
    <svn name="ivysvn" repositoryRoot="svn://localhost/opt/svntest/ivy/repository" 
         userName="${svn.user.name}" userPassword="${svn.user.password}">
      <ivy pattern="[organisation]/[module]/[revision]/ivy-[revision].xml"/>
      <artifact pattern="[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"/>
    </svn>    
```
`repositoryRoot` now contains the full path to the Ivy repository and the two patterns are relative to this instead of the Subversion repository root.

## Upgrading from 1.4 or below ##
If you are upgrading from IvySvn 1.4 or below you will also need to:
  1. Modify `ivysettings.xml` - add a required "repositoryRoot" attribute pointing to the root of your repository (e.g. repositoryRoot="svn+ssh://host.foo.com/svn") and then modify the ivy and artifact patterns to contain paths relative to this root. Also, many attribute names now contain mixed case (e.g. "keyfile" is now "keyFile") and some attribute names have changed (e.g. "port" is now "sshPort"). For more information refer to [Configuration](Configuration.md).
  1. If you previously used the attribute "repositoryURL" on the "svn" element in `ivysettings.xml`, remove it as it is no longer required and has been superceded by the new mandatory attribute "repositoryRoot" described above.
  1. Remove previous Ivy jar files (e.g. 'ivy-2.0.0-beta2.jar') from `ANT_HOME/lib/`. All future versions of IvySvn will name this jar `ivy.jar` so future upgrades can just replace this file instead of having to remove older versions.

## Upgrading from 1.3 or below ##
If you are upgrading from IvySvn 1.3 or below you will also need to:
  1. Remove the contents of your local ivy2 cache (this is located in `~/.ivy2` on Linux) - remove the folder and all its contents).
  1. Remove `ANT_HOME/lib/ganymed.jar` and previous versions of Ivy jar files from `ANT_HOME/lib/`.