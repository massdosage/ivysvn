**IvySvn** is a resolver for [Ivy](http://ant.apache.org/ivy/) 2.0 which uses [Subversion](http://subversion.tigris.org/) for storing artifacts for use in Ivy's publish and retrieve 
operations. Ivy is a dependency manager for Java which can be integrated into [Ant](http://ant.apache.org) builds to allow for retrieval of artifacts (e.g. third-party jar files) 
which your project may depend on as well as publication of artifacts (e.g. your own .jar/.war/.ear/.tgz files etc.) that your build generates. Subversion is not supported "out of the 
box" by Ivy itself, which is the gap that IvySvn aims to fill.

IvySvn can be [downloaded here](https://code.google.com/p/ivysvn/downloads/list).

Please refer to the project's [Wiki](https://github.com/massdosage/ivysvn/wiki) for usage and other information.

The key benefits of using IvySvn for existing Subversion users are:

* Everything is in one place - your source code, dependent libraries and build artifacts are all kept in Subversion.
* No need to set up and maintain a separate HTTP/FTP/etc. repository.
* Ability to use Subversion's binary diff feature, reducing space required for multiple published versions of an artifact.
* Ability to use Subversion revisions for retrieving artifacts, allowing more complex versioning.
* Ability to publish artifacts to Subversion allows easy setup of dependencies between different projects in Subversion.
* Less firewall and network configuration - can be used in environments where HTTP or other traffic is restricted but Subversion is allowed (e.g. build and testing machines). 

If you have any issues, problems, queries, requests, comments or praise relating to IvySvn please send a message to the [IvySvn group](http://groups.google.com/group/ivysvn).

Note: IvySvn 2.1.0 requires >= Java 5.0.

Latest news

* **Project moved from Google Code to Github** (2015-06-31) - with Google Code's [impending closure] (http://google-opensource.blogspot.co.uk/2015/03/farewell-to-google-code.html) 
IvySvn has been moved to a new home in Github. IvySvn isn't actively maintained any more but at least the code is still available for future reference or forking. 
* **2.2.0 release available** (2010-12-20) - 2.2.0 is now available and uses Ivy 2.2.0. Get it [here](http://code.google.com/p/ivysvn/downloads/list), and report any issues to the 
[IvySvn group](http://groups.google.com/group/ivysvn).

#Legal

This project is available under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

Copyright 2008-2015 Last.fm & Mass Dosage
