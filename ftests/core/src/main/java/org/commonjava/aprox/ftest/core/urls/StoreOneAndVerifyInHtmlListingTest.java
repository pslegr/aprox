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
package org.commonjava.aprox.ftest.core.urls;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

public class StoreOneAndVerifyInHtmlListingTest
    extends AbstractCoreUrlsTest
{

    @Test
    public void storeOneFileAndVerifyItInParentDirectoryListing()
        throws Exception
    {
        final byte[] data = "this is a test".getBytes();
        final ByteArrayInputStream stream = new ByteArrayInputStream( data );
        final String root = "/path/to/";
        final String path = root + "foo.txt";

        client.content()
              .store( hosted, STORE, path, stream );

        final AproxClientHttp http = getHttp();

        final HttpGet request = http.newRawGet( client.content()
                                                   .contentUrl( hosted, STORE, root ) );

        request.addHeader( "Accept", "text/html" );

        final CloseableHttpClient hc = http.newClient();
        final CloseableHttpResponse response = hc.execute( request );

        final InputStream listing = response.getEntity()
                                            .getContent();
        final String html = IOUtils.toString( listing );

        // TODO: Charset!!
        final Document doc = Jsoup.parse( html );
        for ( final Element item : doc.select( "a.item-link" ) )
        {
            final String fname = item.text();
            System.out.printf( "Listing contains: '%s'\n", fname );
            final String href = item.attr( "href" );
            final String expected = client.content()
                                          .contentUrl( hosted, STORE, root, fname );

            assertThat( fname + " does not have a href", href, notNullValue() );
            assertThat( fname + " has incorrect link: '" + href + "' (" + href.getClass()
                                                                              .getName() + ")\nshould be: '" + expected
                + "' (String)", href,
                        equalTo( expected ) );
        }
    }

}
