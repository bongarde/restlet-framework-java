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

package org.restlet.test.ext.oauth.test.resources;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.ext.oauth.OAuthAuthorizer;
import org.restlet.routing.Router;
import org.restlet.test.ext.oauth.provider.AuthorizationServerTest;

public class Oauth2ProtectedTestApplication extends Application {
	@Override
	public synchronized Restlet createInboundRoot() {
		Context ctx = getContext();
		Router router = new Router(ctx);
		
		OAuthAuthorizer auth = new OAuthAuthorizer(
				AuthorizationServerTest.prot+"://localhost:"
				+AuthorizationServerTest.serverPort+
			"/oauth/validate",
			AuthorizationServerTest.prot+"://localhost:"+
			AuthorizationServerTest.serverPort+"/oauth/authorize"
			);
		auth.setNext(DummyResource.class);
		router.attach("/protected",auth);
		
		OAuthAuthorizer auth2 = new OAuthAuthorizer(
				AuthorizationServerTest.prot+"://localhost:"
				+AuthorizationServerTest.serverPort+
			"/oauth/validate",
			AuthorizationServerTest.prot+"://localhost:"+
			AuthorizationServerTest.serverPort+"/oauth/authorize"
			);
		auth2.setNext(ScopedDummyResource.class);
		router.attach("/scoped",auth2);
		
		return router;
	}

}
