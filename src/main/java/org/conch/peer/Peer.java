/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.peer;

import org.apache.commons.lang3.StringUtils;
import org.conch.http.APIEnum;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.Set;

public interface Peer extends Comparable<Peer> {
    //peer type
    enum Type {
        BOX(5,"Sharder Box"),
        HUB(4,"Sharder Hub"),
        NORMAL(3, "Normal Node"),
        COMMUNITY(2, "Community Node"),
        FOUNDATION(1, "Foundation Node");
        private final int code;
        private final String name;

        Type(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }
        
        public String getName() {
            return name;
        }

        public static Type getByCode(int code) {
            for (Type _enum : values()) {
                if (_enum.code == code) {
                    return _enum;
                }
            }
            return null;
        }
        
        public static Type getByCode(String code) {
            if(StringUtils.isEmpty(code)) return null;
            
            for (Type _enum : values()) {
                if (_enum.code == Integer.valueOf(code).intValue()) {
                    return _enum;
                }
            }
            
            return null;
        }
    }

    enum State {
        /**
         *
         */
        NON_CONNECTED,
        CONNECTED,
        DISCONNECTED
    }
    
    enum Service {
        HALLMARK(1),                    // Hallmarked node
        PRUNABLE(2),                    // Stores expired prunable messages
        API(4),                         // Open API access over http
        API_SSL(8),                     // Open API access over https
        CORS(16),                       // API CORS enabled
        BAPI(32),                       // Business API access over http => watcher role
        STORAGE(64),                    // Off-chain data storage => Storer role
        MINER(128),                     // Proxy mining => Miner role
        NATER(256),                     // Nat service => Traversal role (TBD)
        PROVER(512);                    // Prove service => Prover role (TBD)
        private final long code;        // Service code - must be a power of 2

        Service(int code) {
            this.code = code;
        }

        public long getCode() {
            return code;
        }
    }

    enum BlockchainState {
        UP_TO_DATE, //最新的
        DOWNLOADING, //下载中
        LIGHT_CLIENT, //轻客户端
        FORK //分叉
    }

    boolean providesService(Service service);

    boolean providesServices(long services);

    boolean isUseNATService();

    String getHost();

    int getPort();

    String getAnnouncedAddress();

    State getState();

    String getVersion();

    String getApplication();

    String getPlatform();

    String getSoftware();

    int getApiPort();

    int getApiSSLPort();

    Set<APIEnum> getDisabledAPIs();

    int getApiServerIdleTimeout();

    BlockchainState getBlockchainState();

    Hallmark getHallmark();

    int getWeight();
    
    String getBindRsAccount();
    
    void setBindRsAccount(String bindRsAccount);

    boolean shareAddress();

    boolean isBlacklisted();

    void blacklist(Exception cause);

    void blacklist(String cause);

    void unBlacklist();

    void deactivate();

    void remove();

    long getDownloadedVolume();

    long getUploadedVolume();

    int getLastUpdated();

    int getLastConnectAttempt();

    Type getType();
    
    void setType(Type type);
    
    boolean isType(Type type);

    boolean isInbound();

    boolean isInboundWebSocket();

    boolean isOutboundWebSocket();

    boolean isOpenAPI();

    boolean isApiConnectable();

    StringBuilder getPeerApiUri();

    String getBlacklistingCause();

    PeerLoad getPeerLoad();

    JSONObject send(JSONStreamAware request);

    JSONObject send(JSONStreamAware request, int maxResponseSize);

}
