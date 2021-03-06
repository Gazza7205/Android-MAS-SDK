/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas;

import android.content.Context;

import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASAuthenticationListener;
import com.ca.mas.foundation.MASConfiguration;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASOtpAuthenticationHandler;
import com.ca.mas.foundation.MASUser;
import com.ca.mas.foundation.auth.MASAuthenticationProviders;

import org.junit.After;
import org.junit.Before;


public abstract class MASStartTestBase extends MASMockGatewayTestBase {

    @Before
    public void masStart() throws Exception {
        MAS.start(getContext(), TestUtils.getJSONObject("/msso_config.json"));
        MAS.setAuthenticationListener(new MASAuthenticationListener() {
            @Override
            public void onAuthenticateRequest(Context context, long requestId, MASAuthenticationProviders providers) {
                MASUser.login("admin", "layer7".toCharArray(), null);
            }

            @Override
            public void onOtpAuthenticateRequest(Context context, MASOtpAuthenticationHandler handler) {

            }
        });
    }

    @After
    public void masStop() {
        if (isSkipped) return;

        MASCallbackFuture<Void> masCallback = new MASCallbackFuture<>();
        MASDevice.getCurrentDevice().deregister(masCallback);
        try {
            masCallback.get();
        } catch (Exception ignore) {
            //Ignore
        }
        MASDevice.getCurrentDevice().resetLocally();
        MAS.cancelAllRequests();
        MAS.stop();
    }

    protected boolean isLocal() {
        return MASConfiguration.getCurrentConfiguration().getGatewayHostName().equals("localhost");
    }

}