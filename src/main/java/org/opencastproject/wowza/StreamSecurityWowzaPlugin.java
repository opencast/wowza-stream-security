/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.wowza;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The Wowza plugin to determine if a resource request for a signed url is valid.
 */
public class StreamSecurityWowzaPlugin extends ModuleBase {
  private static final String CONF_KEYS_PROPERTIES_LOCATION = "conf/streamsecurity.properties";
  private static final String CONF_PLUGIN_PROPERTIES_LOCATION = "conf/plugin.properties";

  /** The keys to use to encrypt the signatures. */
  private static Properties properties = new Properties();

  /** The plugin settings */
  private static Properties pluginSettings = new Properties();
  private static List<String> whitelist = new ArrayList<String>();

  public StreamSecurityWowzaPlugin() {
    super();

    // read in the plugin settings
    if (pluginSettings.size() == 0) {
      FileInputStream fis = null;
      try {
        String currentJarLocation = StreamSecurityWowzaPlugin.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI().getPath();
        String wowzaDirectory = new File(new File(currentJarLocation).getParent()).getParent();
        File pluginProperties = new File(wowzaDirectory, CONF_PLUGIN_PROPERTIES_LOCATION);
        if (getLogger().isDebugEnabled()) {
          getLogger().debug("Loading plugin settings from: " + pluginProperties.getAbsolutePath());
        }
        if (!pluginProperties.exists()) {
          getLogger().warn("The plugin settings file at " + pluginProperties.getAbsolutePath() + " is missing.");
          return;
        }
        if (!pluginProperties.canRead()) {
          getLogger().warn("Unable to read the plugin settings file at " + pluginProperties.getAbsolutePath()
                          + ". Please check the permissions for the file.");
          return;
        }
        fis = new FileInputStream(pluginProperties);
        pluginSettings.load(fis);

        // populate whitelist
        for (String propertyName: pluginSettings.stringPropertyNames()) {
          if (propertyName.contains("whitelist")) {
            whitelist.add(pluginSettings.getProperty(propertyName));
          }
        }

      } catch (IOException e) {
        getLogger().error("Unable to load plugin settings because: ", e);
      } catch (URISyntaxException e) {
        getLogger().error("Unable to load plugin settings because: ", e);
      } finally {
        try {
          if (fis != null) {
            fis.close();
          }
        } catch (IOException e) {
          getLogger().warn("Unable to close plugin settings because: ", e);
        }
      }
    }

    if (properties.size() == 0) {
      FileInputStream fis = null;
      try {
        String currentJarLocation = StreamSecurityWowzaPlugin.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI().getPath();
        String wowzaDirectory = new File(new File(currentJarLocation).getParent()).getParent();
        File keyProperties = new File(wowzaDirectory, CONF_KEYS_PROPERTIES_LOCATION);
        if (getLogger().isDebugEnabled()) {
          getLogger().debug("Loading encryption key properties from: " + keyProperties.getAbsolutePath());
        }
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
        getLogger().error("Unable to load the encryption keys because: ", e);
      } catch (URISyntaxException e) {
        getLogger().error("Unable to load the encryption keys because: ", e);
      } finally {
        try {
          if (fis != null) {
            fis.close();
          }
        } catch (IOException e) {
          getLogger().warn("Unable to close the encryption keys because: ", e);
        }
      }
    }
  }

  /**
   * Reject or allow a request that has an {@link IClient}
   *
   * @param client
   *          The client for the request.
   * @param request
   *          The request to determine if it should be logged and rejected.
   */
  private static void handleClient(IClient client, ResourceRequest request) {
    logResourceRequest(request);
    switch (request.getStatus()) {
      case BadRequest:
        getLogger().warn(request.getRejectionReason());
        client.rejectConnection("The request was rejected because it was a bad request.");
        break;
      case Forbidden:
        getLogger().warn(request.getRejectionReason());
        client.rejectConnection("Forbidden");
        break;
      case Gone:
        getLogger().warn(request.getRejectionReason());
        client.rejectConnection("The resource is currently not available.");
        break;
      case Ok:
        getLogger().debug("The resource is allowed to be viewed.");
    }
  }

