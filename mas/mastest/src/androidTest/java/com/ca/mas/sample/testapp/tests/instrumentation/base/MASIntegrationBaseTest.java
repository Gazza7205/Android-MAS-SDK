/*
 * Copyright (c) 2016 CA. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 *
 */

package com.ca.mas.sample.testapp.tests.instrumentation.base;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.ca.mas.foundation.MAS;
import com.ca.mas.foundation.MASConnectionListener;
import com.ca.mas.foundation.MASDevice;
import com.ca.mas.foundation.MASCallback;
import com.ca.mas.foundation.MASUser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public abstract class MASIntegrationBaseTest {

    public static final int TIMEOUT = 5;

    @BeforeClass
    public static void beforeClass() {
        MAS.debug();
        MAS.setConnectionListener(new MASConnectionListener() {
            @Override
            public void onObtained(HttpURLConnection connection) {
                ((HttpsURLConnection) connection).setHostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            }

            @Override
            public void onConnected(HttpURLConnection connection) {

            }
        });
        MAS.start(InstrumentationRegistry.getInstrumentation().getTargetContext(), true);
        login();
    }

    public static void login() {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        MASUser.login(getUsername(), getPassword(), new MASCallback<MASUser>() {
            @Override
            public void onSuccess(MASUser user) {
                result[0] = true;
                latch.countDown();

            }

            @Override
            public void onError(Throwable e) {
                result[0] = false;
                latch.countDown();
            }
        });

        try {
            await(latch);
            assertTrue(result[0]);
        } catch (InterruptedException e) {
            fail();
        }
    }


    @AfterClass
    public static void afterClass() {
        MASDevice masDevice = MASDevice.getCurrentDevice();
        final CountDownLatch latch = new CountDownLatch(1);
        masDevice.deregister(new MASCallback<Void>() {
            @Override
            public void onSuccess(Void object) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable e) {
                latch.countDown();
            }
        });
        try {
            latch.await(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    protected static String getUsername() {
        return "sarek";
    }

    protected static String getPassword() {
        return "StRonG5^)";
    }

    protected static void await(CountDownLatch latch) throws InterruptedException {
        //latch.await(TIMEOUT, TimeUnit.SECONDS);
        latch.await();
    }
}
