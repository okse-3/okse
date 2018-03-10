/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Norwegian Defence Research Establishment / NTNU
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package no.ntnu.okse.protocol.wsn;

import com.google.common.io.ByteStreams;
import no.ntnu.okse.core.messaging.Message;
import no.ntnu.okse.core.subscription.Subscriber;
import no.ntnu.okse.core.subscription.SubscriptionService;
import no.ntnu.okse.protocol.AbstractProtocolServer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.ntnunotif.wsnu.base.internal.ServiceConnection;
import org.ntnunotif.wsnu.base.net.NuNamespaceContextResolver;
import org.ntnunotif.wsnu.base.soap.Soap;
import org.ntnunotif.wsnu.base.util.InternalMessage;
import org.ntnunotif.wsnu.base.util.RequestInformation;
import org.ntnunotif.wsnu.services.general.WsnUtilities;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WSNotificationServer extends AbstractProtocolServer {

  // Path to internal configuration file on classpath
  private static final String wsnInternalConfigFile = "/config/wsnserver.xml";

  private static final String SERVERTYPE = "WSNotification";

  // Internal Default Values
  private static final String DEFAULT_HOST = "0.0.0.0";
  private static final int DEFAULT_PORT = 61000;
  private static final String DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME = "Content";

  // Flag and defaults for operation behind NAT
  private final boolean behindNAT;
  private final String publicWANHost;
  private final Integer publicWANPort;

  // HTTP Client fields
  private final Long connectionTimeout;
  private final Integer clientPoolSize;

  // Non-XMl Content Wrapper Name
  private String contentWrapperElementName;

  // Instance fields
  private Server _server;
  private WSNRequestParser _requestParser;
  private WSNCommandProxy _commandProxy;
  private final ArrayList<Connector> _connectors = new ArrayList<>();
  private HttpClient _client;
  private HashSet<ServiceConnection> _services;
  private ExecutorService clientPool;
  private final TreeSet<String> relays = new TreeSet<>();

  /**
   * Constructor that takes in configuration options for the WSNotification server. <p>
   *
   * @param host A String representing the host the WSNServer should bind to
   * @param port An int representing the port the WSNServer should bind to.
   * @param timeout A Long
   * @param pool_size An int
   * @param wrapper_name A String
   * @param nat A boolean
   * @param wan_host A string
   * @param wan_port An int
   */
  public WSNotificationServer(String host, int port, Long timeout, int pool_size,
      String wrapper_name, boolean nat, String wan_host, int wan_port) {
    super(host, port, SERVERTYPE);
    connectionTimeout = timeout;
    clientPoolSize = pool_size;
    contentWrapperElementName = wrapper_name;
    behindNAT = nat;
    publicWANHost = wan_host;
    publicWANPort = wan_port;
    init();
  }

  /**
   * Initialization method that reads the wsnserver.xml configuration file and constructs a jetty
   * server instance.
   */
  private void init() {
    log.warn("Logging!");

    clientPool = Executors.newFixedThreadPool(clientPoolSize);

    if (contentWrapperElementName.contains("<") || contentWrapperElementName.contains(">")) {
      log.warn(
          "Non-XML message payload element wrapper name cannot contain XML element characters (< or >),"
              +
              " using default: " + DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME);
      contentWrapperElementName = DEFAULT_MESSAGE_CONTENT_WRAPPER_NAME;
    }

    // Declare configResource (Fetched from classpath as a Resource from system)
    Resource configResource;
    try {
      // Try to parse the configFile for WSNServer to set up the Server instance
      configResource = Resource.newSystemResource(wsnInternalConfigFile);
      XmlConfiguration config = new XmlConfiguration(configResource.getInputStream());
      this._server = (Server) config.configure();
      // Remove the xml config connector
      this._server.removeConnector(this._server.getConnectors()[0]);

      // Add a the server connector
      log.debug("Adding WSNServer connector");
      this.addStandardConnector(this.host, this.port);

      // Initialize the RequestParser for WSNotification
      this._requestParser = new WSNRequestParser(this);

      // Initialize the collection of ServiceConnections
      this._services = new HashSet<>();

      // Initialize and set the HTTPHandler for the Server instance
      HttpHandler handler = new WSNotificationServer.HttpHandler();
      this._server.setHandler(handler);

      log.debug("XMLConfig complete, server instantiated.");

    } catch (Exception e) {
      log.error("Unable to start WSNotificationServer: " + e.getMessage());
    }
  }

  /**
   * The primary boot method for starting a WSNServer instance. Will only perform actions if the
   * server instance is not already running. <p> Initializes a HttpClient, and starts it. Also adds
   * predefined connectors to the jetty server instance. Constructs a new serverThread and starts
   * the jetty server instance in this new thread. </p>
   */
  public void boot() {

    log.info("Booting WSNServer.");

    if (!_running) {
      try {
        // Initialize a plain HttpClient
        this._client = new HttpClient();
        // Turn off following HTTP 30x redirects for the client
        this._client.setFollowRedirects(false);
        this._client.start();
        log.info("Started WSNServer HTTPClient");

        // For all registered connectors in WSNotificationServer, add these to the Jetty Server
        this._connectors.forEach(c -> this._server.addConnector(c));

        /* OKSE custom WS-Nu web services */

        // Initialize the CommandProxy
        WSNCommandProxy broker = new WSNCommandProxy(this);
        _commandProxy = broker;
        // Initialize the WSN SubscriptionManager and PublisherRegistrationManager
        WSNSubscriptionManager subscriptionManager = new WSNSubscriptionManager(this);
        WSNRegistrationManager registrationManager = new WSNRegistrationManager(this);
        // Add listener support from the OKSE SubscriptionService
        SubscriptionService.getInstance().addSubscriptionChangeListener(subscriptionManager);
        SubscriptionService.getInstance().addPublisherChangeListener(registrationManager);

        // QuickBuild the broker
        broker.quickBuild("broker", this._requestParser);
        // QuickBuild the WSN SubManager
        subscriptionManager.quickBuild("subscriptionManager", this._requestParser);
        subscriptionManager.initCoreSubscriptionService(SubscriptionService.getInstance());
        // QuickBuild the WSN PubRegManager
        registrationManager.quickBuild("registrationManager", this._requestParser);
        registrationManager.initCoreSubscriptionService(SubscriptionService.getInstance());
        // Register the WSN managers to the command proxy (proxied broker)
        broker.setSubscriptionManager(subscriptionManager);
        broker.setRegistrationManager(registrationManager);

        // Create a new thread for the Jetty Server to run in
        this._serverThread = new Thread(this::run);
        this._serverThread.setName("WSNServer");
        // Start the Jetty Server
        this._serverThread.start();
        Connector c = _connectors.get(0);
        while (!c.isStarted()) {
          if (c.isFailed()) {
            throw new BootErrorException("Unable to bind to " + host + ":" + port);
          }
        }
        _running = true;
        log.info("WSNServer Thread started successfully.");
      } catch (BootErrorException e) {
        throw e;
      } catch (Exception e) {
        totalErrors.incrementAndGet();
        log.trace(e.getStackTrace());
      }
    }
  }

  /**
   * This interface method should contain the main run loop initialization
   */
  @Override
  public void run() {
    try {
      WSNotificationServer.this._server.start();
      WSNotificationServer.this._server.join();

    } catch (Exception serverError) {
      totalErrors.incrementAndGet();
      log.trace(serverError.getStackTrace());
    }
  }

  /**
   * Fetch the HashSet containing all WebServices registered to the protocol server
   *
   * @return A HashSet of ServiceConnections for all the registered web services.
   */
  public HashSet<ServiceConnection> getServices() {
    return _services;
  }

  /**
   * This method stops the execution of the WSNotificationServer instance.
   */
  @Override
  public void stopServer() {
    try {
      log.info("Stopping WSNServer...");
      // Removing all subscribers
      _commandProxy.getAllRecipients()
          .forEach(s -> _commandProxy.getProxySubscriptionManager().removeSubscriber(s));
      // Removing all publishers
      _commandProxy.getProxyRegistrationManager().getAllPublishers()
          .forEach(p -> _commandProxy.getProxyRegistrationManager().removePublisher(p));

      // Stop the HTTP Client
      this._client.stop();
      // Stop the ServerConnector
      this._server.stop();
      this._serverThread = null;
      // Reset flags
      log.info("WSNServer Client and ServerThread stopped");
    } catch (Exception e) {
      totalErrors.incrementAndGet();
      log.trace(e.getStackTrace());
    }
  }

  /**
   * Retrieve the default element name for non-XML messages that are to be wrapped in a soap
   * enveloped WSNotification Notify element. This element will be the first and only child of the
   * Message element.
   *
   * @return The default name of the content wrapper element
   */
  public String getMessageContentWrapperElementName() {
    return contentWrapperElementName;
  }

  /**
   * This interface method must take in an instance of Message, which contains the appropriate
   * references and flags needed to distribute the message to consumers. Implementation specific
   * details can vary from protocol to protocol, but the end result of a method call to sendMessage
   * is that the message is delivered, or an error is logged.
   *
   * @param message An instance of Message containing the required data to distribute a message.
   */
  @Override
  public void sendMessage(Message message) {
    log.debug("WSNServer received message for distribution");
    if (!message.getOriginProtocol().equals(protocolServerType)
        || message.getAttribute("duplicate") != null) {
      log.debug("The message originated from other protocol than WSNotification");

      WSNTools.NotifyWithContext notifywrapper = WSNTools
          .buildNotifyWithContext(getMessageContentWrapperElementName(), message.getMessage(),
              message.getTopic(), null, null);
      // If it contained XML, we need to create properly marshalled jaxb node structure
      if (message.getMessage().contains("<") || message.getMessage().contains(">")) {
        // Unmarshal from raw XML
        Notify notify = WSNTools.createNotify(message);
        // If it was malformed, or maybe just a message containing < or >, build it as generic content element
        if (notify == null) {
          WSNTools.injectMessageContentIntoNotify(WSNTools
              .buildGenericContentElement(getMessageContentWrapperElementName(),
                  message.getMessage()), notifywrapper.notify);
          // Else inject the unmarshalled XML nodes into the Notify message attribute
        } else {
          WSNTools.injectMessageContentIntoNotify(WSNTools.extractMessageContentFromNotify(notify),
              notifywrapper.notify);
        }
      }

            /*
                Start to resolve recipients. The reason we cannot re-use the WSNCommandProxy's
                sendNotification method is that it will inject the message to the MessageService for relay
                thus creating duplicate messages.
             */

      NuNamespaceContextResolver namespaceContextResolver = notifywrapper.nuNamespaceContextResolver;

      // bind namespaces to topics
      for (NotificationMessageHolderType holderType : notifywrapper.notify
          .getNotificationMessage()) {

        // Extract the topic
        TopicExpressionType topic = holderType.getTopic();

        if (holderType.getTopic() != null) {
          NuNamespaceContextResolver.NuResolvedNamespaceContext context = namespaceContextResolver
              .resolveNamespaceContext(topic);

          if (context == null) {
            continue;
          }

          context.getAllPrefixes().forEach(prefix -> {
            // check if this is the default xmlns attribute
            if (!prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
              // add namespace context to the expression node
              topic.getOtherAttributes()
                  .put(new QName("xmlns:" + prefix), context.getNamespaceURI(prefix));
            }
          });
        }
      }

      // For all valid recipients
      for (String recipient : _commandProxy.getAllRecipients()) {

        // If the subscription has expired, continue
        if (_commandProxy.getProxySubscriptionManager().getSubscriber(recipient).hasExpired()) {
          continue;
        }

        // Filter do filter handling, if any
        Notify toSend = _commandProxy
            .getRecipientFilteredNotify(recipient, notifywrapper.notify, namespaceContextResolver);

        // If any message was left to send, send it
        if (toSend != null) {
          InternalMessage outMessage = new InternalMessage(
              InternalMessage.STATUS_OK |
                  InternalMessage.STATUS_HAS_MESSAGE |
                  InternalMessage.STATUS_ENDPOINTREF_IS_SET,
              toSend
          );
          // Update the request-information
          outMessage.getRequestInformation()
              .setEndpointReference(_commandProxy.getEndpointReferenceOfRecipient(recipient));

          // Check if the subscriber has requested raw message format
          // If the recipient has requested UseRaw, remove Notify payload wrapping
          if (_commandProxy
              .getProxySubscriptionManager()
              .getSubscriber(recipient)
              .getAttribute(WSNSubscriptionManager.WSN_USERAW_TOKEN) != null) {

            Object content = WSNTools.extractMessageContentFromNotify(toSend);
            // Update the InternalMessage with the content of the NotificationMessage
            outMessage.setMessage(content);
          }

          // Use the correct SOAP version for this subscriber
          Subscriber subscriber = _commandProxy.getProxySubscriptionManager()
              .getSubscriber(recipient);
          String version = subscriber.getAttribute("soap_version");
          if (version == null) {
            outMessage.setVersion(Soap.SoapVersion.SOAP_1_1);
          } else {
            switch (version) {
              default:
              case "soap11":
                outMessage.setVersion(Soap.SoapVersion.SOAP_1_1);
                break;
              case "soap12D":
                outMessage.setVersion(Soap.SoapVersion.SOAP_1_2_2001);
                break;
              case "soap12F":
                outMessage.setVersion(Soap.SoapVersion.SOAP_1_2_2003);
                break;
            }
          }

          // Pass it along to the request parser wrapped as a thread pool executed job
          clientPool.execute(() -> _requestParser.acceptLocalMessage(outMessage));
        }
      }
    } else {
      log.debug("Message originated from WSN protocol, already processed");
    }
  }

  /**
   * Fetches the complete URI of this ProtocolServer
   *
   * @return A string representing the complete URI of this ProtocolServer
   */
  public String getURI() {
    // Check if we are behind NAT
    if (behindNAT) {
      return "http://" + publicWANHost + ":" + publicWANPort;
    }
    // If somehow URI could not be retrieved
    if (_server.getURI() == null) {
      log.warn("Failed to fetch URI of server");
      return "http://" + DEFAULT_HOST + ":" + DEFAULT_PORT;
    }
    // Return the server connectors registered host and port
    //return "http://" + _server.getURI().getHost() + ":" + (_server.getURI().getPort() > -1 ? _server.getURI().getPort() : DEFAULT_PORT);
    // TODO: getURI.getPort() returns -1 (because the server hasn't started yet?), we should find a better fix for this
    ServerConnector c = (ServerConnector) _server.getConnectors()[0];
    return "http://" + _server.getURI().getHost() + ":" + (_server.getURI().getPort() > -1 ? _server
        .getURI().getPort() : c.getPort());
  }

  /**
   * Returns the public WAN Host if behindNAT is true. If behindNAT is false, the value of host is
   * returned.
   *
   * @return The public WAN Host
   */
  public String getPublicWANHost() {
    if (behindNAT) {
      return publicWANHost;
    }
    return host;
  }

  /**
   * Returns the public WAN Port if behindNAT is true. If behindNAT is false, the value of port is
   * returned.
   *
   * @return The public WAN Port
   */
  public Integer getPublicWANPort() {
    if (behindNAT) {
      return publicWANPort;
    }
    return port;
  }

  /**
   * Registers the specified ServiceConnection to the ProtocolServer
   *
   * @param webServiceConnector: The ServiceConnection you wish to register.
   */
  public synchronized void registerService(ServiceConnection webServiceConnector) {
    _services.add(webServiceConnector);
  }

  /**
   * Unregisters the specified ServiceConnection from the ProtocolServer
   *
   * @param webServiceConnector: The ServiceConnection you wish to remove.
   */
  public synchronized void removeService(ServiceConnection webServiceConnector) {
    _services.remove(webServiceConnector);
  }

  /**
   * Add a standard serverconnector to the server instance.
   *
   * @param address The IP address you wish to bind the server-connector to
   * @param port The port you with to bind the server-connector to
   */
  public void addStandardConnector(String address, int port) {
    ServerConnector connector = new ServerConnector(_server);
    connector.setHost(address);
    if (port == 80) {
      log.warn(
          "You have requested to use port 80. This will not work unless you are running as root." +
              "Are you running as root? You shouldn't. Reroute port 80 to 8080 instead.");
    }
    connector.setPort(port);
    _connectors.add(connector);
    _server.addConnector(connector);
  }

  /**
   * Add a predefined server-connector to the server instance.
   *
   * @param connector A jetty ServerConnector
   */
  public void addConnector(Connector connector) {
    _connectors.add(connector);
    this._server.addConnector(connector);
  }

  /**
   * Fetch the WSNRequestParser object
   *
   * @return WSNRequestParser
   */
  public WSNRequestParser getRequestParser() {
    return this._requestParser;
  }

  // This is the HTTP Handler that the WSNServer uses to process all incoming requests
  private class HttpHandler extends AbstractHandler {

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

      log.debug("HttpHandle invoked on target: " + target);

      // Do some stats.
      totalRequests.incrementAndGet();

      boolean isChunked = false;

      Enumeration headerNames = request.getHeaderNames();

      log.debug("Checking headers...");

      // Check the request headers, check for chunked encoding
      while (headerNames.hasMoreElements()) {
        String outMessage = (String) headerNames.nextElement();
        Enumeration returnMessage = request.getHeaders(outMessage);

        while (returnMessage.hasMoreElements()) {
          String inputStream = (String) returnMessage.nextElement();
          if (outMessage.equals("Transfer-Encoding") && inputStream.equals("chunked")) {
            log.debug("Found Transfer-Encoding was chunked.");
            isChunked = true;
          }
        }
      }

      log.debug("Accepted message, trying to instantiate WSNu InternalMessage");

      // Get message content, if any
      InternalMessage outgoingMessage;
      if (request.getContentLength() > 0) {
        InputStream inputStream = request.getInputStream();
        outgoingMessage = new InternalMessage(
            InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE, inputStream);
      } else if (isChunked) {
        InputStream chunkedInputStream = request.getInputStream();
        StringWriter swriter = new StringWriter();
        IOUtils.copy(chunkedInputStream, swriter);
        String rawRequest = swriter.toString();
        log.debug(rawRequest);
        outgoingMessage = new InternalMessage(
            InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE,
            new ByteArrayInputStream(rawRequest.getBytes()));
      } else {
        outgoingMessage = new InternalMessage(InternalMessage.STATUS_OK, null);
      }

      log.debug("WSNInternalMessage: " + outgoingMessage);

      // Update the request information object
      outgoingMessage.getRequestInformation().setEndpointReference(request.getRemoteHost());
      outgoingMessage.getRequestInformation().setRequestURL(request.getRequestURI());
      outgoingMessage.getRequestInformation().setParameters(request.getParameterMap());

      log.debug(
          "EndpointReference: " + outgoingMessage.getRequestInformation().getEndpointReference());
      log.debug("Request URI: " + outgoingMessage.getRequestInformation().getRequestURL());

      log.debug("Forwarding message to requestParser...");

      // Push the outgoingMessage to the request parser. Based on the status flags of the return message
      // we should know what has happened, and which response we should send.
      InternalMessage returnMessage = null;
      try {
        returnMessage = WSNotificationServer.this._requestParser
            .parseMessage(outgoingMessage, response.getOutputStream());
      } catch (Exception e) {
        log.error("Uncaught exception: " + e.getMessage());
        log.trace(e.getStackTrace());
      }

      // Improper response from WSNRequestParser! FC WHAT DO?
      if (returnMessage == null) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        totalErrors.incrementAndGet();
        baseRequest.setHandled(true);
        returnMessage = new InternalMessage(InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
      }

      /* Handle possible errors */
      if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT) > 0) {

        /* Have we got an error message to return? */
        if ((returnMessage.statusCode & InternalMessage.STATUS_HAS_MESSAGE) > 0) {
          response.setContentType("application/soap+xml;charset=utf-8");

          // Declare input and output streams
          InputStream inputStream = (InputStream) returnMessage.getMessage();
          OutputStream outputStream = response.getOutputStream();

          // Pipe the data from input to output stream
          ByteStreams.copy(inputStream, outputStream);

          // Set proper HTTP status, flush the output stream and set the handled flag
          response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
          outputStream.flush();
          baseRequest.setHandled(true);
          totalErrors.incrementAndGet();

          return;
        }

        /* If no valid destination was found for the request (Endpoint non-existent) */
        if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_DESTINATION) > 0) {
          response.setStatus(HttpStatus.NOT_FOUND_404);
          baseRequest.setHandled(true);
          totalBadRequests.incrementAndGet();

          return;

          /* If there was an internal server error */
        } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INTERNAL_ERROR) > 0) {
          response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
          baseRequest.setHandled(true);
          totalErrors.incrementAndGet();

          return;

          /* If there was syntactical errors or otherwise malformed request content */
        } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_INVALID_PAYLOAD) > 0) {
          response.setStatus(HttpStatus.BAD_REQUEST_400);
          baseRequest.setHandled(true);
          totalBadRequests.incrementAndGet();

          return;

          /* If the requested method or access to endpoint is forbidden */
        } else if ((returnMessage.statusCode & InternalMessage.STATUS_FAULT_ACCESS_NOT_ALLOWED)
            > 0) {
          response.setStatus(HttpStatus.FORBIDDEN_403);
          baseRequest.setHandled(true);
          totalBadRequests.incrementAndGet();

          return;
        }

                /*
                    Otherwise, there has been an exception of some sort with no message attached,
                    and we will reply with a server error
                */
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        baseRequest.setHandled(true);
        totalErrors.incrementAndGet();

        // Check if we have status=OK and also we have a message
      } else if (((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) &&
          (InternalMessage.STATUS_HAS_MESSAGE & returnMessage.statusCode) > 0) {

        /* Liar liar pants on fire */
        if (returnMessage.getMessage() == null) {

          log.error(
              "The HAS_RETURNING_MESSAGE flag was checked, but there was no returning message content");
          response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
          baseRequest.setHandled(true);
          totalErrors.incrementAndGet();

          return;
        }

        // Prepare the response content type
        response.setContentType("application/soap+xml;charset=utf-8");

        // Allocate the input and output streams
        InputStream inputStream = (InputStream) returnMessage.getMessage();
        OutputStream outputStream = response.getOutputStream();

        /* Copy the contents of the input stream into the output stream */
        ByteStreams.copy(inputStream, outputStream);

        /* Set proper OK status and flush out the stream for response to be sent */
        response.setStatus(HttpStatus.OK_200);
        outputStream.flush();

        baseRequest.setHandled(true);

        /* Everything is fine, and nothing is expected */
      } else if ((InternalMessage.STATUS_OK & returnMessage.statusCode) > 0) {

        response.setStatus(HttpStatus.OK_200);
        baseRequest.setHandled(true);

      } else {
        // We obviously should never land in this block, hence we set the 500 status.
        log.error(
            "HandleMessage: The message returned to the WSNotificationServer was not flagged with either STATUS_OK or"
                +
                "STATUS_FAULT. Please set either of these flags at all points");

        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        baseRequest.setHandled(true);
        totalErrors.incrementAndGet();

      }
    }
  }

  public InternalMessage sendMessage(InternalMessage message) {

    // Fetch the requestInformation from the message, and extract the endpoint
    RequestInformation requestInformation = message.getRequestInformation();
    String endpoint = requestInformation.getEndpointReference();

    /* If we have nowhere to send the message */
    if (endpoint == null) {
      log.error("Endpoint reference not set");
      totalErrors.incrementAndGet();
      return new InternalMessage(InternalMessage.STATUS_FAULT, null);
    }

    /* Create the actual http-request*/
    org.eclipse.jetty.client.api.Request request = _client
        .newRequest(requestInformation.getEndpointReference());
    request.timeout(connectionTimeout, TimeUnit.SECONDS);

    /* Try to send the message */
    try {
      /* Raw request */
      if ((message.statusCode & InternalMessage.STATUS_HAS_MESSAGE) == 0) {

        request.method(HttpMethod.GET);

        log.debug(
            "Sending message without content to " + requestInformation.getEndpointReference());
        ContentResponse response = request.send();
        totalRequests.incrementAndGet();

        return new InternalMessage(InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE,
            response.getContentAsString());
        /* Request with message */
      } else {

        // Set proper request method
        request.method(HttpMethod.POST);

        // If the status-flag has set a message and it is not an input stream
        if ((message.statusCode & InternalMessage.STATUS_MESSAGE_IS_INPUTSTREAM) == 0) {
          log.error(
              "sendMessage(): " + "The message contained something else than an inputStream." +
                  "Please convert your message to an InputStream before calling this methbod.");

          return new InternalMessage(
              InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

        } else {

          // Check if we should have had a message, but there was none
          if (message.getMessage() == null) {
            log.error("No content was found to send");
            totalErrors.incrementAndGet();
            return new InternalMessage(
                InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);
          }

          // Send the request to the specified endpoint reference
          log.info("Sending message with content to " + requestInformation.getEndpointReference());
          InputStream msg = (InputStream) message.getMessage();
          request
              .content(new InputStreamContentProvider(msg), "application/soap+xml; charset=utf-8");

          ContentResponse response = request.send();
          totalMessagesSent.incrementAndGet();

          // Check what HTTP status we received, if is not A-OK, flag the internal-message as fault
          // and make the response content the message of the InternalMessage returned
          if (!HttpStatus.isSuccess(response.getStatus())) {
            totalBadRequests.incrementAndGet();
            return new InternalMessage(
                InternalMessage.STATUS_FAULT | InternalMessage.STATUS_HAS_MESSAGE,
                response.getContentAsString());
          } else {
            return new InternalMessage(
                InternalMessage.STATUS_OK | InternalMessage.STATUS_HAS_MESSAGE,
                response.getContentAsString());
          }
        }
      }
    } catch (ClassCastException e) {
      log.error("sendMessage(): The message contained something else than an inputStream." +
          "Please convert your message to an InputStream before calling this method.");
      totalErrors.incrementAndGet();

      return new InternalMessage(
          InternalMessage.STATUS_FAULT | InternalMessage.STATUS_FAULT_INVALID_PAYLOAD, null);

    } catch (Exception e) {
      totalErrors.incrementAndGet();
      e.printStackTrace();
      log.error("sendMessage(): Unable to establish connection: " + e.getMessage());
      return new InternalMessage(InternalMessage.STATUS_FAULT_INTERNAL_ERROR, null);
    }
  }

  public boolean addRelay(String relay, String host, Integer port, String topic,
      Soap.SoapVersion version) {
    final Set<String> localRelays = new HashSet<String>() {{
      add("127.0.0.1");
      add("0.0.0.0");
      add("localhost");
    }};

    if (relays.contains(relay)) {
      return false;
    }

    // if relay.host == 0.0.0 etc check port
    if (localRelays.contains(host)) {
      log.debug("Same host, need to check port");
      if (getPort() == port) {
        log.debug("Same port, invalid relay command");
        return false;
      }
      // else check relay.host mot publicWANHost, then check port
    } else if (host.equals(getPublicWANHost())) {
      log.info("Same host (WAN), need to check port");
      if (getPublicWANPort().equals(port)) {
        log.info("Same port (WAN), invalid relay command");
        return false;
      }
    }

    if (!relay.startsWith("http://") && !relay.startsWith("https://")) {
      relay = "http://" + relay;
    }

    String subscriptionReference = WSNTools.extractSubscriptionReferenceFromRawXmlResponse(
        sendMessage(
            WSNTools.generateSubscriptionRequestWithTopic(
                relay,
                topic, // Topic
                getURI(),
                null, // Termination time
                version
            )
        )
    );

    if (subscriptionReference == null) {
      log.debug("Relay could not be created");
      return false;

    }
    relays.add(subscriptionReference);
    log.debug("Relay added");
    return true;
  }

  public boolean deleteRelay(String relay) {
    if (relays.contains(relay)) {
      WsnUtilities.sendUnsubscribeRequest(relay, getRequestParser());
      relays.remove(relay);
      log.debug("Removed relay: " + relay);
      return true;
    } else {
      log.debug("Unable to remove relay: " + relay);
      return false;
    }
  }

  public void deleteAllRelays() {
    relays.forEach((v) -> {
      WsnUtilities.sendUnsubscribeRequest(v, getRequestParser());
      log.debug("Removed relay: " + v);
    });

    relays.clear();
  }

  public Set<String> getRelays() {
    return relays;
  }
}