  /**
   * Log a request based upon its result.
   *
   * @param request
   *          The {@link ResourceRequest} with the status of the authentication.
   */
  private static void logResourceRequest(ResourceRequest request) {
    switch (request.getStatus()) {
      case BadRequest:
        getLogger().warn(request.getRejectionReason());
        break;
      case Forbidden:
        getLogger().warn(request.getRejectionReason());
        break;
      case Gone:
        getLogger().warn(request.getRejectionReason());
        break;
      case Ok:
        getLogger().trace("The resource is allowed to be viewed.");
    }
  }

  public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("onConnect: " + client.getClientId());
    }
  }

  public void onConnectAccept(IClient client) {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("onConnectAccept: " + client.getClientId());
    }
  }

  public void onStreamCreate(IMediaStream stream) {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("onStreamCreate: " + stream.getSrc());
    }
  }

  /**
   * Callback from Wowza when a request is made over HTTP for a resource.
   *
   * @param httpSession
   *          The details of the request for the resource.
   */
  public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
    try {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("onHTTPSessionCreate: " + httpSession.getSessionId());
      }
      String resourceUri = httpSession.getUri();
      ResourceRequest resourceRequest = authenticate(httpSession.getQueryStr(), httpSession.getIpAddress(),
              resourceUri, properties);
      logResourceRequest(resourceRequest);
      if (resourceRequest.getStatus() != ResourceRequest.Status.Ok) {
        httpSession.rejectSession();
      }
    } catch (Throwable t) {
      getLogger().error("Unable to play http session.", t);
    }
  }

  public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession) {
    try {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("onHTTPCupertinoStreamingSessionCreate: " + httpSession.getSessionId());
      }
      String resourceUri = httpSession.getUri();
      ResourceRequest resourceRequest = authenticate(httpSession.getQueryStr(), httpSession.getIpAddress(),
              resourceUri, properties);
      logResourceRequest(resourceRequest);
      if (resourceRequest.getStatus() != ResourceRequest.Status.Ok) {
        httpSession.rejectSession();
      }
    } catch (Throwable t) {
      getLogger().error("Unable to play cupertino http session.", t);
    }
  }

  public void onHTTPSmoothStreamingSessionCreate(HTTPStreamerSessionSmoothStreamer httpSession) {

    try {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("onHTTPSmoothStreamingSessionCreate: " + httpSession.getSessionId());
      }
      String resourceUri = httpSession.getUri();
      ResourceRequest resourceRequest = authenticate(httpSession.getQueryStr(), httpSession.getIpAddress(),
              resourceUri, properties);
      logResourceRequest(resourceRequest);
      if (resourceRequest.getStatus() != ResourceRequest.Status.Ok) {
        httpSession.rejectSession();
      }
    } catch (Throwable t) {
      getLogger().error("Unable to play smooth http session.", t);
    }
  }

  public void onHTTPSanJoseStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession) {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("onHTTPSanJoseStreamingSessionCreate: " + httpSession.getSessionId());
    }
    try {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("onHTTPSanJoseStreamingSessionCreate: " + httpSession.getSessionId());
      }
      String resourceUri = httpSession.getUri();
      ResourceRequest resourceRequest = authenticate(httpSession.getQueryStr(), httpSession.getIpAddress(),
              resourceUri, properties);
      logResourceRequest(resourceRequest);
      if (resourceRequest.getStatus() != ResourceRequest.Status.Ok) {
        httpSession.rejectSession();
      }
    } catch (Throwable t) {
      getLogger().error("Unable to play san jose http session.", t);
    }
  }


  /**
   * Callback function when Wowza has a new request for an RTP session.
   *
   * @param rtpSession
   *          The details of the RTP session making the request for a stream.
   */
  public void onRTPSessionCreate(RTPSession rtpSession) {
    try {
      if (getLogger().isTraceEnabled()) {
        getLogger().trace("onRTPSessionCreate: " + rtpSession.getSessionId());
      }
      String resourceUri = rtpSession.getUri().replaceFirst(
              rtpSession.getAppInstance().getApplication().getName() + "/", "");
      ResourceRequest resourceRequest = authenticate(rtpSession.getQueryStr(), rtpSession.getIp(), resourceUri,
              properties);
      logResourceRequest(resourceRequest);
      if (resourceRequest.getStatus() != ResourceRequest.Status.Ok) {
        rtpSession.rejectSession();
      }
    } catch (Throwable t) {
      getLogger().error("Unable to play rtp session.", t);
    }
  }

  public void onCall(String handlerName, IClient client, RequestFunction function, AMFDataList params) {
    if (getLogger().isTraceEnabled()) {
      getLogger().trace("onCall: " + handlerName);
    }
  }

  /**
   * Callback function from Wowza for when a stream is played. Used by RTMP.
   *
   * @param client
   *          The client that is making the request to play.
   * @param function
   *          The {@link RequestFunction}
   * @param params
   *          The parameters for the request.
   */
  public void play(IClient client, com.wowza.wms.request.RequestFunction function, AMFDataList params) {
    try {
      String streamName = params.getString(PARAM1);
      String queryStr = "";
      int streamQueryIdx = streamName.indexOf("?");
      if (streamQueryIdx >= 0) {
        queryStr = streamName.substring(streamQueryIdx + 1);
        streamName = streamName.substring(0, streamQueryIdx);
      }
      ResourceRequest request = authenticate(queryStr, client.getIp(), client.getUri(), properties);
      handleClient(client, request);
      if (ResourceRequest.Status.Ok.equals(request.getStatus())) {
        invokePrevious(client, function, params);
      }
    } catch (Throwable t) {
     getLogger().error("Unable to play media:", t);
    }
  }

  /**
   * Check if a resource uri needs to be signed
   *
   * @param resourceUri The uri to check
   * @return true if the uri is whitelisted, ergo does not need to be signed, false otherwise.
   */
  private static boolean isWhitelisted(String resourceUri) {
    for (String whitelistPattern: whitelist) {
      if (resourceUri.matches(whitelistPattern)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Authenticate a request for a resource.
   *
   * @param queryString
   *          The query string of the request that will contain the signature, policy and key id.
   * @param clientIp
   *          The ip of the client making the request in case it is a part of the policy.
   * @param resourceUri
   *          The uri of the resource being requested, the stream location.
   * @param properties
   *          The collection of keys and properties for this plugin.
   * @return A {@link ResourceRequest} with the result whether a request should be allowed or rejected.
   */
  protected static ResourceRequest authenticate(String queryString, String clientIp, String resourceUri,
          Properties properties) {

    try {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("Query String: " + queryString);
        getLogger().debug("Client Ip: " + clientIp);
        getLogger().debug("Resource: " + resourceUri);
      }

      if (isWhitelisted(resourceUri)) {
        if (getLogger().isDebugEnabled()) {
          getLogger().debug("Resource request does not need to be signed because the uri is whitelisted.");
        }
        ResourceRequest request = new ResourceRequest();
        request.setStatus(Status.Ok);
        return request;
      }

      ResourceRequest request = ResourceRequestUtil.resourceRequestFromQueryString(queryString, clientIp, resourceUri,
              properties, false);
      if (getLogger().isDebugEnabled()) {
        getLogger().debug("Encoded Policy: " + request.getEncodedPolicy());
        getLogger().debug("Encrypt Id: " + request.getEncryptionKeyId());
        getLogger().debug("Signature: " + request.getSignature());
        getLogger().debug("Status: " + request.getStatus());
        if (request != null && request.getPolicy() != null) {
          getLogger().debug("BaseURL: " + request.getPolicy().getBaseUrl());
          getLogger().debug("Valid Until: " + request.getPolicy().getValidUntil());
        }
      }
      return request;
    } catch (Throwable t) {
      getLogger().error("Unable to process request because: ", t);
      ResourceRequest request = new ResourceRequest();
      request.setStatus(Status.Forbidden);
      request.setRejectionReason("Unable to process request due to server error. Unable to verify signed url.");
      return request;
    }
  }

}
