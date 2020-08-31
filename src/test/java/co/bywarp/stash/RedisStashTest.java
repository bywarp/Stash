/*
 * Copyright (c) 2020 Warp Studios
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package co.bywarp.stash;

import co.bywarp.lightkit.json.JsonSerializer;
import co.bywarp.lightkit.util.logger.Logger;
import co.bywarp.stash.element.ElementExpiryPolicy;
import co.bywarp.stash.redis.RedisConnection;
import co.bywarp.stash.redis.RedisKeyspace;
import co.bywarp.stash.redis.RedisTypeAdapter;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.junit.jupiter.api.Assertions.*;

public class RedisStashTest {

    @Getter
    @AllArgsConstructor
    private static class Client {
        public Client() {
        }

        private UUID uuid;
        private String name;
        private long createdAt;
    }

    private final Client EXAMPLE = new Client(UUID.randomUUID(), "Client", System.currentTimeMillis());
    private final ElementExpiryPolicy EXPIRY_POLICY = ElementExpiryPolicy.of(20, TimeUnit.SECONDS);
    private final RedisConnection CONNECTION = RedisConnection.configure(
            "127.0.0.1",
            RedisConnection.DEFAULT_PORT,
            false, null,
            ".", 64,
            RedisConnection.CONNECT_TIMEOUT,
            RedisConnection.RESOURCE_TIMEOUT,
            1500,
            true, true
    );

    @Test
    public void init() {
        Logger logger = new Logger("Tests");
        JsonSerializer serializer = new JsonSerializer();
        StashProvider<UUID, Client> stash = new StashFactory<UUID, Client>(StashSource.REDIS)
                .withExpiryPolicy(EXPIRY_POLICY)
                .withRedisConnection(CONNECTION)
                .withKeyspace(new RedisKeyspace<>(
                        "cache.clients", CONNECTION,
                        UUID::toString,
                        UUID::fromString
                ))
                .withResultSerializer(new RedisTypeAdapter<>(
                        json -> serializer
                                .fromJSON(new JSONObject(json),
                                        new Client())
                ))
                .withTypeSerializer(new RedisTypeAdapter<>(
                        client -> serializer.toJSON(client).toString()
                ))
                .build();

        stash.store(EXAMPLE.getUuid(), EXAMPLE);

        Client retrieved = stash.retrieve(EXAMPLE.getUuid());
        assertNotNull(retrieved);

        Map<UUID, Client> all = stash.retrieveAll();
        all.forEach((uuid, client) -> logger.info(uuid + " : " + client.getName() + " : " + client.getCreatedAt()));
        assertFalse(all.isEmpty());

        boolean evictResult = stash.evict(EXAMPLE.getUuid());
        assertTrue(evictResult);

        stash.evictAll();
    }

}
