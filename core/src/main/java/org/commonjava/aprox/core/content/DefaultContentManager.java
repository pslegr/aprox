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
package org.commonjava.aprox.core.content;

import static org.commonjava.aprox.util.ContentUtils.dedupeListing;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.content.ContentDigest;
import org.commonjava.aprox.content.ContentGenerator;
import org.commonjava.aprox.content.ContentManager;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultContentManager
    implements ContentManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Instance<ContentGenerator> contentProducerInstances;

    private Set<ContentGenerator> contentGenerators;

    @Inject
    private StoreDataManager storeManager;

    @Inject
    private DownloadManager downloadManager;

    protected DefaultContentManager()
    {
    }

    public DefaultContentManager( final StoreDataManager storeManager, final DownloadManager downloadManager,
                                  final Set<ContentGenerator> contentProducers )
    {
        this.storeManager = storeManager;
        this.downloadManager = downloadManager;
        this.contentGenerators = contentProducers == null ? new HashSet<ContentGenerator>() : contentProducers;
    }

    @PostConstruct
    public void initialize()
    {
        contentGenerators = new HashSet<ContentGenerator>();
        if ( contentProducerInstances != null )
        {
            for ( final ContentGenerator producer : contentProducerInstances )
            {
                contentGenerators.add( producer );
            }
        }
    }

    @Override
    public Transfer retrieveFirst( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        Transfer txfr = null;
        for ( final ArtifactStore store : stores )
        {
            txfr = doRetrieve( store, path );
            if ( txfr != null )
            {
                break;
            }
        }

        return txfr;
    }

    @Override
    public List<Transfer> retrieveAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final List<Transfer> txfrs = new ArrayList<Transfer>();
        for ( final ArtifactStore store : stores )
        {
            if ( StoreType.group == store.getKey()
                                         .getType() )
            {
                List<ArtifactStore> members;
                try
                {
                    members = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
                }
                catch ( final AproxDataException e )
                {
                    throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                      e.getMessage() );
                }

                final List<Transfer> storeTransfers = new ArrayList<Transfer>();
                for ( final ContentGenerator generator : contentGenerators )
                {
                    final Transfer txfr = generator.generateGroupFileContent( (Group) store, members, path );
                    if ( txfr != null )
                    {
                        storeTransfers.add( txfr );
                    }
                }

                // If the content was generated, don't try to retrieve it from a member store...this is the lone exception to retrieveAll
                // ...if it's generated, it's merged in this case.
                if ( storeTransfers.isEmpty() )
                {
                    for ( final ArtifactStore member : members )
                    {
                        // NOTE: This is only safe to call because we're concrete ordered stores, so anything passing through here is concrete.
                        final Transfer txfr = doRetrieve( member, path );
                        if ( txfr != null )
                        {
                            storeTransfers.add( txfr );
                        }
                    }
                }

                txfrs.addAll( storeTransfers );
            }
            else
            {
                // NOTE: This is only safe to call because we're doing the group check up front, so anything passing through here is concrete.
                final Transfer txfr = doRetrieve( store, path );
                if ( txfr != null )
                {
                    txfrs.add( txfr );
                }
            }
        }

        return txfrs;
    }

    @Override
    public Transfer retrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        Transfer item;
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            List<ArtifactStore> members;
            try
            {
                members = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                  e.getMessage() );
            }

            item = null;
            for ( final ContentGenerator generator : contentGenerators )
            {
                item = generator.generateGroupFileContent( (Group) store, members, path );
                if ( item != null )
                {
                    break;
                }
            }

            if ( item == null )
            {
                for ( final ArtifactStore member : members )
                {
                    item = doRetrieve( member, path );
                    if ( item != null )
                    {
                        break;
                    }
                }
            }
        }
        else
        {
            item = doRetrieve( store, path );
        }

        return item;
    }

    private Transfer doRetrieve( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        logger.info( "Attempting to retrieve: {} from: {}", path, store.getKey() );
        Transfer item = downloadManager.retrieve( store, path );

        if ( item == null )
        {
            for ( final ContentGenerator generator : contentGenerators )
            {
                item = generator.generateFileContent( store, path );
                if ( item != null )
                {
                    break;
                }
            }
        }

        return item;
    }

    @Override
    public Transfer store( final ArtifactStore store, final String path, final InputStream stream,
                           final TransferOperation op )
        throws AproxWorkflowException
    {
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            try
            {
                final List<ArtifactStore> allMembers = storeManager.getOrderedConcreteStoresInGroup( store.getName() );

                return store( allMembers, path, stream, op );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                  e.getMessage() );
            }
        }

        final Transfer txfr = downloadManager.store( store, path, stream, op );
        if ( txfr != null )
        {
            final KeyedLocation kl = (KeyedLocation) txfr.getLocation();
            ArtifactStore transferStore;
            try
            {
                transferStore = storeManager.getArtifactStore( kl.getKey() );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup store: %s. Reason: %s", e, kl.getKey(),
                                                  e.getMessage() );
            }

            for ( final ContentGenerator generator : contentGenerators )
            {
                generator.handleContentStorage( transferStore, path, txfr );
            }

            if ( !store.equals( transferStore ) )
            {
                for ( final ContentGenerator generator : contentGenerators )
                {
                    generator.handleContentStorage( transferStore, path, txfr );
                }
            }
        }

        return txfr;
    }

    @Override
    public Transfer store( final List<? extends ArtifactStore> stores, final String path, final InputStream stream,
                           final TransferOperation op )
        throws AproxWorkflowException
    {
        final Transfer txfr = downloadManager.store( stores, path, stream, op );
        if ( txfr != null )
        {
            final KeyedLocation kl = (KeyedLocation) txfr.getLocation();
            ArtifactStore transferStore;
            try
            {
                transferStore = storeManager.getArtifactStore( kl.getKey() );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup store: %s. Reason: %s", e, kl.getKey(),
                                                  e.getMessage() );
            }

            for ( final ContentGenerator generator : contentGenerators )
            {
                generator.handleContentStorage( transferStore, path, txfr );
            }
        }

        return txfr;
    }

    @Override
    public boolean delete( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        boolean result = false;
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            List<ArtifactStore> members;
            try
            {
                members = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                  e.getMessage() );
            }

            for ( final ArtifactStore member : members )
            {
                if ( downloadManager.delete( member, path ) )
                {
                    result = true;
                    for ( final ContentGenerator generator : contentGenerators )
                    {
                        generator.handleContentDeletion( member, path );
                    }
                }
            }

            if ( result )
            {
                for ( final ContentGenerator generator : contentGenerators )
                {
                    generator.handleContentDeletion( store, path );
                }
            }
        }
        else
        {
            if ( downloadManager.delete( store, path ) )
            {
                result = true;
                for ( final ContentGenerator generator : contentGenerators )
                {
                    generator.handleContentDeletion( store, path );
                }
            }
        }

        return result;
    }

    @Override
    public boolean deleteAll( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        boolean result = false;
        for ( final ArtifactStore store : stores )
        {
            result = delete( store, path ) || result;
        }

        return result;
    }

    @Override
    public void rescan( final ArtifactStore store )
        throws AproxWorkflowException
    {
        downloadManager.rescan( store );
    }

    @Override
    public void rescanAll( final List<? extends ArtifactStore> stores )
        throws AproxWorkflowException
    {
        downloadManager.rescanAll( stores );
    }

    @Override
    public List<StoreResource> list( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        List<StoreResource> listed;
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            List<ArtifactStore> members;
            try
            {
                members = storeManager.getOrderedConcreteStoresInGroup( store.getName() );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                  e.getMessage() );
            }

            listed = new ArrayList<StoreResource>();
            for ( final ContentGenerator generator : contentGenerators )
            {
                final List<StoreResource> generated =
                    generator.generateGroupDirectoryContent( (Group) store, members, path );
                if ( generated != null )
                {
                    listed.addAll( generated );
                }
            }

            for ( final ArtifactStore member : members )
            {
                final List<StoreResource> storeListing = list( member, path );
                if ( storeListing != null )
                {
                    listed.addAll( storeListing );
                }
            }
        }
        else
        {
            listed = downloadManager.list( store, path );

            for ( final ContentGenerator producer : contentGenerators )
            {
                final List<StoreResource> produced = producer.generateDirectoryContent( store, path, listed );
                if ( produced != null )
                {
                    listed.addAll( produced );
                }
            }
        }

        return dedupeListing( listed );
    }

    @Override
    public List<StoreResource> list( final List<? extends ArtifactStore> stores, final String path )
        throws AproxWorkflowException
    {
        final List<StoreResource> listed = new ArrayList<StoreResource>();
        for ( final ArtifactStore store : stores )
        {
            final List<StoreResource> storeListing = list( store, path );
            if ( storeListing != null )
            {
                listed.addAll( storeListing );
            }
        }

        return dedupeListing( listed );
    }

    @Override
    public Map<ContentDigest, String> digest( final StoreKey key, final String path, final ContentDigest... types )
        throws AproxWorkflowException
    {
        final Transfer txfr = downloadManager.getStorageReference( key, path );
        if ( txfr == null || !txfr.exists() )
        {
            return Collections.emptyMap();
        }

        InputStream stream = null;
        try
        {
            // TODO: Compute it as the file is uploaded/downloaded into cache.
            stream = txfr.openInputStream( false );

            final Map<ContentDigest, MessageDigest> digests = new HashMap<>();
            for ( final ContentDigest digest : types )
            {
                digests.put( digest, MessageDigest.getInstance( digest.digestName() ) );
            }

            final byte[] buf = new byte[16384];
            int read = -1;
            while ( ( read = stream.read( buf ) ) > -1 )
            {
                for ( final MessageDigest digest : digests.values() )
                {
                    digest.update( buf, 0, read );
                }
            }

            final Map<ContentDigest, String> result = new HashMap<>();
            for ( final Map.Entry<ContentDigest, MessageDigest> entry : digests.entrySet() )
            {
                final StringBuilder sb = new StringBuilder();
                for ( final byte b : entry.getValue()
                                          .digest() )
                {
                    final String hex = Integer.toHexString( b & 0xff );
                    if ( hex.length() < 2 )
                    {
                        sb.append( '0' );
                    }
                    sb.append( hex );
                }

                result.put( entry.getKey(), sb.toString() );
            }

            return result;
        }
        catch ( IOException | NoSuchAlgorithmException e )
        {
            throw new AproxWorkflowException( "Failed to calculate checksums (MD5, SHA-256) for: %s. Reason: %s", e,
                                              txfr, e.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }
    }

    @Override
    public Transfer getTransfer( final StoreKey store, final String path, final TransferOperation op )
        throws AproxWorkflowException
    {
        try
        {
            return getTransfer( storeManager.getArtifactStore( store ), path, op );
        }
        catch ( final AproxDataException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                              "Failed to retrieve ArtifactStore for key: %s. Reason: %s", e, store,
                                              e.getMessage() );

        }
    }

    @Override
    public Transfer getTransfer( final ArtifactStore store, final String path, final TransferOperation op )
        throws AproxWorkflowException
    {
        if ( StoreType.group == store.getKey()
                                     .getType() )
        {
            try
            {
                final List<ArtifactStore> allMembers = storeManager.getOrderedConcreteStoresInGroup( store.getName() );

                return getTransfer( allMembers, path, op );
            }
            catch ( final AproxDataException e )
            {
                throw new AproxWorkflowException( "Failed to lookup concrete members of: %s. Reason: %s", e, store,
                                                  e.getMessage() );
            }
        }

        return downloadManager.getStorageReference( store, path, op );
    }

    @Override
    public Transfer getTransfer( final List<ArtifactStore> stores, final String path, final TransferOperation op )
        throws AproxWorkflowException
    {
        return downloadManager.getStorageReference( stores, path, op );
    }

}
