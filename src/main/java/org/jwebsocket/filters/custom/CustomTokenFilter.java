//    ---------------------------------------------------------------------------
//    jWebSocket - Custom Token Filter for demonstration purposes
//    Copyright (c) 2010 Alexander Schulze, Innotrade GmbH
//    ---------------------------------------------------------------------------
//    This program is free software; you can redistribute it and/or modify it
//    under the terms of the GNU Lesser General Public License as published by the
//    Free Software Foundation; either version 3 of the License, or (at your
//    option) any later version.
//    This program is distributed in the hope that it will be useful, but WITHOUT
//    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
//    FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
//    more details.
//    You should have received a copy of the GNU Lesser General Public License along
//    with this program; if not, see <http://www.gnu.org/licenses/lgpl.html>.
//    ---------------------------------------------------------------------------
package org.jwebsocket.filters.custom;

import org.apache.log4j.Logger;
import org.jwebsocket.api.FilterConfiguration;
import org.jwebsocket.api.WebSocketConnector;
import org.jwebsocket.filter.TokenFilter;
import org.jwebsocket.kit.FilterResponse;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.token.Token;

/**
 * 
 * @author aschulze
 */
public class CustomTokenFilter extends TokenFilter {

  private static Logger mLog = Logging.getLogger();

  public CustomTokenFilter(FilterConfiguration configuration) {
    super(configuration);
    if (mLog.isDebugEnabled()) {
      mLog.debug("Instantiating custom token filter...");
    }
  }

  @Override
  public void processTokenIn(FilterResponse aResponse, WebSocketConnector aConnector, Token aToken) {
  }

  @Override
  public void processTokenOut(FilterResponse aResponse, WebSocketConnector aSource, WebSocketConnector aTarget, Token aToken) {
  }
}
