/*
 *  Copyright (c) 2016 CA. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license.  See the LICENSE file for details.
 */

package com.ca.mas.foundation;

import com.ca.mas.core.conf.ConfigurationManager;
import com.ca.mas.core.conf.Server;
import com.ca.mas.core.http.ContentType;
import com.ca.mas.core.http.MAGRequest;
import com.ca.mas.core.http.MAGRequestBody;
import com.ca.mas.core.request.internal.MAGRequestProxy;
import com.ca.mas.core.store.StorageProvider;
import com.ca.mas.core.store.TokenManager;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.util.DateUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.PrivateKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class JwtSignRequest extends MAGRequestProxy {

    JwtSignRequest(MAGRequest request) {
        this.request = request;
    }

    @Override
    public MAGRequestBody getBody() {
        MAGRequestBody body = super.getBody();
        if (body != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                body.write(baos);
                byte[] data = baos.toByteArray();
                JWSSigner signer = new RSASSASigner(getPrivateKey());
                JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder();
                TokenManager tokenManager = StorageProvider.getInstance().getTokenManager();

                // JWT claims
                // iss
                String magId = tokenManager.getMagIdentifier();
                claimBuilder.issuer("device://" + magId + "/$");

                // TODO: sub: username

                // aud
                Server server = ConfigurationManager.getInstance().getConnectedGateway();
                String gateway = server.getHost();
                claimBuilder.audience(gateway);

                // jti
                UUID uuid = UUID.randomUUID();
                claimBuilder.jwtID(uuid.toString());

                // iat
                long currentTime = System.currentTimeMillis() / 1000;
                Date currentDate = DateUtils.fromSecondsSinceEpoch(currentTime);
                claimBuilder.issueTime(currentDate);

                // exp
                /* TODO fix the timeout
                TimeUnit timeUnit = getTimeUnit();
                if (timeUnit == null) {
                    timeUnit = TimeUnit.DAYS.SECONDS;
                }

                long timeOut = TimeUnit.SECONDS.convert(getTimeout(), timeUnit);
                if (timeOut != 0) {
                    Date expiryDate = DateUtils.fromSecondsSinceEpoch(timeOut + currentTime);
                    claimBuilder.expirationTime(expiryDate);
                }
                */

                claimBuilder.claim("content", new String(data));
                claimBuilder.claim("content-type", ContentType.APPLICATION_JSON.getMimeType());

                JWSHeader rs256Header = new JWSHeader(JWSAlgorithm.RS256);
                SignedJWT claimsToken = new SignedJWT(rs256Header, claimBuilder.build());
                claimsToken.sign(signer);

                //JWSObject jwsObject = new JWSObject(rs256Header, new Payload(claimsToken));

                String compactJws = claimsToken.serialize();
                return MAGRequestBody.stringBody(compactJws);
            } catch (IOException | JOSEException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    protected PrivateKey getPrivateKey() {
        return StorageProvider.getInstance().getTokenManager().getClientPrivateKey();
    }
}