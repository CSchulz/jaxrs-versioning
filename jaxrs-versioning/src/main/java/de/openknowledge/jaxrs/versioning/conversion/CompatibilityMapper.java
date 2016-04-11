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

import java.sql.Date;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import de.openknowledge.jaxrs.versioning.Added;
import de.openknowledge.jaxrs.versioning.MovedFrom;
import de.openknowledge.jaxrs.versioning.Provider;
import de.openknowledge.jaxrs.versioning.Removed;

/**
 * @author Arne Limburg - open knowledge GmbH
 * @author Philipp Geers - open knowledge GmbH
 */
public class CompatibilityMapper {

  private VersionTypeFactory versionTypeFactory;

  CompatibilityMapper(VersionTypeFactory factory) {
    versionTypeFactory = factory;
  }

  public void map(Object object) {
    map(object, new VersionContext(object));
  }

  public void map(Object object, VersionContext context) {
    VersionType versionType = versionTypeFactory.get(object.getClass());

    for (VersionProperty versionProperty : versionType.getProperties()) {
      MovedFrom movedFrom = versionProperty.getAnnotation(MovedFrom.class);
      Added added = versionProperty.getAnnotation(Added.class);

      if (movedFrom == null && added == null) {
        if (!isSimpleValue(versionProperty.getType())) {
          Object value = versionProperty.get(object);
          if (value == null) {
            value = versionTypeFactory.get(versionProperty.getType()).newInstance();
            versionProperty.set(object, value);
          }
          map(value, context.getChildContext(value));
        }
        continue;
      }
      Object value = versionProperty.get(object);
      if (value == null) {
        updateDependentValues(new VersionPropertyValue(versionProperty, context));
      } else {
        setDependentValues(versionType, movedFrom, added, value, context);
        if (!isSimpleValue(value.getClass())) {
          map(value, context.getChildContext(value));
        }
      }
    }
  }

  private void updateDependentValues(VersionPropertyValue propertyValue) {
    if (propertyValue.get() != null) {
      return;
    }
    VersionContext dependentContext = propertyValue.getContext();
    VersionType dependentType = versionTypeFactory.get(dependentContext.getParent().getClass());

    MovedFrom movedFrom = propertyValue.getAnnotation(MovedFrom.class);
    if (movedFrom != null) {
      VersionPropertyValue dependentValue = getPropertyValue(dependentType, movedFrom.value(), dependentContext);
      Object value = dependentValue.get();
      if (value != null) {
        propertyValue.set(value);
        return;
      }
      updateDependentValues(dependentValue);
      value = dependentValue.get();
      if (value != null) {
        propertyValue.set(value);
        return;
      }
    }
    String defaultValue = StringUtils.EMPTY;
    Class<? extends Provider> provider = Provider.class;

    Added added = propertyValue.getAnnotation(Added.class);
    if (added != null) {
      for (String dependency : added.dependsOn()) {
        VersionPropertyValue dependentValue = getPropertyValue(dependentType, dependency, dependentContext);
        if (dependentValue.get() == null) {
          updateDependentValues(dependentValue);
        }
      }
      defaultValue = added.defaultValue();
      provider = added.provider();
    }
    setValue(propertyValue, provider, defaultValue, dependentContext);
  }

  private void setValue(VersionPropertyValue value, Class<? extends Provider> provider, String defaultValue, VersionContext context) {
    if (!provider.equals(Provider.class)) {
      Provider providerInstance = (Provider)versionTypeFactory.get(provider).newInstance();
      value.set(providerInstance.get(context));
    } else if (!defaultValue.isEmpty()) {
      value.set(defaultValue);
    } else if (!isSimpleValue(value.getProperty().getType())) {
      Object instance = versionTypeFactory.get(value.getProperty().getType()).newInstance();
      value.set(instance);
      map(instance, context.getChildContext(instance));
    }
  }

  private void setDependentValues(VersionType versionType, MovedFrom movedFrom, Added added, Object value, VersionContext context) {
    if (movedFrom != null) {
      setDependentValues(versionType, movedFrom.value(), value, context);
    }
    if (added != null) {
      for (String dependency: added.dependsOn()) {
        setRemovedValues(versionType, dependency, context);
      }
    }
  }

  private void setDependentValues(VersionType versionType, String path, Object value, VersionContext context) {
    VersionPropertyValue propertyValue = getPropertyValue(versionType, path, context);
    propertyValue.set(value);
    MovedFrom movedFrom = propertyValue.getAnnotation(MovedFrom.class);
    Added added = propertyValue.getAnnotation(Added.class);
    VersionType propertyParentType = versionTypeFactory.get(propertyValue.getContext().getParent().getClass()); 
    setDependentValues(propertyParentType, movedFrom, added, value, propertyValue.getContext());
  }

  private void setRemovedValues(VersionType versionType, String dependency, VersionContext context) {
    VersionPropertyValue propertyValue = getPropertyValue(versionType, dependency, context);
    if (propertyValue.get() == null) {
      Removed removed = propertyValue.getAnnotation(Removed.class);
      if (removed != null) {
        setValue(propertyValue, removed.provider(), removed.defaultValue(), context);
        Object value = propertyValue.get();
        if (value != null) {
          MovedFrom movedFrom = propertyValue.getAnnotation(MovedFrom.class);
          Added added = propertyValue.getAnnotation(Added.class);
          setDependentValues(versionType, movedFrom, added, value, context);
        }
      }
    }
  }

  private VersionPropertyValue getPropertyValue(VersionType versionType, String path, VersionContext context) {
    return getPropertyValue(versionType, path.split("/"), 0, context);
  }

  private VersionPropertyValue getPropertyValue(VersionType versionType, String[] pathElements, int index, VersionContext context) {
    if (pathElements[index].equals("..")) {
      context = context.getParentContext();
      return getPropertyValue(versionTypeFactory.get(context.getParent().getClass()), pathElements, index + 1, context);
    }
    VersionProperty property = versionType.getProperty(pathElements[index]);
    if (property == null) {
      throw new IllegalArgumentException("@MoveFrom contains unknown property " + pathElements[index]);
    }
    if (pathElements.length == index + 1) {
      return new VersionPropertyValue(property, context);
    }
    Object value = property.get(context.getParent());
    if (value == null) {
      value = versionTypeFactory.get(property.getType()).newInstance();
    }
    return getPropertyValue(versionTypeFactory.get(property.getType()), pathElements, index + 1,
        context.getChildContext(value));
  }

  private boolean isSimpleValue(Class<?> type) {
    return type == String.class || type == Date.class || ClassUtils.isPrimitiveOrWrapper(type);
  }
}
