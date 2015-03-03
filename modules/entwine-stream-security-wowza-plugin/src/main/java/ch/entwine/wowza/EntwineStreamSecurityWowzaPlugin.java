package ch.entwine.wowza;

import ch.entwine.utils.ResourceRequestUtil;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.HTTPStreamerSessionSmoothStreamer;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.IMediaStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Wowza plugin to determine if a resource request for a signed url is valid.
 */
public class EntwineStreamSecurityWowzaPlugin extends ModuleBase {
  /** The possible status for a request that is a signed URL. */
  public enum Status {
    BadRequest, Forbidden, Gone, Ok
  };

  /** The keys to use to encrypt the signatures. */
  private static Properties properties = new Properties();

  public EntwineStreamSecurityWowzaPlugin() {
    super();

    InputStream in = getClass().getResourceAsStream("/../conf");
    try {
      properties.load(in);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  private static void handleClient(IClient client) {
    Status accepted = authenticate(client.getQueryStr(), client.getIp(), client.getPageUrl(), client.getUri(),
            properties);
    switch (accepted) {
      case BadRequest:
        getLogger().warn("The request resulted in a bad request.");
        client.rejectConnection("The request resulted in a bad request.");
        break;
      case Forbidden:
        getLogger().warn("The request resulted in a forbidden.");
        client.rejectConnection("The request resulted in a forbidden.");
        break;
      case Gone:
        getLogger().warn("The resource is now gone.");
        client.rejectConnection("The resource is now gone.");
      case Ok:
        getLogger().debug("The resource is allowed to be viewed.");
    }
  }

  public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
    getLogger().info("onConnect: " + client.getClientId());
    handleClient(client);
  }

  public void onConnectAccept(IClient client) {
    getLogger().info("onConnectAccept: " + client.getClientId());
    handleClient(client);
  }

  public void onStreamCreate(IMediaStream stream) {
    getLogger().info("onStreamCreate: " + stream.getSrc());
    handleClient(stream.getClient());
  }

  public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
    getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onHTTPCupertinoStreamingSessionCreate(HTTPStreamerSessionCupertino httpSession) {
    getLogger().info("onHTTPCupertinoStreamingSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onHTTPSmoothStreamingSessionCreate(HTTPStreamerSessionSmoothStreamer httpSession) {
    getLogger().info("onHTTPSmoothStreamingSessionCreate: " + httpSession.getSessionId());
    handleClient(httpSession.getStream().getClient());
  }

  public void onRTPSessionCreate(RTPSession rtpSession) {
    getLogger().info("onRTPSessionCreate: " + rtpSession.getSessionId());
    handleClient(rtpSession.getRTSPStream().getStream().getClient());
  }

  public void onCall(String handlerName, IClient client, RequestFunction function, AMFDataList params) {
    getLogger().info("onCall: " + handlerName);
    handleClient(client);
  }

  protected static Status authenticate(String queryString, String clientIp, String pageUrl, String resourceUri,
          Properties properties) {
    getLogger().info("Query String: " + queryString);
    getLogger().info("Client Ip: " + clientIp);
    getLogger().info("Page URL - The page that is opening the stream: " + pageUrl);
    getLogger().info("URI - The URI for the stream connection: " + resourceUri);
    // return new ResourceRequest(queryString, clientIp, resourceUri, properties).getStatus();
    return ResourceRequestUtil.resourceRequestfromQueryString(queryString, clientIp, resourceUri, properties)
            .getStatus();
  }

}