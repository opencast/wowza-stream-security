# Wowza Stream Security Plugin

This plugin brings Opencast Stream Security to the Wowza Media Server. More information about Opencast Stream Security can be found in the [Opencast Administration Guide](http://docs.opencast.org/latest/admin/)


## Installing Wowza Plugin

1. Install Wowza: [Wowza Media Server](https://opencast.jira.com/wiki/display/MHDOC/Wowza+Media+Server)

1. Make sure your Wowza installation is working by processing a mediapackage. Load the video in your Matterhorn player (engage, theodul etc) and verify that it is using Wowza by looking at the logs/wowzastreamingengine_access.log log file and seeing the accesses.

1. Checkout the Wowza plugin code from the above source repository.

1. Build the plugin jar by going into modules/matterhorn-urlsigning-security-wowza-plugin and running `mvn clean install`

1. This will create a jar file in the modules/matterhorn-urlsigning-security-wowza-plugin/target directory

1. Copy the built jar file from target/matterhorn-urlsigning-security-wowza-plugin-1.0.0-SNAPSHOT.jar into the lib directory in your Wowza installation directory. (Default is: /usr/local/WowzaStreamingEngine/lib/)

1. Checkout the Matterhorn stream security code base

1. Go into the "modules/matterhorn-urlsigning-common" directory and run `mvn clean install`. Copy the "matterhorn-urlsigning-common-2.1-SNAPSHOT.jar file into the "lib" directory in your Wowza installation directory. (Default is: /usr/local/WowzaStreamingEngine/lib/)

1. Download the Binary Apache Commons Codec jar file into your Wowza lib directory from: http://commons.apache.org/proper/commons-codec/download_codec.cgi

1. Download the Binary JSON Simple jar file into your Wowza lib directory from: https://code.google.com/p/json-simple/downloads/list

1. Download the Binary Google Guava jar file into your Wowza lib directory from: https://code.google.com/p/guava-libraries/wiki/Release17

### Version 1.6.x Only

1. Download the Binary Entwine Functional jar file into your Wowza lib directory from: [http://maven.entwinemedia.com/content/repositories/releases/com/entwinemedia/common/functional/1.1.1/functional-1.1.1.jar](http://maven.entwinemedia.com/content/repositories/releases/com/entwinemedia/common/functional/1.1.1/functional-1.1.1.jar)


## Configuring Wowza plugin


#### 1. Add module definition
After running through the Wowza installation instructions you will have a `conf/matterhorn-engage/Application.xml` file. Edit this file and add the following configuration to the Modules section as the last entry:

```
<Module>
  <Name>StreamSecurity</Name>
  <Description>StreamSecurity</Description>
  <Class>org.opencastproject.wowza.StreamSecurityWowzaPlugin</Class>
</Module>
```


#### 2. Add configuration properties

Create a "streamsecurity.properties" file in the Wowza conf directory (Default is: /usr/local/WowzaStreamingEngine/conf/streamsecurity.properties).


It is a java properties file which lists the encryption keys to use that will be passed in the query string parameter as keyId and the right side of the equal sign is the encryption key itself. For example to support the above Matterhorn Stream Security configuration the properties file would look like:

```
demoKeyOne=6EDB5EDDCF994B7432C371D7C274F
demoKeyTwo=C843C21ECF59F2B38872A1BCAA774
```


#### 3. Restart Wowza

Now restart the sever to enable the Stream security by running: `service WowzaStreamingEngine restart`