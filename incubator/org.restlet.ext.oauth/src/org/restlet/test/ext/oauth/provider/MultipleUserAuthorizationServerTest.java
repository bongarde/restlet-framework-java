/**
 * Copyright 2005-2011 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.test.ext.oauth.provider;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import javax.xml.datatype.Duration;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Client;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.engine.Engine;
import org.restlet.engine.security.AuthenticatorHelper;
import org.restlet.ext.oauth.OAuthHelper;
import org.restlet.ext.oauth.OAuthParameters;
import org.restlet.ext.oauth.OAuthUser;
import org.restlet.ext.oauth.internal.OAuthUtils;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.test.ext.oauth.test.resources.Oauth2ClientTestApplication;
import org.restlet.test.ext.oauth.test.resources.Oauth2MultipleUserProtectedTestApplication;
import org.restlet.test.ext.oauth.test.resources.Oauth2MultipleUserTestApplication;
import org.restlet.test.ext.oauth.test.resources.SingletonStore;
import org.restlet.util.Series;

public class MultipleUserAuthorizationServerTest {
    public static Component component;

    // Use for http test when debugging
    public static int serverPort = 8080;

    public static int oauthServerPort = 8081;

    public static final String prot = "http";

    // public static int serverPort = 8443;
    // public static final String prot = "https";

    public static Oauth2ClientTestApplication client = new Oauth2ClientTestApplication();

    @BeforeClass
    public static void startServer() throws Exception {

        // org.restlet.ext.httpclient.internal.IgnoreCookieSpecFactory i;

        Logger log = Context.getCurrentLogger();
        log.info("Starting server test!");

        // SSL global configuration
        String keystore = ClassLoader.getSystemResource("localhost.jks")
                .getPath();
        System.setProperty("javax.net.ssl.trustStore", keystore);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStorePassword", "testpass");

        // protected resource server
        Server server = new Server(new Context(), Protocol.HTTP, serverPort);
        // server.getContext().getParameters().add("maxQueued", "10");
        // server.getContext().getParameters().add("maxThreads", "200");
        component = new Component();
        component.getServers().add(server);
        component.getClients().add(Protocol.HTTP);
        component.getClients().add(Protocol.HTTPS);
        component.getClients().add(Protocol.RIAP);
        component.getDefaultHost().attach("/server",
                new Oauth2MultipleUserProtectedTestApplication());

        // oauth server
        Server oauthServer = new Server(new Context(), Protocol.HTTP,
                oauthServerPort);
        // oauthServer.getContext().getParameters().add("maxQueued", "-1");
        // oauthServer.getContext().getParameters().add("maxThreads", "200");
        Component oauthcomp = new Component();
        oauthcomp.getServers().add(oauthServer);
        oauthcomp.getClients().add(Protocol.HTTP);
        oauthcomp.getClients().add(Protocol.HTTPS);
        oauthcomp.getClients().add(Protocol.RIAP);

        oauthcomp.getDefaultHost().attach("/oauth",
                new Oauth2MultipleUserTestApplication(0)); // unlimited
        // token life

        Series<Parameter> parameters = server.getContext().getParameters();
        parameters.add("keystorePath", keystore);
        parameters.add("keystorePassword", "testpass");
        parameters.add("keyPassword", "testpass");
        parameters.add("keystoreType", "JKS");
        parameters.add("sslServerAlias", "localhost");

        // server.getContext().getParameters().add("maxThreads", "30");
        // component.getDefaultHost();
        component.start();
        oauthcomp.start();

        List<AuthenticatorHelper> authenticators = Engine.getInstance()
                .getRegisteredAuthenticators();
        for (AuthenticatorHelper helper : authenticators) {
            System.out.println("Found default auth helper : " + helper);
        }
        authenticators.add(new OAuthHelper());

        System.out.println(Engine.getInstance().getRegisteredClients().get(0));
    }

    @AfterClass
    public static void stopServer() throws Exception {
        component.stop();
    }

    @Test
    public void multipleRequestTest() throws Exception {
        int numThreads = 100;
        int numCalls = 10;
        int totRequests = (numThreads * numCalls) + numThreads;
        Thread[] clients = new Thread[numThreads];
        Context c = new Context();
        for (int i = 0; i < numThreads; i++) {
            clients[i] = new ClientCall(numCalls, c);
            clients[i].start();
        }
        Awaitility.setDefaultTimeout(Duration.FOREVER);
        Awaitility.await().until(numCalls(), Matchers.equalTo(totRequests));
        System.out.println(SingletonStore.I().getCallbacks() + " "
                + SingletonStore.I().getErrors());
        Assert.assertEquals(0, SingletonStore.I().getErrors());
    }

    private Callable<Integer> numCalls() {
        return new Callable<Integer>() {
            public Integer call() throws Exception {
                return SingletonStore.I().getCallbacks(); // The condition
                                                          // supplier part
            }
        };
    }

    class ClientCall extends Thread {

        int numTimes;

        Random r;

        OAuthParameters params;

        Context c;

        Client myClient = new Client(Protocol.HTTP);

        public ClientCall(int numTimes, Context c) {
            this.numTimes = numTimes;
            this.c = c;
            r = new Random(System.nanoTime());
            params = new OAuthParameters(
                    "client1234",
                    "secret1234",
                    AuthorizationServerTest.prot
                            + "://localhost:"
                            + MultipleUserAuthorizationServerTest.oauthServerPort
                            + "/oauth/", "foo bar");
        }

        @Override
        public void run() {

            try {
                this.sleep(r.nextInt(500));
            } catch (InterruptedException e1) {

                e1.printStackTrace();
            }
            for (int i = 0; i < numTimes; i++) {
                System.out.println(this.getName() + " " + i);
                int u = r.nextInt(5) + 1;
                OAuthUser user = OAuthUtils.passwordFlow(params, "user" + u,
                        "pass" + u, myClient);
                if (user == null) {
                    SingletonStore.I().addError();
                    SingletonStore.I().addRequest();
                    continue;
                }

                Reference ref = new Reference(prot + "://localhost:"
                        + serverPort + "/server/scoped/user" + u);
                ref.addQueryParameter("oauth_token", user.getAccessToken());
                // ClientResource cr = new ClientResource(ref);
                ClientResource cr = new ClientResource(ref);
                cr.setNext(myClient);
                Representation r = cr.get();
                if (r == null) {
                    SingletonStore.I().addError();
                    SingletonStore.I().addRequest();
                    cr.release();
                    continue;
                }
                try {
                    String text = r.getText();
                    if (!text.equalsIgnoreCase("TestSuccessful")) {
                        SingletonStore.I().addError();
                    }
                } catch (Exception e) {
                    SingletonStore.I().addError();
                }
                SingletonStore.I().addRequest();
                r.release();
                cr.release();
            }
            SingletonStore.I().addRequest();
        }

    }

}
