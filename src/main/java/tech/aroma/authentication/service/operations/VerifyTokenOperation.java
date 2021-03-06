/*
 * Copyright 2017 RedRoma, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.aroma.authentication.service.operations;

import com.google.common.base.Strings;
import javax.inject.Inject;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aroma.data.TokenRepository;
import tech.aroma.thrift.authentication.AuthenticationToken;
import tech.aroma.thrift.authentication.TokenStatus;
import tech.aroma.thrift.authentication.service.VerifyTokenRequest;
import tech.aroma.thrift.authentication.service.VerifyTokenResponse;
import tech.aroma.thrift.exceptions.InvalidTokenException;
import tech.aroma.thrift.exceptions.OperationFailedException;
import tech.aroma.thrift.functions.TimeFunctions;
import tech.sirwellington.alchemy.annotations.access.Internal;
import tech.sirwellington.alchemy.arguments.AlchemyAssertion;
import tech.sirwellington.alchemy.arguments.FailedAssertionException;
import tech.sirwellington.alchemy.thrift.operations.ThriftOperation;

import static tech.aroma.thrift.assertions.AromaAssertions.checkRequestNotNull;
import static tech.aroma.thrift.assertions.AromaAssertions.withMessage;
import static tech.aroma.thrift.authentication.TokenStatus.EXPIRED;
import static tech.sirwellington.alchemy.arguments.Arguments.checkThat;
import static tech.sirwellington.alchemy.arguments.assertions.Assertions.notNull;
import static tech.sirwellington.alchemy.arguments.assertions.StringAssertions.nonEmptyString;

/**
 *
 * @author SirWellington
 */
@Internal
final class VerifyTokenOperation implements ThriftOperation<VerifyTokenRequest, VerifyTokenResponse>
{

    private final static Logger LOG = LoggerFactory.getLogger(VerifyTokenOperation.class);

    private final TokenRepository repository;

    @Inject
    VerifyTokenOperation(TokenRepository repository)
    {
        checkThat(repository).is(notNull());

        this.repository = repository;
    }

    @Override
    public VerifyTokenResponse process(VerifyTokenRequest request) throws TException
    {
        LOG.debug("Received request to verify an  token: {}", request);

        checkRequestNotNull(request);

        String tokenId = request.tokenId;
        checkThat(tokenId)
            .throwing(withMessage("missing tokenId"))
            .is(nonEmptyString());
        
        AuthenticationToken token = repository.getToken(tokenId);
        
        checkThat(token)
            .throwing(InvalidTokenException.class)
            .usingMessage("Token is expired")
            .is(notExpired());

        //Update and check if the token is expired.
        if(expirationDateHasPassed(token))
        {
            saveAsExpired(token);
            throw new InvalidTokenException("Token has expired.");
        }

        if (shouldCheckAgainstOwner(request))
        {
            String ownerId = request.ownerId;
            ensureTokenAndOwnerMatch(tokenId, ownerId);
        }
        

        return new VerifyTokenResponse();

    }

    private boolean shouldCheckAgainstOwner(VerifyTokenRequest request)
    {
        return request.isSetOwnerId() && !Strings.isNullOrEmpty(request.ownerId);
    }

    private void ensureTokenAndOwnerMatch(String tokenId, String ownerId) throws OperationFailedException, InvalidTokenException
    {

        boolean tokenAndOwnerMatch = tryDetermineMatch(tokenId, ownerId);

        if (!tokenAndOwnerMatch)
        {
            throw new InvalidTokenException();
        }
    }

    private boolean tryDetermineMatch(String tokenId, String ownerId) throws OperationFailedException
    {
        boolean match;

        try
        {
            match = repository.doesTokenBelongTo(tokenId, ownerId);
        }
        catch (Exception ex)
        {
            throw new OperationFailedException("Could not read token repository: " + ex.getMessage());
        }

        return match;
    }
    
    private AlchemyAssertion<AuthenticationToken> notExpired()
    {
        return token ->
        {
            if (token.status == EXPIRED)
            {
                throw new FailedAssertionException("Token has expired");
            }
        };
    }

    private void saveAsExpired(AuthenticationToken token) throws TException
    {
        token.setStatus(TokenStatus.EXPIRED);
        
        repository.saveToken(token);
    }

    private boolean expirationDateHasPassed(AuthenticationToken token)
    {
        return TimeFunctions.isInThePast(token.timeOfExpiration);
    }



}
