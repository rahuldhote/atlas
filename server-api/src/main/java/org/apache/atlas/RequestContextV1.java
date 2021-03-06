/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

import org.apache.atlas.metrics.Metrics;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RequestContextV1 {
    private static final Logger LOG = LoggerFactory.getLogger(RequestContextV1.class);

    private static final ThreadLocal<RequestContextV1> CURRENT_CONTEXT = new ThreadLocal<>();

    private final Map<String, AtlasObjectId>          updatedEntities = new HashMap<>();
    private final Map<String, AtlasObjectId>          deletedEntities = new HashMap<>();
    private final Map<String, AtlasEntityWithExtInfo> entityCacheV2   = new HashMap<>();
    private final Metrics                             metrics         = new Metrics();
    private final long                                requestTime     = System.currentTimeMillis();

    private String user;

    private RequestContextV1() {
    }

    //To handle gets from background threads where createContext() is not called
    //createContext called for every request in the filter
    public static RequestContextV1 get() {
        RequestContextV1 ret = CURRENT_CONTEXT.get();

        if (ret == null) {
            ret = new RequestContextV1();
            CURRENT_CONTEXT.set(ret);
        }

        return ret;
    }

    public static void clear() {
        RequestContextV1 instance = CURRENT_CONTEXT.get();

        if (instance != null) {
            instance.updatedEntities.clear();
            instance.deletedEntities.clear();
            instance.entityCacheV2.clear();
        }

        CURRENT_CONTEXT.remove();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void recordEntityUpdate(AtlasObjectId entity) {
        if (entity != null && entity.getGuid() != null) {
            updatedEntities.put(entity.getGuid(), entity);
        }
    }

    public void recordEntityDelete(AtlasObjectId entity) {
        if (entity != null && entity.getGuid() != null) {
            deletedEntities.put(entity.getGuid(), entity);
        }
    }

    /**
     * Adds the specified instance to the cache
     *
     */
    public void cache(AtlasEntityWithExtInfo entity) {
        if (entity != null && entity.getEntity() != null && entity.getEntity().getGuid() != null) {
            entityCacheV2.put(entity.getEntity().getGuid(), entity);
        }
    }

    public Collection<AtlasObjectId> getUpdatedEntities() {
        return updatedEntities.values();
    }

    public Collection<AtlasObjectId> getDeletedEntities() {
        return deletedEntities.values();
    }

    /**
     * Checks if an instance with the given guid is in the cache for this request.  Either returns the instance
     * or null if it is not in the cache.
     *
     * @param guid the guid to find
     * @return Either the instance or null if it is not in the cache.
     */
    public AtlasEntityWithExtInfo getInstanceV2(String guid) {
        return entityCacheV2.get(guid);
    }

    public long getRequestTime() {
        return requestTime;
    }

    public boolean isUpdatedEntity(String guid) {
        return updatedEntities.containsKey(guid);
    }

    public boolean isDeletedEntity(String guid) {
        return deletedEntities.containsKey(guid);
    }

    public static Metrics getMetrics() {
        return get().metrics;
    }
}
