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
package de.openknowledge.jaxrs.versioning.conversion;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import de.openknowledge.jaxrs.versioning.VersionedMediaType;

/**
 * @author Arne Limburg - open knowledge GmbH
 */
@Provider
public class VersionedMediaTypeFeature implements DynamicFeature {

  @Override
  public void configure(ResourceInfo resource, FeatureContext context) {
    VersionedMediaType versionedContentType = resource.getResourceMethod().getAnnotation(VersionedMediaType.class);
    if (versionedContentType == null) {
      versionedContentType = resource.getResourceClass().getAnnotation(VersionedMediaType.class);
    }
    
    if (versionedContentType != null) {
      VersionedMediaTypeFilter.register(MediaType.valueOf(versionedContentType.value()), MediaType.valueOf(versionedContentType.mapsTo()));
    }
  }
}
