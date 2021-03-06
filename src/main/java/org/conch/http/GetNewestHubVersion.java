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

package org.conch.http;

import org.conch.tools.ClientUpgradeTool;
import org.conch.util.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * get hub latest version
 *
 * @author jiangbubai
 */
public final class GetNewestHubVersion extends APIServlet.APIRequestHandler {

    static final GetNewestHubVersion INSTANCE = new GetNewestHubVersion();
    private static final String FAILED_INFO = "failed to fetch latest hub version!";

    private GetNewestHubVersion() {
        super(new APITag[]{APITag.DEBUG});
    }

    @Override
    @SuppressWarnings("unchecked")
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        try {
            response.put("version", ClientUpgradeTool.fetchLastHubVersion());
            response.put("success", true);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", FAILED_INFO);
            Logger.logErrorMessage(FAILED_INFO, e);
        }
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return false;
    }

    @Override
    protected boolean requirePassword() {
        return false;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
