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
package io.setl.websocket.messages.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.setl.bc.pychain.msgpack.MPWrappedMap;

@JsonPropertyOrder({"latitude", "country", "regionName", "longitude", "ip"})
public class Location {

  private String country;

  private String ip;

  private double latitude;

  private double longitude;

  private String regionName;


  /**
   * New instance.
   *
   * @param latitude   latitude of location
   * @param longitude  longitude of location
   * @param country    the country containing this location
   * @param regionName the region within the country which contains this location
   * @param ip         the IP address for this location
   */
  public Location(double latitude, double longitude, String country, String regionName, String ip) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.country = country;
    this.regionName = regionName;
    this.ip = ip;
  }


  public Location(MPWrappedMap<String, Object> locationInfo) {
    setLocation(locationInfo);
  }


  @JsonProperty
  public String getCountry() {
    return country;
  }


  @JsonProperty
  public String getIp() {
    return ip;
  }


  @JsonProperty
  public double getLatitude() {
    return latitude;
  }


  @JsonProperty
  public double getLongitude() {
    return longitude;
  }


  @JsonProperty("regionname")
  public String getRegionName() {
    return regionName;
  }


  public void setCountry(String country) {
    this.country = country;
  }


  public void setIp(String ip) {
    this.ip = ip;
  }


  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }


  private void setLocation(MPWrappedMap<String, Object> locationInfo) {
    locationInfo.iterate((k, v) -> {
      switch (k) {
        case "latitude":
          setLatitude(((Number) v).doubleValue());
          break;
        case "longitude":
          setLongitude(((Number) v).doubleValue());
          break;
        case "country":
          setCountry((String) v);
          break;
        case "regionname":
          setRegionName((String) v);
          break;
        case "ip":
          setIp((String) v);
          break;
        default:
          break;
      }
    });
  }


  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }


  public void setRegionName(String regionName) {
    this.regionName = regionName;
  }
}
