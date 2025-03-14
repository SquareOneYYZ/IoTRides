package org.traccar.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.traccar.BaseTest;
import org.traccar.model.User;
import org.traccar.model.Typed;
import org.traccar.storage.StorageException;
// import org.traccar.api.BaseResource;
import org.traccar.api.resource.NotificationResource;
import org.traccar.api.security.PermissionsService;
import org.traccar.storage.Storage;

// import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.traccar.api.BaseResource;
import java.lang.reflect.Field;

public class NotificationResourceTest extends BaseTest {

    private NotificationResource resource;
    @Mock
    protected Storage storage;

    @Mock
    protected PermissionsService permissionsService;


    @BeforeEach
    public void setupTest() throws Exception {
        openMocks(this);
        resource = new NotificationResource();
        Field storageField = BaseResource.class.getDeclaredField("storage");
        Field permissionsField = BaseResource.class.getDeclaredField("permissionsService");

        storageField.setAccessible(true);
        permissionsField.setAccessible(true);
        
        storageField.set(resource, storage);
        permissionsField.set(resource, permissionsService);
        
    }

    @Test 
    public void testGetTypesWithFiltering() throws StorageException {

        User user = new User();
        user.setId(1);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("RemoveNotificationTypes", "alarm,deviceOffline");
        user.setAttributes(attributes);

        when(storage.getObject(any(), any())).thenReturn(user);

        // Get notification types
        Collection<Typed> types = resource.getTypes(true, user.getId(), 0, 0);

        // Verify results
        assertNotNull(types);
        assertFalse(types.isEmpty());

        // Check filtered types
        boolean hasAlarm = false;
        boolean hasDeviceOffline = false;
        boolean hasDeviceOnline = false;

        for (Typed type : types) {
            switch (type.type()) {
                case "alarm" -> hasAlarm = true;
                case "deviceOffline" -> hasDeviceOffline = true;
                case "deviceOnline" -> hasDeviceOnline = true;
            }
        }

        // Verify filtered types are removed and others remain
        assertFalse(hasAlarm, "Alarm type should be filtered out");
        assertFalse(hasDeviceOffline, "DeviceOffline type should be filtered out");
        assertTrue(hasDeviceOnline, "DeviceOnline type should be present");
    }

    @Test
    public void testGetTypesWithoutFiltering() throws StorageException {
        // Setup test user without filters
        User user = new User();
        user.setId(1);
        user.setAttributes(new HashMap<>());

        when(storage.getObject(any(), any())).thenReturn(user);

        // Get notification types
        Collection<Typed> types = resource.getTypes(true, user.getId(), 0, 0);

        // Verify results
        assertNotNull(types);
        assertFalse(types.isEmpty());

        // Check types are present
        boolean hasAlarm = false;
        boolean hasDeviceOffline = false;

        for (Typed type : types) {
            switch (type.type()) {
                case "alarm" -> hasAlarm = true;
                case "deviceOffline" -> hasDeviceOffline = true;
            }
        }

        // Verify no types are filtered
        assertTrue(hasAlarm, "Alarm type should be present");
        assertTrue(hasDeviceOffline, "DeviceOffline type should be present");
    }
}