/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.commonjava.aprox.client.core.helper.PathInfo;
import org.commonjava.aprox.ftest.core.fixture.DelayedDownload;
import org.commonjava.aprox.ftest.core.fixture.InputTimer;
import org.commonjava.aprox.ftest.core.fixture.ReluctantInputStream;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.test.fixture.core.TestHttpServer;
import org.junit.Rule;
import org.junit.Test;

public class DownloadWhileProxyingInProgressTest
    extends AbstractContentManagementTest
{

    @Rule
    public TestHttpServer server = new TestHttpServer( "repos" );

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }

    @Test
    public void downloadWhileSlowProxyCompletes()
        throws Exception
    {
        client.stores()
              .create( new RemoteRepository( STORE, server.formatUrl( STORE ) ), "adding test proxy",
                       RemoteRepository.class );

        final String path = "org/foo/foo-project/1/foo-1.txt";
        final byte[] data = ( "This is a test: " + System.nanoTime() ).getBytes();

        final CountDownLatch latch = new CountDownLatch( 2 );

        final ReluctantInputStream stream = new ReluctantInputStream( data );
        server.expect( server.formatUrl( STORE, path ), 200, stream );

        final InputTimer input = new InputTimer( stream, 10000 / data.length, latch );
        newThread( "input", input ).start();

        final DelayedDownload download = new DelayedDownload( client, new StoreKey( remote, STORE ), path, 5, latch );
        newThread( "download", download ).start();

        System.out.println( "Waiting for content transfers to complete." );
        latch.await();

        final PathInfo result = client.content()
                                      .getInfo( remote, STORE, path );

        assertThat( "no result", result, notNullValue() );
        assertThat( "doesn't exist", result.exists(), equalTo( true ) );

        System.out.printf( "Timing results:\n  Input started: {}\n  Input ended: {}\n  Download started: {}\n  Download ended: {}",
                           input.getStartTime(), input.getEndTime(), download.getStartTime(), download.getEndTime() );

        assertThat( Arrays.equals( download.getContent()
                                           .toByteArray(), data ), equalTo( true ) );
        assertThat( input.getEndTime() > download.getStartTime(), equalTo( true ) );
    }

}
