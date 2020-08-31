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

package co.bywarp.stash.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import redis.clients.jedis.Protocol;

@Getter
@AllArgsConstructor(staticName = "configure")
public class RedisConnection {

    public static final int DEFAULT_PORT = 6379;
    public static final int CONNECT_TIMEOUT = Protocol.DEFAULT_TIMEOUT;
    public static final long RESOURCE_TIMEOUT = 20000;

    private String host;
    private int port;
    private boolean auth;
    private String password;
    private String delimiter;

    // Pool Settings
    private int poolSize;
    private int connectTimeout;
    private long resourceTimeout;
    private long timeoutMillis;
    private boolean testOnBorrow;
    private boolean blockWhenExhausted;

    public RedisConnection(String host, int port, boolean auth, String password) {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.password = password;
        this.poolSize = 256;
        this.connectTimeout = CONNECT_TIMEOUT;
        this.resourceTimeout = RESOURCE_TIMEOUT;
        this.timeoutMillis = 1500;
        this.testOnBorrow = true;
        this.blockWhenExhausted = true;
    }

}
