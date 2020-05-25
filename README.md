# Wowza Stream Security Verification Component

This plugin brings Opencast Stream Security to the Wowza Media Server. More information about Opencast Stream Security can be found in the [Opencast Administration Guide](http://docs.opencast.org/develop/admin/)

## Install Wowza
You need to install [Wowza Streaming Engine](https://www.wowza.com/pricing/installer) on the machine where you want to build the Wowza Stream Security module. This module is tested with Wowza 4.5+. You can use a free trial or developer version to build this module. In fact, as we are onlylinking the libraries from Wowza and do not need to start the software, Wowza does not even have to be registered.

Please follow the installation instructions from Wowza.

Usually Wowza should now be installed to `/usr/local/WowzaStreamingEngine`.

## Install Wowza Stream Security Component
The Wowza Stream Security component is based on Java. Compiling it is therefore very similar to compiling Opencast. Note that in order to build the Wowza component, dependencies will be linked from an online Nexus repo or your local M2_REPO if it contains the dependencies already. If you are not running an Opencast release version that is provided by the nexus, you have to build Opencast first on your local machine.

Check out the Wowza Stream Security code first. 

    git clone https://bitbucket.org/opencast-community/wowza-stream-security-plugin.git
    cd wowza-stream-security-plugin

You need to have a Java build environment with JDK and Maven on your machine. If have this already you can skip these steps!

    yum install git java-1.8.0-openjdk java-1.8.0-openjdk-devel
    cd /opt
    wget http://apache.sunsite.ualberta.ca/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz
    tar -xvf apache-maven-3.3.3-bin.tar.gz
    export PATH=/opt/apache-maven-3.3.3/bin:$PATH

Build and Install the Wowza Stream Security Component

    mvn clean install -Dopencast.version=<your opencast version> [-Dpackage.dir=<output directory>] [-Dwowza.path=<path to your Wowza installation>]

* `-Dopencast.version` specifies the Opencast version that you are using. Make sure this version matches to the Opencast version that you are using (i.e. `-Dopencast.version=4.2`)
* `-Dpackage.dir` specifies the output directory for the build plugin. This parameter is optionally, with predefined value of `${project.basedir}/build/`
* `-Dwowza.path` specifies the path to your local Wowza installation. If you installed Wowza to the default path you do not need to set this.

# Install Plugin
The build module and all the dependencies will be copied to the output directory you specified with `package.dir` parameter or per default to the `build` directory. Next you have to copy the jar files from the output to Wowza lib directory, default path `/usr/local/WowzaStreamingEngine/lib`.

    cp build/*.jar /usr/local/WowzaStreamingEngine/lib/

## Configuration of Wowza
You can use the Wowza Engine Manager on http://localhost:8088 to setup the Opencast Application.

Select "Applications" from the main menu and click on VOD (Video On Demand).

![new application](/img/add_application.png)

Then enter a name for you application, i.e. "opencast".

![new application dialog](/img/add_application2.png)

Within the setup details of the application it is recommended to deactivate all unsupported "Playback Types" and only activate MPEG-DASH, Apple HLS and Adobe RTMP.
You also need to set the content directory to the `org.opencastproject.streaming.directory` defined in the Opencast configuration.

![application setup](/img/application_details.png)

Now you need to add an addition module to your application. Open the "Modules" tab, choose "Edit" and "+ Add Module ..." 

![module section](/img/application_modules.png)

The new module needs these settings:

* **Name**: StreamSecurity
* **Description**: Opencast Stream Security Module
* **Fully Qualified Class Name**: org.opencastproject.wowza.StreamSecurityWowzaPlugin

![new module](/img/application_modules_new.png)

Now you need to save these settings and restart the application.

![module overview](/img/application_modules_edit.png)

![restart](/img/restart.png)



The signing keys and secrets have to be configured in a separate file named **streamsecurity.properties** which must be placed inside Wowza’s configuration directory (usually `/usr/local/WowzaStreamingEngine/conf`). 

The file simply contains a collection of key-value pairs, one per line:

    demoKeyOne=6EDB5EDDCF994B7432C371D7C274F
    demoKeyTwo=C843C21ECF59F2B38872A1BCAA774

As with the HTTPd component, the entries in this file need to have the same values as used for the Signing Providers configuration.

## Configuration for HLS and Dash

URLs can be whitelisted by defining regular expressions in a file called pugin.properties, which will also have to be placed in the Wowza's configuration directory. URLs matching this pattern will not be checked for a correct signature. The patterns will be defined in the file in the following way:

	whitelist.<name>=<pattern>

This can be used to whitelist the segment URLs for HLS and Dash streaming since they are often not signed correctly or not signed at all. Example:

	whitelist.hls=.*media_.*\.ts.*
	whitelist.hls2=.*chunklist_.*\.m3u8.*
	whitelist.mpd=.*segment_.*\.m4s.*

## Configuration of Opencast

To secure the RTMP links the `$OPENCAST_HOME/etc/org.opencastproject.security.urlsigning.provider.impl.WowzaUrlSigningProvider.cfg` has to be adjusted:

    id.1=demoKeyOne
    key.1=6EDB5EDDCF994B7432C371D7C274F
    url.1=rtmp


But as the adaptive streaming URLs are HTTP links you have to use the `$OPENCAST_HOME/etc/org.opencastproject.security.urlsigning.provider.impl.GenericUrlSigningProvider.cfg` configuration to secure these links:

    id.2=demoKeyOne
    key.2=6EDB5EDDCF994B7432C371D7C274F
    url.2=http://localhost:1935/secure
    organization.2=mh_default_org

