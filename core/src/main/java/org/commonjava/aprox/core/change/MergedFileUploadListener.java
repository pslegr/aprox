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
package org.commonjava.aprox.core.change;

import static org.commonjava.aprox.util.LocationUtils.getKey;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import org.commonjava.aprox.change.event.AproxFileEventManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.core.content.group.ArchetypeCatalogMerger;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.core.content.group.MavenMetadataMerger;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileEvent;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.enterprise.context.ApplicationScoped
public class MergedFileUploadListener
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    @Inject
    private DownloadManager fileManager;

    @Inject
    private AproxFileEventManager fileEvent;

    @Inject
    @ExecutorConfig( daemon = true, priority = 7, named = "aprox-events" )
    private Executor executor;

    // NOTE: Disabling @Observes on this because I'm pretty sure the ContentManager is handling it now.
    public void reMergeUploaded( /*@Observes*/final FileEvent event )
    {
        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                final String path = event.getTransfer()
                                         .getPath();

                final StoreKey key = getKey( event );

                if ( !path.endsWith( MavenMetadataMerger.METADATA_NAME ) && !path.endsWith( ArchetypeCatalogMerger.CATALOG_NAME ) )
                {
                    return;
                }

                try
                {
                    final Set<? extends Group> groups = dataManager.getGroupsContaining( key );

                    if ( groups != null )
                    {
                        for ( final Group group : groups )
                        {
                            try
                            {
                                reMerge( group, path );
                            }
                            catch ( final IOException e )
                            {
                                logger.error( String.format( "Failed to delete: %s from group: %s. Error: %s", path, group, e.getMessage() ), e );
                            }
                        }
                    }
                }
                catch ( final AproxDataException e )
                {
                    logger.warn( "Failed to regenerate maven-metadata.xml for groups after deployment to: {}"
                        + "\nCannot retrieve associated groups: {}", e, key, e.getMessage() );
                }
            }
        } );
    }

    private void reMerge( final Group group, final String path )
        throws IOException
    {
        final Transfer[] toDelete =
            { fileManager.getStorageReference( group, path ),
                fileManager.getStorageReference( group, path + GroupMergeHelper.MERGEINFO_SUFFIX ),
                fileManager.getStorageReference( group, path + GroupMergeHelper.SHA_SUFFIX ),
                fileManager.getStorageReference( group, path + GroupMergeHelper.MD5_SUFFIX ) };

        for ( final Transfer item : toDelete )
        {
            if ( item.exists() )
            {
                item.delete();

                if ( fileEvent != null )
                {
                    fileEvent.fire( new FileDeletionEvent( item ) );
                }
            }
        }
    }

}
