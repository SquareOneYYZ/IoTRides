/*
 * Copyright 2018 Anton Tananaev (anton@traccar.org)
 * Copyright 2018 Andrey Kunitsyn (andrey@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.api.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.traccar.api.ExtendedObjectResource;
import org.traccar.model.Device;
import org.traccar.model.Group;
import org.traccar.model.Maintenance;
import org.traccar.model.User;
import org.traccar.storage.StorageException;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;

import org.traccar.storage.query.Request;



@Path("maintenance")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaintenanceResource extends ExtendedObjectResource<Maintenance> {

    public MaintenanceResource() {
        super(Maintenance.class, "name");
    }

    @GET 
    @Override
    public Collection<Maintenance> get(
            @QueryParam("all") boolean all, @QueryParam("userId") long userId,
            @QueryParam("groupId") long groupId, @QueryParam("deviceId") long deviceId) throws StorageException {

        // Collection<Maintenance> maintenances = super.get(all, userId, groupId, deviceId);

        var conditions = new LinkedList<Condition>();
        if (all) {
            if (permissionsService.notAdmin(getUserId())) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            }
        } else {
            if (userId == 0) {
                conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
            } else {
                permissionsService.checkUser(getUserId(), userId);
                conditions.add(new Condition.Permission(User.class, userId, baseClass).excludeGroups());
            }
        }

        if (groupId > 0) {
            permissionsService.checkPermission(Group.class, getUserId(), groupId);
            conditions.add(new Condition.Permission(Group.class, groupId, baseClass).excludeGroups());
        }
        if (deviceId > 0) {
            permissionsService.checkPermission(Device.class, getUserId(), deviceId);
            conditions.add(new Condition.Permission(Device.class, deviceId, baseClass).excludeGroups());
        }

         User user = permissionsService.getUser(userId);

         Map<String, Object> userAttributes = user.getAttributes();

         Collection<Map<String, Object>> result = new LinkedList<>();
         result.add(userAttributes);


         List<Maintenance> maintenance = storage.getObjects(baseClass,
                 new Request(new Columns.All(), Condition.merge(conditions)));



        
        return maintenance;

    }



    @GET 
@Path("attributes")  
public Collection<Map<String, Object>> getAttributes(
        @QueryParam("all") boolean all, 
        @QueryParam("userId") long userId,
        @QueryParam("groupId") long groupId, 
        @QueryParam("deviceId") long deviceId) throws StorageException { 

    var conditions = new LinkedList<Condition>();
    if (all) {
        if (permissionsService.notAdmin(getUserId())) {
            conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
        }
    } else {
        if (userId == 0) {
            conditions.add(new Condition.Permission(User.class, getUserId(), baseClass));
        } else {
            permissionsService.checkUser(getUserId(), userId);
            conditions.add(new Condition.Permission(User.class, userId, baseClass).excludeGroups());
        }
    }

    if (groupId > 0) {
        permissionsService.checkPermission(Group.class, getUserId(), groupId);
        conditions.add(new Condition.Permission(Group.class, groupId, baseClass).excludeGroups());
    }
    if (deviceId > 0) {
        permissionsService.checkPermission(Device.class, getUserId(), deviceId);
        conditions.add(new Condition.Permission(Device.class, deviceId, baseClass).excludeGroups());
    }

    // Get both user and maintenance attributes
    User user = permissionsService.getUser(userId);
    Collection<Maintenance> maintenances = storage.getObjects(baseClass, new Request(
            new Columns.All(), 
            Condition.merge(conditions)));

    Collection<Map<String, Object>> result = new LinkedList<>();
    
    // Add user attributes
    Map<String, Object> userAttrs = user.getAttributes();
    result.add(userAttrs);


    for (Maintenance maintenance : maintenances) {
        result.add(maintenance.getAttributes());
    }

    return result;
}


        
    
        
    

}
