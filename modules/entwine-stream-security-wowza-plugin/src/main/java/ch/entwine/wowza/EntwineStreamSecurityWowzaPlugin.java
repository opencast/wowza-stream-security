package ch.entwine.wowza;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.cupertinostreaming.httpstreamer.HTTPStreamerSessionCupertino;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.httpstreamer.smoothstreaming.httpstreamer.HTTPStreamerSessionSmoothStreamer;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.IMediaStream;

/**
 * The Wowza plugin to determine if a resource request for a signed url is valid.
 */
public class EntwineStreamSecurityWowzaPlugin extends ModuleBase {
  /** The possible status for a request that is a signed URL. */
  public enum Status {
    BadRequest, Forbidden, Gone, Ok
  };

  /** The key to use to encrypt the signatures. */
  private static String key = "0123456789abcdef";

  private static void handleClient(IClient client) {
    Status accepted = authenticate(client.getQueryStr(), client.getIp(), client.getPageUrl(), client.getUri());
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

  protected static Status authenticate(String queryString, String clientIp, String pageUrl, String resourceUri) {
    getLogger().info("Query String: " + queryString);
    getLogger().info("Client Ip: " + clientIp);
    getLogger().info("Page URL - The page that is opening the stream: " + pageUrl);
    getLogger().info("URI - The URI for the stream connection: " + resourceUri);
    return new ResourceRequest(queryString, clientIp, resourceUri, key).getStatus();
  }

}