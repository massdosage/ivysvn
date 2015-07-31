# Unit testing #
This page assumes you have already followed the steps described in BuildingFromSource and can successfully build the ivysvnresolver.jar file.

## Set up a test Subversion repository ##
The unit tests publish and retrieve data from Subversion and thus require a test Subversion repository. The user that runs the test must have read and write access to this repository. This repository should be used for testing only and not contain any production data as an errant unit test might delete everything in the repository (being svn of course you will have a backup but still!)

You can either set up a repository to be tested using svnserver (i.e. using the svn:// protocol) or using Apache httpd (i.e using the http:// protocol). There are some differences in how SvnKit (the library used by IvySvn to communicate with Subversion) deals with the different repository types so ideally unit tests should be run successfully against both before submitting a patch.

There are many different ways to set up Subversion. The steps below are the bare minimum needed to get the repository working for unit tests on Linux. Feel free to modify them to suit your environment or read through the relevant sections in [Version Control With Subversion](http://svnbook.red-bean.com/).

### Create a repository ###
The steps below create a repository with it's root located at
```
/opt/ivysvntest/repo/root/
```
feel free to modify the paths to suit your own set up.
Create the repository root folder, modify access permissions and create the Subversion repository root:
```
sudo mkdir -p ivysvntest/repo/root/
sudo chmod a+rwx ivysvntest/repo/root/
svnadmin create /opt/ivysvntest/repo/root/
```

#### Configure repository for svn access ####
Edit the file
```
/opt/ivysvntest/repo/root/conf/svnserv.conf
```
and add the following in the `[`general`]` section:
```
password-db = passwd
```
Edit the file
```
/opt/ivysvntest/repo/root/conf/passwd
```
and add the follwing in the `[`users`]` section:
```
testuser=testpassword
```
Start the Subversion server as a daemon process:
```
svnserve -d
```

#### Configure repository for http access ####
Install apache2, mod\_dav and mod\_dav\_svn. On Ubuntu:
```
apt-get install apache2
apt-get install libapache2-svn
```
Generate a test user and password for http access:
```
htpasswd -c /opt/ivysvntest/http-svn-auth-file testuser
```
and enter "testpassword" when prompted.
Edit Apache's httpd.conf file (located under /etc/apache2 on Ubuntu) and add the following:
```
<Location /svn/repos>
  DAV svn
  SVNPath /opt/ivysvntest/repo/root
  AuthType Basic
  AuthName "Subversion repository"
  AuthUserFile /opt/ivysvntest/http-svn-auth-file
  Require valid-user
</Location>
```
The Location is the path you will need to put in your test.properties file. If you get permission denied errors when running the unit tests you can  either delve into sorting these out with proper auth or, seeing as this is just for unit tests, you can take a shortcut:
```
chmod -R a+rw /opt/ivysvntest/
```

## Configure the unit tests ##
To run the unit tests you need to specify the location of the Subversion repository and the protocol to use to access it as well as authentication information. To do this copy the file
```
src/test/conf/test.properties.example
```
to
```
src/test/conf/test.properties
```
Edit this file so that it contains the authentication information configured above:
```
svn.user.name=testuser
svn.user.password=testpassword
```
Then specify whether you want to run the tests via svnserve:
```
svn.repository.root=svn://localhost/opt/ivysvntest/repo/root/
```
**or** via httpd:
```
svn.repository.root=http://localhost/svn/repos
```

## Run the unit tests ##
Run
```
ant test
```
and the unit tests will be run against the configured repository. The results will be output to
```
build/junit/junit-noframes.html
```