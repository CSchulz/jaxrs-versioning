/*
 * Copyright (C) open knowledge GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.openknowledge.jaxrs.versioning;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import de.openknowledge.jaxrs.versioning.model.AddressV1;
import de.openknowledge.jaxrs.versioning.model.StreetV1;

/**
 * @author Arne Limburg - open knowledge GmbH
 * @author Philipp Geers - open knowledge GmbH
 */
@Path("/{version}/addresses")
public class AddressResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  public AddressV1 getAddress(@PathParam("id") int id) {
    return new AddressV1(
        new StreetV1(
            "Samplestreet",
            "1"
        ),
        "12345 Samplecity");
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}")
  public AddressV1 createAddress(String addressJson) {
    JSONObject address = new JSONObject(addressJson);
    if (!address.getJSONObject("street").getString("name").equals("Samplestreet")) {
      throw new IllegalArgumentException("wrong street");
    }
    if (!address.getJSONObject("street").getString("number").equals("1")) {
      throw new IllegalArgumentException("wrong number");
    }
    if (!address.getString("city").equals("12345 Samplecity")) {
      throw new IllegalArgumentException("wrong city");
    }
    return new AddressV1(
        new StreetV1(
            address.getJSONObject("street").getString("name"),
            address.getJSONObject("street").getString("number")),
        address.getString("city"));
  }
}