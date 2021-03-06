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
package org.commonjava.aprox.depgraph.discover;

import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.APROX_URI_PREFIX;
import static org.commonjava.aprox.depgraph.util.AproxDepgraphUtils.toDiscoveryURI;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.Production;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.graph.ViewParams;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.discover.DiscoverySourceManager;
import org.commonjava.maven.galley.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Production
@Default
public class AproxDiscoverySourceManager
    implements DiscoverySourceManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager stores;

    protected AproxDiscoverySourceManager()
    {
    }

    public AproxDiscoverySourceManager( final StoreDataManager stores )
    {
        this.stores = stores;
    }

    @Override
    public URI createSourceURI( final String source )
        throws CartoDataException
    {
        final StoreKey key = getKey( source );

        if ( key == null )
        {
            logger.warn( "Cannot find ArtifactStore associated with: '{}'. Assuming this is a naked URI..." );
            try
            {
                return new URI( source );
            }
            catch ( final URISyntaxException e )
            {
                throw new CartoDataException(
                                              "%s is not a URI. Already failed to map it to an ArtifactStore. Unknown source specification. (URI error: %s)",
                                              e, source, e.getMessage() );
            }
        }

        logger.debug( "Got source-URI store-key: '{}'", key );
        final URI result = toDiscoveryURI( key );
        logger.debug( "Resulting source URI: '{}'", result );
        return result;
    }

    private StoreKey getKey( final String source )
        throws CartoDataException
    {
        StoreKey key = StoreKey.fromString( normalize( source ) );
        if ( key == null )
        {
            try
            {
                final List<RemoteRepository> allRemotes = stores.getAllRemoteRepositories();
                for ( final RemoteRepository remote : allRemotes )
                {
                    if ( remote.getUrl()
                               .equals( source ) )
                    {
                        key = remote.getKey();
                        break;
                    }
                }
            }
            catch ( final AproxDataException e )
            {
                throw new CartoDataException(
                                              "Failed to lookup ArtifactStore instances to search for URL: %s. Reason: %s",
                                              e, source, e.getMessage() );
            }
        }

        return key;
    }

    private String normalize( final String source )
    {
        logger.debug( "Normalizing source URI: '{}'", source );
        if ( source.startsWith( APROX_URI_PREFIX ) )
        {
            final String result = source.substring( APROX_URI_PREFIX.length() );
            logger.debug( "Normalized source URI: '{}'", result );
            return result;
        }

        return source;
    }

    @Override
    public String getFormatHint()
    {
        return "<store-type>:<store-name>";
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final Collection<? extends Location> locations )
        throws CartoDataException
    {
        if ( locations == null || locations.isEmpty() )
        {
            return false;
        }

        final Set<String> sources = new HashSet<String>( locations.size() );
        for ( final Location loc : locations )
        {
            sources.add( loc.getUri() );
        }

        return activateWorkspaceSources( params, sources.toArray( new String[sources.size()] ) );
    }

    @Override
    public boolean activateWorkspaceSources( final ViewParams params, final String... sources )
        throws CartoDataException
    {
        boolean result = false;
        if ( params != null )
        {
            final Set<URI> newSources = new HashSet<URI>();
            for ( final String src : sources )
            {
                final StoreKey key = getKey( src );
                if ( key == null )
                {
                    logger.warn( "Cannot find ArtifactStore associated with: '{}'. Assuming this is a naked URI..." );
                    try
                    {
                        newSources.add( new URI( src ) );
                    }
                    catch ( final URISyntaxException e )
                    {
                        throw new CartoDataException(
                                                      "%s is not a URI. Already failed to map it to an ArtifactStore. Unknown source specification. (URI error: %s)",
                                                      e, src, e.getMessage() );
                    }
                }
                else
                {
                    if ( key.getType() == StoreType.group )
                    {
                        try
                        {
                            final List<ArtifactStore> orderedStores =
                                stores.getOrderedConcreteStoresInGroup( key.getName() );

                            for ( final ArtifactStore store : orderedStores )
                            {
                                newSources.add( toDiscoveryURI( store.getKey() ) );
                            }
                        }
                        catch ( final AproxDataException e )
                        {
                            throw new CartoDataException(
                                                          "Failed to lookup ordered concrete stores for: {}. Reason: {}",
                                                          e, key, e.getMessage() );
                        }
                    }
                    else
                    {
                        newSources.add( toDiscoveryURI( key ) );
                    }
                }

                for ( final URI uri : newSources )
                {
                    if ( params.getActiveSources()
                               .contains( uri ) )
                    {
                        continue;
                    }

                    params.addActiveSource( uri );
                    result = result || params.getActiveSources()
                                             .contains( uri );
                }
            }
        }

        return result;
    }

    @Override
    public Location createLocation( final Object source )
    {
        final StoreKey key = StoreKey.fromString( normalize( source.toString() ) );
        ArtifactStore store = null;
        try
        {
            store = stores.getArtifactStore( key );
        }
        catch ( final AproxDataException e )
        {
            logger.error( String.format( "Failed to lookup ArtifactStore for key: {}. Reason: {}", key, e.getMessage() ),
                          e );
        }

        return store == null ? null : LocationUtils.toLocation( store );
    }

    @Override
    public List<? extends Location> createLocations( final Object... sources )
        throws CartoDataException
    {
        return createLocations( Arrays.asList( sources ) );
    }

    @Override
    public List<? extends Location> createLocations( final Collection<Object> sources )
        throws CartoDataException
    {
        final List<Location> results = new ArrayList<Location>();
        for ( final Object source : sources )
        {
            if ( source instanceof KeyedLocation )
            {
                results.add( (KeyedLocation) source );
            }
            else
            {
                final StoreKey key = getKey( source.toString() );

                ArtifactStore store = null;
                try
                {
                    store = this.stores.getArtifactStore( key );
                }
                catch ( final AproxDataException e )
                {
                    logger.error( String.format( "Failed to lookup ArtifactStore for key: %s. Reason: %s", key,
                                                 e.getMessage() ), e );
                }

                if ( store == null )
                {
                    logger.error( "No such ArtifactStore for key: {}", key );
                }
                else
                {
                    results.add( LocationUtils.toLocation( store ) );
                }
            }

        }

        return results;
    }

}
