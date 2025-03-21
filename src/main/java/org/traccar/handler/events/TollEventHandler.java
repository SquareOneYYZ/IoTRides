package org.traccar.handler.events;

import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traccar.config.Config;
import org.traccar.config.Keys;
import org.traccar.helper.model.PositionUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;
import org.traccar.session.state.TollRouteProcessor;
import org.traccar.session.state.TollRouteState;
import org.traccar.storage.Storage;
import org.traccar.storage.StorageException;
import org.traccar.storage.localCache.LocalCache;
import org.traccar.storage.query.Columns;
import org.traccar.storage.query.Condition;
import org.traccar.storage.query.Request;

public class TollEventHandler extends BaseEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TollEventHandler.class);


    private final CacheManager cacheManager;
    private final Storage storage;
    private final LocalCache localCache;

    private final int minimalDuration;

    @Inject
    public TollEventHandler(Config config, CacheManager cacheManager, Storage storage) {
        this.cacheManager = cacheManager;
        this.storage = storage;
        minimalDuration = config.getInteger(Keys.EVENT_TOLL_ROUTE_MINIMAL_DURATION);
        //TODO: fix later
        this.localCache = new LocalCache(key -> {
            TollRouteState state = new TollRouteState();
            Device device = cacheManager.getObject(Device.class, Long.parseLong(key));
            state.fromDevice(device);
            return state;
        });
    }

    @Override
    public void onPosition(Position position, Callback callback) {
        long deviceId = position.getDeviceId();

        String cacheKey = String.format("%s", deviceId);

        Device device = cacheManager.getObject(Device.class, deviceId);
        if (device == null) {
            return;
        }
        if (!PositionUtil.isLatest(cacheManager, position) || !position.getValid()) {
            return;
        }
        String positionTollRef = position.getString(Position.KEY_TOLL_REF);
        Boolean positionIsToll = position.getBoolean(Position.KEY_TOLL);
        String positionTollName = position.getString(Position.KEY_TOLL_NAME);

        TollRouteState tollState = (TollRouteState) localCache.get(cacheKey);

        LOGGER.info("CACHE INFO" + localCache.logCacheStats());

        if (tollState == null) {
            tollState = new TollRouteState();
        }

        tollState.addOnToll(positionIsToll, minimalDuration);
        tollState.fromDevice(device);

        TollRouteProcessor.updateState(tollState, position, positionTollRef, positionTollName,
                minimalDuration);

        Boolean tollConfidence = tollState.isOnToll(minimalDuration);
        if (tollConfidence == null || tollConfidence) {
            localCache.put(cacheKey, tollState);
        }


        if (tollState != null && tollState.isChanged()) {
            tollState.toDevice(device);
            try {
                storage.updateObject(device, new Request(
                        new Columns.Include("tollStartDistance", "tollrouteTime", "attributes"),
                        new Condition.Equals("id", device.getId())));
            } catch (StorageException e) {
                LOGGER.warn("Update device Toll error", e);
            }
        }
        if (tollState.getEvent() != null) {
            callback.eventDetected(tollState.getEvent());
        }



    }
}
