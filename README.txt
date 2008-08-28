Ivy Subversion Resolver
~~~~~~~~~~~~~~~~~~~~~~~

For more information please visit:

  http://code.google.com/p/ivysvn/
  
Binary Version
~~~~~~~~~~~~~~

Installing:

Copy all the files from the "lib" folder to ANT_HOME/lib (if you are upgrading from a previous 
version please remove any older versions of these libraries).
Copy ivysvnresolver.jar to ANT_HOME/lib.

Upgrading from ivy-svn 1.3 or below:

Remove the contents of your local ivy2 cache (this is located in ~/.ivy2 on Linux, remove the folder and all its contents).
Remove ANT_HOME/lib/ganymed.jar and previous versions of Ivy jar files from ANT_HOME/lib/. 

Source Version
~~~~~~~~~~~~~~

Installing from source:

1. Copy ivysettings.xml.example to ivysettings.xml and configure to suit your Subversion environment.
2. Copy build.properties.example to build.properties and set the values according to your Subversion environment.
3. Run "ant -Dskip.retrieve=true -Dversion=0 install" which will compile the Java source code, build ivysvnresolver.jar 
into build/dist and then copy this and the required 3rd party libraries to your JAVA_HOME/ant/lib folder.

If you are upgrading from a previous version please remove any older versions of the libraries copied across 
in the above step from JAVA_HOME/ant/lib.

Testing that it works:

1. Edit ivy.xml so that it contains dependencies to artifacts in your repository.
2. Run "ant retrieve" and the dependent files should be downloaded to the lib/ folder
3. Make sure the "version" property is set and run "ant publish" and the current version of ivysvnresolver.jar 
should be published to your repository. NOTE: Every time you do a publish you will need to use a different 
version number (or set the property "publish.overwrite").


Third party libraries
~~~~~~~~~~~~~~~~~~~~~
The Ivy Subversion resolver requires the following third-party libraries, the version numbers indicate the versions 
that the Ivy Subversion resolver was built and tested against, your mileage with other versions may vary.

Ivy 2.0 beta 2 (http://ant.apache.org/ivy/)
Trilead SSH-2 For Java build 211 (http://www.trilead.com/Products/Trilead-SSH-2-Java/)
SVNKIt 1.1.8 (http://svnkit.com/)

These files are included in the "lib" folder. 

