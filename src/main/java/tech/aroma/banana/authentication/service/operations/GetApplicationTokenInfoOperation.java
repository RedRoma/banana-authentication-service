/*
 * Copyright 2015 Aroma Tech.
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

package tech.aroma.banana.authentication.service.operations;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aroma.banana.authentication.service.data.TokenRepository;
import tech.aroma.banana.thrift.authentication.service.GetApplicationTokenInfoRequest;
import tech.aroma.banana.thrift.authentication.service.GetApplicationTokenInfoResponse;
import tech.aroma.banana.thrift.exceptions.InvalidArgumentException;
import tech.sirwellington.alchemy.annotations.access.Internal;
import tech.sirwellington.alchemy.thrift.operations.ThriftOperation;

import static tech.aroma.banana.authentication.service.AuthenticationAssertions.checkRequestNotNull;
import static tech.sirwellington.alchemy.arguments.Arguments.checkThat;
import static tech.sirwellington.alchemy.arguments.assertions.StringAssertions.nonEmptyString;
import static tech.sirwellington.alchemy.generator.AlchemyGenerator.one;
import static tech.sirwellington.alchemy.generator.ObjectGenerators.pojos;

/**
 *
 * @author SirWellington
 */
@Internal
final class GetApplicationTokenInfoOperation implements ThriftOperation<GetApplicationTokenInfoRequest, GetApplicationTokenInfoResponse>
{

    private final static Logger LOG = LoggerFactory.getLogger(GetApplicationTokenInfoOperation.class);

    private TokenRepository tokenRepository;

    @Override
    public GetApplicationTokenInfoResponse process(GetApplicationTokenInfoRequest request) throws TException
    {
        LOG.debug("Received request to get token info: {}", request);

        checkRequestNotNull(request);
        checkRequestNotNull(request.token);
        
        String tokenId = request.token.token;
        String applicationId = request.token.applicationId;
        
        checkThat(tokenId, applicationId)
            .throwing(ex -> new InvalidArgumentException("tokenId and applicationid are required"))
            .are(nonEmptyString());
        

        GetApplicationTokenInfoResponse response = one(pojos(GetApplicationTokenInfoResponse.class));

        return response;
    }

}
