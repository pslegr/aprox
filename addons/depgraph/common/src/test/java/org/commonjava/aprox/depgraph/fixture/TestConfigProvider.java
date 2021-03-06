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
package org.commonjava.aprox.depgraph.fixture;

import java.io.File;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.depgraph.conf.AproxDepgraphConfig;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;

@javax.enterprise.context.ApplicationScoped
public class TestConfigProvider
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private AproxConfiguration proxyConfig;

    private DefaultStorageProviderConfiguration storageConfig;

    private AproxDepgraphConfig config;

    private DataFileConfiguration dbConfig;

    private File dbDir;

    @PostConstruct
    public void start()
        throws IOException
    {
        dbDir = File.createTempFile( "depgraph.live.", ".dir" );
        dbDir.delete();
        dbDir.mkdirs();
    }

    @Produces
    @Default
    @TestData
    public synchronized DataFileConfiguration getFlatFileConfig()
        throws IOException
    {
        if ( dbConfig == null )
        {
            dbConfig = new DataFileConfiguration().withDataBasedir( dbDir );
        }

        return dbConfig;
    }

    @Produces
    @Default
    @TestData
    public synchronized AproxDepgraphConfig getDepgraphConfig()
    {
        if ( config == null )
        {
            config = new AproxDepgraphConfig();
        }

        return config;
    }

    @Produces
    @Default
    public synchronized AproxConfiguration getProxyConfig()
    {
        if ( proxyConfig == null )
        {
            proxyConfig = new DefaultAproxConfiguration();
        }
        return proxyConfig;
    }

    @Produces
    @Default
    public synchronized DefaultStorageProviderConfiguration getStorageProviderConfiguration()
        throws IOException
    {
        if ( storageConfig == null )
        {
            final String path = System.getProperty( REPO_ROOT_DIR );
            File dir;
            if ( path == null )
            {
                dir = File.createTempFile( "repo.root", ".dir" );
                dir.delete();
                dir.mkdirs();
            }
            else
            {
                dir = new File( path );
            }
            storageConfig = new DefaultStorageProviderConfiguration( dir );
        }

        return storageConfig;
    }
}
