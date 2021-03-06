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
package org.commonjava.aprox.depgraph.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.commonjava.aprox.inject.Production;
import org.commonjava.maven.atlas.graph.RelationshipGraph;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.ArtifactPathInfo;
import org.commonjava.maven.atlas.ident.version.InvalidVersionSpecificationException;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.event.CartoEventManager;
import org.commonjava.maven.cartographer.event.CartoEventManagerImpl;
import org.commonjava.maven.cartographer.event.ProjectRelationshipsErrorEvent;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Resource;
import org.commonjava.maven.galley.model.VirtualResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Default
@Production
public class AproxDepgraphEvents
    implements CartoEventManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private CartoEventManagerImpl delegate;

    @Inject
    private Event<RelationshipStorageEvent> storageEvents;

    @Inject
    private Event<ProjectRelationshipsErrorEvent> errorEvents;

    protected AproxDepgraphEvents()
    {
    }

    public AproxDepgraphEvents( final CartoEventManagerImpl delegate )
    {
        this.delegate = delegate;
    }

    @PostConstruct
    public void setup()
    {
        delegate = new CartoEventManagerImpl();
    }

    public void unlockOnFileErrorEvent( @Observes final FileErrorEvent evt )
    {
        final String path = evt.getTransfer()
                               .getPath();
        try
        {
            final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
            //            logger.info( "Unlocking {} due to file download error.", info );
            if ( info != null )
            {
                final ProjectVersionRef ref =
                    new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                delegate.notifyOfGraph( ref );
            }
        }
        catch ( final InvalidVersionSpecificationException e )
        {
            logger.error( String.format( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s",
                                         path, e.getMessage() ), e );
        }
    }

    public void unlockOnFileNotFoundEvent( @Observes final FileNotFoundEvent evt )
    {
        final Resource resource = evt.getResource();

        List<ConcreteResource> resources;
        if ( resource instanceof VirtualResource )
        {
            resources = ( (VirtualResource) resource ).toConcreteResources();
        }
        else
        {
            resources = Collections.singletonList( (ConcreteResource) resource );
        }

        final Set<String> seenPaths = new HashSet<String>();
        for ( final ConcreteResource cr : resources )
        {
            final String path = cr.getPath();
            if ( seenPaths.add( path ) )
            {
                try
                {
                    final ArtifactPathInfo info = ArtifactPathInfo.parse( path );
                    //            logger.info( "Unlocking {} due to unresolvable POM.", info );
                    if ( info != null )
                    {
                        final ProjectVersionRef ref =
                            new ProjectVersionRef( info.getGroupId(), info.getArtifactId(), info.getVersion() );

                        delegate.notifyOfGraph( ref );
                    }
                }
                catch ( final InvalidVersionSpecificationException e )
                {
                    logger.error( String.format( "Cannot parse version for path: '%s'. Failed to unlock waiting threads. Reason: %s",
                                                 path, e.getMessage() ), e );
                }
            }
        }

    }

    @Override
    public void fireStorageEvent( final RelationshipStorageEvent evt )
    {
        delegate.fireStorageEvent( evt );
        if ( storageEvents != null )
        {
            storageEvents.fire( evt );
        }
    }

    @Override
    public void fireErrorEvent( final ProjectRelationshipsErrorEvent evt )
    {
        delegate.fireErrorEvent( evt );
        if ( errorEvents != null )
        {
            errorEvents.fire( evt );
        }
    }

    @Override
    public void waitForGraph( final ProjectVersionRef ref, final RelationshipGraph graph, final long timeoutMillis )
        throws CartoDataException
    {
        delegate.waitForGraph( ref, graph, timeoutMillis );
    }

}
