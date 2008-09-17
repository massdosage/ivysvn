Ivy Subversion Resolver
~~~~~~~~~~~~~~~~~~~~~~~

For more information please visit:

  http://code.google.com/p/ivysvn/
  
Binary Version
~~~~~~~~~~~~~~

Installing Ivysvn

The following steps describe how to install Ivysvn after you have downloaded and unpacked the latest binary release.
 
1. Copy all the third party libraries (Ivy, Trilead and SVNKit) from the lib folder to ANT_HOME/lib/ or to somewhere on 
your CLASSPATH. If you are upgrading from a previous version of Ivysvn please remove any older versions of these 
libraries. 
2. Copy ivysvnresolver.jar into ANT_HOME/lib/ or make sure it is on your CLASSPATH. 
3. Create a standard ivy.xml file for your project declaring your dependencies, publications etc. 
4. Create an ivysettings.xml file containing a "svn" resolver element and set various properties on it. 

If you are upgrading from Ivysvn 1.4 or below you will also need to: 

1. Modify ivysettings.xml - many attribute names now contain mixed case (e.g. "keyfile" is now "keyFile") and some 
attribute names have changed (e.g. "port" is now "sshPort"). For more information refer to Configuration. 
2. Remove previous Ivy jar files (e.g. 'ivy-2.0.0-beta2.jar') from ANT_HOME/lib/. All future versions of Ivysvn 
will name this jar ivy.jar so future upgrades can just replace this file instead of having to remove older 
versions. 

If you are upgrading from Ivysvn 1.3 or below you will also need to:
 
1. Remove the contents of your local ivy2 cache (this is located in ~/.ivy2 on Linux) - remove the folder and
 all its contents). 
2. Remove ANT_HOME/lib/ganymed.jar and previous versions of Ivy jar files from ANT_HOME/lib/.

Source Version
~~~~~~~~~~~~~~

Installing from source:

1. Copy ivysettings.xml.example to ivysettings.xml and configure to suit your setup.
2. Copy build.properties.example to build.properties and set the values according to your setup.
3. Run "ant -Dskip.retrieve=true -Dversion=0 install" which will compile the Java source code, build ivysvnresolver.jar 
into build/dist and then copy this, ivysettings.xml, ivy-common-targets.xml and the required 3rd party libraries to your 
ANT_HOME/lib folder.

If you are upgrading from a previous version please remove any older versions of the files copied across 
in the above step from ANT_HOME/lib.

Testing that it works:

1. Edit ivy.xml so that it contains dependencies to artifacts in your repository.
2. Run "ant retrieve" and the dependent files should be downloaded to the lib folder.
3. Make sure the "version" property is set and run "ant publish" and the current version of ivysvnresolver.jar. 
should be published to your repository. NOTE: Every time you do a publish you will need to use a different 
version number (or set the property "publish.overwrite" to true).


Third party libraries
~~~~~~~~~~~~~~~~~~~~~
The Ivy Subversion resolver requires the following third-party libraries, the version numbers indicate the versions 
that the Ivy Subversion resolver was built and tested against, your mileage with other versions may vary.

Ivy 2.0 beta 2 (http://ant.apache.org/ivy/)
Trilead SSH-2 For Java build 211 (http://www.trilead.com/Products/Trilead-SSH-2-Java/)
SVNKIt 1.1.8 (http://svnkit.com/)

These files are included in the "lib" folder. 

