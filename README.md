## Overview ##
The protex-jenkins plugin allows the User to run scans during a Jenkins Build as a Post-Build Action.

## Build ##

[![Build Status](https://travis-ci.org/blackducksoftware/protex-jenkins.svg?branch=master)](https://travis-ci.org/blackducksoftware/protex-jenkins)
[![Coverage Status](https://coveralls.io/repos/github/blackducksoftware/protex-jenkins/badge.svg?branch=master)](https://coveralls.io/github/blackducksoftware/protex-jenkins?branch=master)

## Where can I get the latest release? ##
You can download the latest source from GitHub: https://github.com/blackducksoftware/protex-jenkins. 

You can download the latest release from GitHub.

## Documentation ##
All documentation for protex-jenkins can be found in project:  https://github.com/blackducksoftware/protex-jenkins/wiki

## License ##
Apache License 2.0

Changes
=======

Version : v1.4.1

* Open Source
* Fixed bug with duplicate links for the Protex Full Scan

Version : v1.4.0

* Exclude the bdstool log files from scans
* Add custom update site on installation, recieve plugin updates
* New option to force a full rescan
* Enabled the plugin to be run in parallel builds.

Version : v1.3.2

* Fixes issue with some Jobs hanging
* Enables Jenkins jobs to be aborted

Version : v1.3.1

* Fix an issue, where the password was logged in clear text, when bdstool failed a certain way
* Retirement of 32 bit VM support
* Improve warning for failed login, to cover cases where LDAP is used
* Expose Protex Project/Template as environment variable for further post build steps to use
	 
Version : v1.3.0

* Add optional config of -Xmx memory of the bdstool sub process
* Fix issue with timeout on large projects in the updateProject step (caused by synchromuous BOM refresh in the SDK)
* Fix issue with .bdstool overwrite when multiple protex scans are running on the same Jenkins slave (or master)
* Now the bdstool.log is in ${workspace}/BDSToolLog/bdstool.log, where it can be added to a builds asset, preserving it after the job run

Version : v1.2.0

* Removed the migration tools
* Updated to use the Protex 7.0 Api's instead of the 6.2 Api's
* Fixed support for Pass through proxies
* Added support for proxies with Basic authentication (can not support Digest authentication due to lack of support for it in CXF)


Version : v1.1.0

* New Plugin installation
  * Can be installed at the same time as the old plugin, useful for migration purposes
* Migration Action to migrate from the Old configuration to the New Protex Job configuration per job
  * If the Old configuration has the scan disabled then in the Migration screen the User will get a warning that the scan will run with every build (assuming the build was successful) using the new configuration
  * If the Old configuration does not start with $WORKSPACE or ${WORKSPACE} AND/OR contains escape strings (i.e. '..'), the User will be warned in the Migration screen that the new configuration will only scan targets within the workspace of the Job.
  * The Protex Server Url will be added to global list of Servers if the Url is not already present, the default Server Name is 'Protex Url'
  * The Protex User Name and Password will be added to the Credential Store in the global domain if they are not currently present, the default Description is 'Protex Credentials'
  * If the Old configuration contains the Failure conditions they will be added along with the new configuration as a separate post-build action called 'Protex- Failure Condtions'
* Global configuration:
  * Define multiple Protex Servers using a Name and Url
  * Specify connection timeout per server
  * Allow User to populate Credentials list and test connection to the server
* Job Configuration:
  * Protex Server is now chosen from the list of Globally defined servers
  * Credentials are now chose from a drop down list, if the correct credentials are not found the User can easily add to the list
  * The Protex Project will be created during the build if:
    * The Project does not currently exist in Protex
    * If a Template Project Name is provided a clone of that Project is made
    * Else a Project is created with default settings
  * The User can manually create the Project using a Create Project button
    * Only if a Project with the specified Project Name does not already exist
    * If a Template Project Name is specified and exists, 
      * a clone of that Project will be made with the specified Project Name 
    * Else 
      * a Project is created with default settings
  * "Scan Protex Project" option removed, Configuring the plugin indicates that you want the Protex scan to run during this build
    * Scan will only run if the Build is Successful
    * "Do not fail the build even if Protex actions fail?" option removed, This plugin will not fail the build for any technical failures 
    		(i.e. Unsupported java, can not connect to the Protex server, invalid credentials, etc.), instead the Build will be set to Unstable
  * "Protex Project Source Path" leaving this field empty points to this Jobs workspace, any path provided will be appended to the Path of the workspace.
    * The User should not be able to scan targets outside the workspace.
  * "Fail build if code has pending identifications?" and "Fail build if code has license violations?" moved to a separate Post-build Action.
    		These options will fail the Build. If the User has checked "Fail build if code has pending identifications?" and there are pending identifications on the project, the Build will be set to Failure.
    	
RELEASE NOTES
=============

Requires : Protex 6.2 or Greater and Jenking 1.509.4 or greater
	*** If your are using Protex 7.0 or greater, then a 1.7 JVM is required

Known issues
============
If you encounter a Perm out of memory exception, please verify your application server settings for Java Memory
FATAL: PermGen space
java.lang.OutOfMemoryError: PermGen space
…..

Based on your environment, you may have to increase your Mx and MaxPermSize values.
i.e. on Tomcat be sure you have higher values in setenv.sh (or bat) 
export JAVA_OPTS="-Xmx512m -XX:MaxPermSize=256m"

Increase your values and restart your application server.

Installation
============

1. Upload the Plugin file protex-jenkins.hpi
  - Login to Jenkins --> Manage Jenkins --> Manage Plugins --> 	Advanced --> Upload Plugin
  - Select the file protex-jenkins.hpi and follow instructions...
2. Configure Protex  Global Settings 
  * Login to Jenkins --> Manage Jenkins --> Configure System
  * Look for the Black Duck Protex section
  * Fill in the information for any Protex Servers you may be using:
    * Provide a Server Name for each Protex instance
    * Provide a Url for that Server
    * In the Advanced section you can change the SDK timeout, default is 300 seconds
    * You can also test the connection by selecting some credentials to use and pressing the Test Connection button

Installation is done.

Usage
=====

Create or access a Jenkins Job and add the post-build action : Protex - Create and Scan Actions
Then fill the required information

This build step allows to integrate Black Duck Protex with 2 mains actions :

- Create Protex Project :
  - If the Protex Project Name provided already exists in Protex then you do not need to use the Create Project button and the Project will be used during the Build
  - If the Name provided does not exist in Protex then you can create it one time using the Create Project button
  - If the Name contains a variable like $BUILD_TAG then the Project will get created during the Build unless it already exists in which case it will be updated.
  - If a Protex Project Template Name* is provided and contains a variable then a clone of the template is made during the Build unless the Project already exists
			i.e. :
				MyPrefix_$BUILD_TAG_MySuffix or $BUILD_TAG, etc...
  - If the Protex project name does not exist :
    - If a Protex Project Template Name* is provided and exists then a clone of the template is made using the Create Project button
    - Else (no Protex Project Template Name provided, or does not exists) a Protex Project will be created using the default settings

  - Scan the Protex Project using the provided Source Path, the Jenkins workspace folder is the base of this path.
		Scans will only run on targets within the workspace of the current Job. 
				For instance : src/main/java , src/, etc...

Once configured, Click Save and Build Now :)

Enjoy!
		
* Regular Protex Project used as a template
