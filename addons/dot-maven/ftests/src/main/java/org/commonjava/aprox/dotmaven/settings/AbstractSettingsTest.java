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
package org.commonjava.aprox.dotmaven.settings;

import static org.apache.http.client.utils.URIUtils.resolve;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;

import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.ftest.core.fixture.AproxRawHttpModule;
import org.commonjava.maven.galley.util.PathUtils;

/**
 * Base class for tests that verify dotMaven settings generation.
 */
public class AbstractSettingsTest
    extends AbstractAproxFunctionalTest
{

    protected final String getDotMavenUrl( final String path )
        throws URISyntaxException, MalformedURLException
    {
        final String mavdavPath = PathUtils.normalize( "/../mavdav", path );
        System.out.println( "Resolving dotMaven URL. Base URL: '" + client.getBaseUrl() + "'\ndotMaven path: '"
            + mavdavPath + "'" );
        
        final URI result = resolve( new URI( client.getBaseUrl() ), new URI( mavdavPath ) );
        System.out.println( "Resulting URI: '" + result.toString() + "'" );
        
        final String url = result.toURL()
                                 .toExternalForm();
        
        System.out.println( "Resulting URL: '" + url + "'" );
        return url;
    }

    /**
     * Retrieve the AProx client HTTP component, which helps with raw requests to the AProx instance.
     */
    protected final AproxClientHttp getHttp()
        throws AproxClientException
    {
        return client.module( AproxRawHttpModule.class )
                     .getHttp();
    }

    /**
     * Add the {@link AproxRawHttpModule} module to expose the raw HTTP helper component for use in these
     * tests.
     */
    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.<AproxClientModule> singleton( new AproxRawHttpModule() );
    }

}
