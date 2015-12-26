
package tech.aroma.banana.authentication.service.server;

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



import com.google.inject.Guice;
import com.google.inject.Injector;
import java.net.SocketException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.aroma.banana.authentication.service.AuthenticationServiceModule;
import tech.aroma.banana.authentication.service.data.AuthenticationDataModule;
import tech.aroma.banana.authentication.service.operations.AuthenticationOperationsModule;
import tech.aroma.banana.thrift.authentication.service.AuthenticationService;
import tech.aroma.banana.thrift.authentication.service.AuthenticationServiceConstants;
import tech.sirwellington.alchemy.annotations.access.Internal;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This Main Class runs the Authentication Service on a Server Socket.
 * 
 * @author SirWellington
 */
@Internal
public final class TcpServer
{

    private final static Logger LOG = LoggerFactory.getLogger(TcpServer.class);
    private static final int PORT = AuthenticationServiceConstants.SERVICE_PORT;

    public static void main(String[] args) throws TTransportException, SocketException
    {
        Injector injector = Guice.createInjector(new AuthenticationDataModule(),
                                                 new AuthenticationOperationsModule(),
                                                 new AuthenticationServiceModule());

        AuthenticationService.Iface authenticationService = injector.getInstance(AuthenticationService.Iface.class);
        AuthenticationService.Processor processor = new AuthenticationService.Processor<>(authenticationService);

        TServerSocket socket = new TServerSocket(PORT);
        socket.getServerSocket().setSoTimeout((int) SECONDS.toMillis(30));

        TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(socket)
            .protocolFactory(new TBinaryProtocol.Factory())
            .processor(processor)
            .requestTimeout(60)
            .requestTimeoutUnit(SECONDS)
            .minWorkerThreads(5)
            .maxWorkerThreads(100);
        
        LOG.info("Starting Authentication Service at port {}", PORT);
        
        TThreadPoolServer server = new TThreadPoolServer(serverArgs);
        server.serve();
        server.stop();
    }
}