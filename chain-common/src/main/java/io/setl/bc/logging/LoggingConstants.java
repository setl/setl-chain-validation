/* <notice>
 
    SETL Blockchain
    Copyright (C) 2021 SETL Ltd
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License, version 3, as
    published by the Free Software Foundation.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.
 
    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 
</notice> */
package io.setl.bc.logging;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class LoggingConstants {

  public static final Marker MARKER_BROADCAST = MarkerFactory.getMarker("BROADCAST");

  public static final Marker MARKER_CONNECT = MarkerFactory.getMarker("CONNECT");

  public static final Marker MARKER_CONSENSUS = MarkerFactory.getMarker("CONSENSUS");

  public static final Marker MARKER_FATAL_ERROR = MarkerFactory.getMarker("FATAL_ERROR");

  public static final Marker MARKER_MESSAGING = MarkerFactory.getMarker("MESSAGING");

  public static final Marker MARKER_PERFORMANCE = MarkerFactory.getMarker("PERFORMANCE");

  public static final Marker MARKER_PROCESSED = MarkerFactory.getMarker("PROCESSED");

  public static final Marker MARKER_STATE = MarkerFactory.getMarker("STATE");

  public static final Marker MARKER_STORAGE = MarkerFactory.getMarker("STORAGE");

}
