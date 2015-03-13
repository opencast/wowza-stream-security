package ch.entwine.wowza;

import org.opencastproject.urlsigning.common.ResourceRequest;
import org.opencastproject.urlsigning.common.ResourceRequest.Status;
import org.opencastproject.urlsigning.utils.ResourceRequestUtil;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.HTTPStreamerSessionSmoothStreamer;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.IMediaStream;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * The Wowza plugin to determine if a resource request for a signed url is valid.
 */
public class EntwineStreamSecurityWowzaPlugin extends ModuleBase {
  private static final String CONF_KEYS_PROPERTIES_LOCATION = "conf/keys.properties";

  /** The keys to use to encrypt the signatures. */
  private static Properties properties = new Properties();

  public EntwineStreamSecurityWowzaPlugin() {
    super();
    if (properties.size() == 0) {
      FileInputStream fis = null;
      try {
        String currentJarLocation = EntwineStreamSecurityWowzaPlugin.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI().getPath();
        String wowzaDirectory = new File(new File(currentJarLocation).getParent()).getParent();
        File keyProperties = new File(wowzaDirectory, CONF_KEYS_PROPERTIES_LOCATION);
        getLogger().debug("Loading encryption key properties from: " + keyProperties.getAbsolutePath());
        if (!keyProperties.exists()) {
          getLogger().warn(
                  "The encryption key properties file at " + keyProperties.getAbsolutePath()
                          + " is missing so all signed urls will be rejected.");
          return;
        }
        if (!keyProperties.canRead()) {
          getLogger()
                  .warn("Unable to read the encryption key properties file at "
                          + keyProperties.getAbsolutePath()
                          + " is missing so all signed urls will be rejected. Please check the permissions for the file.");
          return;
        }
        fis = new FileInputStream(keyProperties);
        properties.load(fis);
      } catch (IOException e) {
        getLogger().error("Unable to load the encryption keys because: " + ExceptionUtils.getStackTrace(e));
      } catch (URISyntaxException e) {
        getLogger().error("Unable to load the encryption keys because: " + ExceptionUtils.getStackTrace(e));
      } finally {
        try {
          if (fis != null) {
            fis.close();
          }
        } catch (IOException e) {
          getLogger().warn("Unable to close the encryption keys because: " + ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }

  private static void handleClient(IClient client) {
    Status accepted = authenticate(client.getQueryStr(), client.getIp(), client.getUri(), properties);
    switch (accepted) {
      case BadRequest:
        getLogger().warn("The request was rejected because it was a bad request.");
        client.rejectConnection("The request was rejected because it was a bad request.");
        break;
      case Forbidden:
        getLogger().warn(
                "The credentials provided were not correct so the client is forbidden from seeing the resource..");
        client.rejectConnection("Forbidden");
        break;
      case Gone:
        getLogger().warn("The resource is not currently available and is gone.");
        client.rejectConnection("The resource is currently not available.");
      case Ok:
        getLogger().debug("The resource is allowed to be viewed.");
    }
  }

  public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
    getLogger().trace("onConnect: " + client.getClientId());
    handleClient(client);
  }

  public void onConnectAccept(IClient client) {
    getLogger().trace("onConnectAccept: " + client.getClientId());
    handleClient(client);
  }

  public void onStreamCreate(IMediaStream stream) {
    getLogger().trace("onStreamCreate: " + stream.getSrc());
    handleClient(stream.getClient());
  }

  public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
    getLogger().trace("onHTTPSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession) {
    getLogger().trace("onHTTPCupertinoStreamingSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onHTTPSmoothStreamingSessionCreate(HTTPStreamerSessionSmoothStreamer httpSession) {
    getLogger().trace("onHTTPSmoothStreamingSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onRTPSessionCreate(RTPSession rtpSession) {
    getLogger().trace("onRTPSessionCreate: " + rtpSession.getSessionId());
    handleClient(rtpSession.getRTSPStream().getStream().getClient());
  }

  public void onCall(String handlerName, IClient client, RequestFunction function, AMFDataList params) {
    getLogger().trace("onCall: " + handlerName);
    handleClient(client);
  }

  protected static Status authenticate(String queryString, String clientIp, String resourceUri, Properties properties) {
    try {
      getLogger().trace("Query String: " + queryString);
      getLogger().trace("Client Ip: " + clientIp);
      getLogger().trace("Resource: " + resourceUri);
      ResourceRequest request = ResourceRequestUtil.resourceRequestFromQueryString(queryString, clientIp, resourceUri,
              properties);
      getLogger().trace("Encoded Policy: " + request.getEncodedPolicy());
      getLogger().trace("Encrypt Id: " + request.getEncryptionKeyId());
      getLogger().trace("Signature: " + request.getSignature());
      getLogger().trace("Status: " + request.getStatus());
      if (request != null && request.getPolicy() != null) {
        getLogger().trace("BaseURL: " + request.getPolicy().getBaseUrl());
        getLogger().trace("Valid Until: " + request.getPolicy().getValidUntil());
      }
      return request.getStatus();
    } catch (Throwable t) {
      getLogger().error("Unable to process request because: " + ExceptionUtils.getStackTrace(t));
      return Status.BadRequest;
    }
  }

}
