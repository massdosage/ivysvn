IvySvn is a resolver for [Ivy](http://ant.apache.org/ivy) 2.0 which uses Subversion for storing artifacts for use in Ivy's publish and retrieve operations. Ivy is a dependency manager for Java which can be integrated into Ant builds to allow for retrieval of artifacts (e.g. third-party jar files) which your project may depend on as well as publication of artifacts (e.g. your own .jar/.war/.ear/.tgz files etc.) that your build generates. Subversion is not supported "out of the box" by Ivy itself, which is the gap that IvySvn aims to fill.

The key benefits of this IvySvn for existing Subversion users are:

  * Everything is in one place - your source code, dependent libraries and build artifacts are all kept in Subversion.
  * No need to set up and maintain a separate HTTP/FTP/etc. repository.
  * Ability to use Subversion's binary diff feature, reducing space required for multiple published versions of an artifact.
  * Ability to use Subversion revisions for retrieving artifacts, allowing more complex versioning.
  * Ability to publish artifacts to Subversion allows easy setup of dependencies between different projects in Subversion.
  * Less firewall and network configuration - can be used in environments where HTTP or other traffic is restricted but Subversion is allowed (e.g. build and testing machines).

If you have any issues, problems, queries, requests, comments or praise relating to IvySvn please send a message to the [IvySvn group](http://groups.google.com/group/ivysvn) or contact the project [admin](http://code.google.com/u/massdosage/).

Note: IvySvn 2.1.0 requires >= Java 5.0.

---


**Latest news**
  * **2.2.0 release available** (2010-12-20) - 2.2.0 is now available and uses Ivy 2.2.0. Get it [here](http://code.google.com/p/ivysvn/downloads/list), and report any issues to the [IvySvn group](http://groups.google.com/group/ivysvn).
  * **2.1.0 release available** (2010-04-12) - 2.1.0 finally out the door! Get it [here](http://code.google.com/p/ivysvn/downloads/list), and report any issues to the [IvySvn group](http://groups.google.com/group/ivysvn).
  * **2.0.0 release available** (2009-05-14) - 2.0.0 locked and loaded and good to go! Get it [here](http://code.google.com/p/ivysvn/downloads/list), and report any issues to the [IvySvn group](http://groups.google.com/group/ivysvn).
