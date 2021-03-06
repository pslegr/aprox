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
package org.commonjava.aprox.test.fixture.core;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.data.StoreEventDispatcher;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.flat.data.DataFileStoreDataManager;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.subsys.datafile.DataFileManager;
import org.commonjava.aprox.subsys.datafile.change.DataFileEventManager;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.web.config.ConfigurationException;
import org.junit.rules.TemporaryFolder;

@ApplicationScoped
public class CoreServerProvider
{

    private final TemporaryFolder folder = new TemporaryFolder();

    @Inject
    private DefaultAproxConfiguration.FeatureConfig aproxConfigFeature;

    private AproxConfiguration config;

    private NotFoundCache nfc;

    private DataFileStoreDataManager storeManager;

    private DefaultStorageProviderConfiguration storageConfig;

    private XMLInfrastructure xml;

    private DataFileManager dataFileManager;

    @Inject
    private AproxObjectMapper objectMapper;

    @Inject
    private StoreEventDispatcher storeDispatch;

    @Inject
    private DataFileEventManager dataFileEvents;

    private StandardTypeMapper typeMapper;

    @PostConstruct
    public void init()
    {
        try
        {
            folder.create();
            this.nfc = new MemoryNotFoundCache();
            this.dataFileManager =
                new DataFileManager( new DataFileConfiguration( folder.newFolder( "aprox-data" ) ), dataFileEvents );
            this.storeManager = new DataFileStoreDataManager( dataFileManager, objectMapper, storeDispatch );
            this.storageConfig = new DefaultStorageProviderConfiguration( folder.newFolder( "aprox-storage" ) );

            this.config = aproxConfigFeature.getAproxConfig();
            this.xml = new XMLInfrastructure();
            this.typeMapper = new StandardTypeMapper();
        }
        catch ( IOException | ConfigurationException e )
        {
            throw new IllegalStateException( "Failed to start core server provider: " + e.getMessage(), e );
        }
        finally
        {
        }
    }

    @PreDestroy
    public void stop()
    {
        folder.delete();
    }

    @Produces
    @Default
    public TypeMapper getTypeMapper()
    {
        return typeMapper;
    }

    @Produces
    @Default
    public XMLInfrastructure getXML()
    {
        return xml;
    }

    @Produces
    @Default
    public AproxConfiguration getAproxConfig()
    {
        return config;
    }

    @Produces
    @Default
    public DefaultStorageProviderConfiguration getStorageProviderConfig()
    {
        return storageConfig;
    }

    @Produces
    @Default
    public NotFoundCache getNfc()
    {
        return nfc;
    }

    @Produces
    @Default
    public DataFileStoreDataManager getStoreDataManager()
    {
        return storeManager;
    }

}
