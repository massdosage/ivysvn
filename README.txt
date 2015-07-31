Ivy Subversion Resolver
~~~~~~~~~~~~~~~~~~~~~~~

For more information please visit:

  https://github.com/massdosage/ivysvn
  
Binary Version
~~~~~~~~~~~~~~

See https://github.com/massdosage/ivysvn/wiki/Installation for the latest version of this.

*Installing IvySvn*

The following steps describe how to install IvySvn after you have downloaded and unpacked the latest binary release.

   1. Copy all the third party library jar files (Ivy, Trilead and SVNKit) from the lib folder to ANT_HOME/lib/ or to somewhere on your CLASSPATH. If you are upgrading from a previous version of IvySvn please remove any older versions of these libraries.
   2. Copy ivysvnresolver.jar into ANT_HOME/lib/ or make sure it is on your CLASSPATH.
   3. Create a standard ivy.xml file for your project declaring your dependencies, publications etc.
   4. Create an ivysettings.xml file containing a "svn" resolver element and set various properties on it as described in Configuration. 

*Upgrading from 2.0.0 to 2.1.0*

A major change in 2.1.0 is the meaning of the repositoryRoot attribute in ivysettings.xml - this is now taken to mean the root location of your Ivy repository in Subversion and not the root of the Subversion repository as it was previously. A follow on from this is that the ivy and artifact patterns need to be changed to be relative to this Ivy root repository location instead of the root of the entire Subversion repository. If your Ivy and Subversion repository roots are identical then you can skip this step.

In practise this usually means moving part of the leading folder structure from the patterns and appending them to the existing repositoryRoot. An example should make this clearer. The following shows a 2.0.0 ivysettings.xml file:

    <svn name="ivysvn" repositoryRoot="svn://localhost/opt/svntest/" 
         userName="${svn.user.name}" userPassword="${svn.user.password}">
      <ivy pattern="ivy/repository/[organisation]/[module]/[revision]/ivy-[revision].xml"/>
      <artifact pattern="ivy/repository/[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"/>
    </svn>

The root of the Subversion repository is

svn://localhost/opt/svntest/

while the root of the Ivy repository is

svn://localhost/opt/svntest/ivy/repository/

So, in this case the the 2.1.0 ivy settings file would need to be changed so that repositoryRoot contains the path to the Ivy repository by adding ivy/repository to it and removing this from the patterns like so:

    <svn name="ivysvn" repositoryRoot="svn://localhost/opt/svntest/ivy/repository" 
         userName="${svn.user.name}" userPassword="${svn.user.password}">
      <ivy pattern="[organisation]/[module]/[revision]/ivy-[revision].xml"/>
      <artifact pattern="[organisation]/[module]/[revision]/[artifact]-[revision].[ext]"/>
    </svn>    

repositoryRoot now contains the full path to the Ivy repository and the two patterns are relative to this instead of the Subversion repository root. 

Source Version
~~~~~~~~~~~~~~

See https://github.com/massdosage/ivysvn/wiki/Building-from-source for the latest version of this.


Third party libraries
~~~~~~~~~~~~~~~~~~~~~
The Ivy Subversion resolver requires the following third-party libraries, the version numbers indicate the versions 
that the Ivy Subversion resolver was built and tested against, your mileage with other versions may vary.

Ivy 2.2.0 (http://ant.apache.org/ivy/)
Trilead SSH-2 For Java build 213-svnkit-1.3-patch (http://www.trilead.com/Products/Trilead-SSH-2-Java/)
SVNKIt 1.3.4 (http://svnkit.com/)

These files are included in the "lib" folder. 

