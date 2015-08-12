# Wowza Stream Security Verification Component

This plugin brings Opencast Stream Security to the Wowza Media Server. More information about Opencast Stream Security can be found in the [Opencast Administration Guide](http://docs.opencast.org/latest/admin/)

## Install External Dependencies
In order to run the Stream Security component, some external dependencies exist that need to be placed in the Wowza’s lib directory.

* [Apache Commons Codec](http://commons.apache.org/proper/commons-codec/download_codec.cgi) - Tested with version 1.10 
* [JSON Simple](https://github.com/fangyidong/json-simple) - Tested with version 1.1
* [Google Guava](https://github.com/google/guava/wiki/Release17) - Tested with version 17.0

The installation steps are:
    cd /tmp/
    wget http://apache.parentingamerica.com//commons/codec/binaries/commons-codec-1.10-bin.tar.gz
    tar -xvf commons-codec-1.10-bin.tar.gz
    cp commons-codec-1.10/commons-codec-1.10.jar /usr/local/WowzaStreamingEngine/lib/
    wget https://github.com/fangyidong/json-simple/archive/tag_release_1_1.tar.gz
    tar -xvf tag_release_1_1.tar.gz
    cp json-simple-tag_release_1_1/lib/json_simple-1.1.jar /usr/local/WowzaStreamingEngine/lib/
    wget -O guava-17.0.jar http://search.maven.org/remotecontent?filepath=com/google/guava/guava/17.0/guava-17.0.jar
    mv guava-17.0.jar /usr/local/WowzaStreamingEngine/lib/

Opencast Version 1.6.x Only
Download the Binary Entwine Functional jar file into your Wowza lib directory.
    cd /tmp/
    wget http://maven.entwinemedia.com/content/repositories/releases/com/entwinemedia/common/functional/1.1.1/functional-1.1.1.jar
    mv functional-1.1.1.jar /usr/local/WowzaStreamingEngine/lib/

## Install Opencast Dependencies
In addition to the third party libraries, the Wowza Stream Security component also requires some bundles from the Opencast project therefore, the following dependency needs to be copied to Wowza’s lib directory:
* matterhorn-urlsigning-common-2.1-SNAPSHOT.jar
The steps for building matterhorn-urlsigning-common-2.1-SNAPSHOT.jar on the Wowza server itself are:
    yum install git java-1.8.0-openjdk java-1.8.0-openjdk-devel
    cd /opt
    wget http://apache.sunsite.ualberta.ca/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz
    tar -xvf apache-maven-3.3.3-bin.tar.gz
    export PATH=/opt/apache-maven-3.3.3/bin:$PATH
    git clone https://bitbucket.org/opencast-community/matterhorn.git
    cd matterhorn
    [[ Checkout the branch of matterhorn you are interested in, e.g. ‘git checkout f/MH-10729-stream-security’, ‘git checkout f/MH-10729-stream-security-1.6.x’ etc. ]]
    mvn clean install -Pworker-standalone,serviceregistry,workspace (It can be any profile that contains matterhorn-urlsigning-common)
    cp modules/matterhorn-urlsigning-common/target/matterhorn-urlsigning-common-*-SNAPSHOT.jar /usr/local/WowzaStreamingEngine/lib/

## Install Wowza Stream Security Component
The Wowza Stream Security component is based on Java. Compiling it is therefore very similar to compiling Opencast. Note that in order to build the Wowza component, either local or online access to Opencast’s matterhorn-urlsigning-common Maven artifact is required. Last but not least, make sure the project POM references the correct version of Opencast that you are running (2.1 or 1.6 at the time of writing).

Once the source code is checked out, it can be built with mvn clean install. The resulting lib called matterhorn-urlsigning-common-2.1-SNAPSHOT.jar needs to be copied to Wowza’s lib directory.
    yum install git java-1.8.0-openjdk java-1.8.0-openjdk-devel
    cd /opt
    wget http://apache.sunsite.ualberta.ca/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz
    tar -xvf apache-maven-3.3.3-bin.tar.gz
    export PATH=/opt/apache-maven-3.3.3/bin:$PATH
    git clone https://bitbucket.org/entwinemedia/wowza-stream-security-plugin.git
    cd wowza-stream-security-plugin/modules/matterhorn-urlsigning-security-wowza-plugin

Replace the version of the plugin if you are using Opencast 1.6
    sed -i 's@<version>2.1-SNAPSHOT</version>@<version>1.6.x-SNAPSHOT</version>@g' pom.xml

Build and Install the Wowza Stream Security Component
    mvn clean install
    cp target/matterhorn-urlsigning-security-wowza-plugin-1.0.0-SNAPSHOT.jar /usr/local/WowzaStreamingEngine/lib/

## Configuration
The component needs to be activated in the Wowza application by altering the configuration file (Application.xml) of the Wowza application used for Opencast like so:

```xml
<Application>
  ...
  <Modules>
    ...
    <Module>
      <Name>StreamSecurity</Name>
      <Description>StreamSecurity</Description>
      <Class>org.opencastproject.wowza.StreamSecurityWowzaPlugin</Class>
    </Module>
  </Modules>
  ...
</Application>
```

Alternatively, you can alter the configuration of the Wowza application used for Opencast in the Wowza EngineManager web interface on Applications -> Your existing Wowza application used for Opencast -> Modules -> Edit -> Add Module. The values to enter in the following PopUp Window are the same as for editing the XML file.
To achieve this, the Wowza EngineManager user used to configure the Application must be given access to advanced properties and features - this can be configured on Server -> Users -> Edit user.

Regardless if you are modifying the application configuration in the XML file or with the EngineManager, you have to restart the Wowza application for the changes to take effect.

The signing keys and secrets have to be configured in a separate file named **streamsecurity.properties** which must be placed inside Wowza’s configuration directory (usually /usr/local/WowzaStreamingEngine/conf). 

The file simply contains a collection of key-value pairs, one per line:

    demoKeyOne=6EDB5EDDCF994B7432C371D7C274F
    demoKeyTwo=C843C21ECF59F2B38872A1BCAA774

As with the HTTPd component, the entries in this file need to have the same values as used for the Signing Providers configuration.
