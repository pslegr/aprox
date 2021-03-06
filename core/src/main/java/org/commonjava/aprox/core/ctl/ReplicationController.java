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
package org.commonjava.aprox.core.ctl;

import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.commonjava.aprox.AproxException;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.data.AproxDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.EndpointView;
import org.commonjava.aprox.model.core.dto.EndpointViewListing;
import org.commonjava.aprox.model.core.dto.ReplicationAction;
import org.commonjava.aprox.model.core.dto.ReplicationAction.ActionType;
import org.commonjava.aprox.model.core.dto.ReplicationDTO;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.aprox.subsys.http.util.HttpResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ReplicationController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private AproxHttpProvider http;

    protected ReplicationController()
    {
    }

    public ReplicationController( final StoreDataManager data, final AproxHttpProvider http,
                                  final ObjectMapper serializer )
    {
        this.data = data;
        this.http = http;
        this.serializer = serializer;
    }

    public Set<StoreKey> replicate( final ReplicationDTO dto, final String user )
        throws AproxWorkflowException
    {
        try
        {
            dto.validate();
        }
        catch ( final AproxException e )
        {
            throw new AproxWorkflowException( "Invalid replication request DTO: %s", e, e.getMessage() );
        }

        List<? extends ArtifactStore> remoteStores = null;
        List<EndpointView> remoteEndpoints = null;

        final boolean overwrite = dto.isOverwrite();
        final Set<StoreKey> replicated = new HashSet<StoreKey>();
        for ( final ReplicationAction action : dto )
        {
            if ( action == null )
            {
                continue;
            }

            logger.info( "Processing replication action:\n\n  {}\n\nin DTO: {}\n\n", action, dto );
            final String include = action.getInclude();
            final String exclude = action.getExclude();

            try
            {
                if ( action.getType() == ActionType.PROXY )
                {
                    if ( remoteEndpoints == null )
                    {
                        remoteEndpoints = getEndpoints( dto );
                    }

                    for ( final EndpointView view : remoteEndpoints )
                    {
                        final String key = "remote-" + view.getType() + "_" + view.getName();
                        if ( ( include == null || key.matches( include ) )
                            && ( exclude == null || !key.matches( exclude ) ) )
                        {
                            final StoreKey sk = new StoreKey( StoreType.remote, key );
                            if ( overwrite || !data.hasArtifactStore( sk ) )
                            {
                                final RemoteRepository repo = new RemoteRepository( key, view.getResourceUri() );
                                setProxyAttributes( repo, action );

                                data.storeRemoteRepository( repo, new ChangeSummary( user,
                                                                                     "REPLICATION: Proxying remote aprox repository: "
                                                                                         + view.getResourceUri() ) );
                                replicated.add( repo.getKey() );
                            }
                        }
                    }
                }
                else if ( action.getType() == ActionType.MIRROR )
                {
                    if ( remoteStores == null )
                    {
                        remoteStores = getRemoteStores( dto );
                    }

                    for ( final ArtifactStore store : remoteStores )
                    {
                        final String key = store.getKey()
                                                .toString();
                        if ( ( include == null || key.matches( include ) )
                            && ( exclude == null || !key.matches( exclude ) ) )
                        {
                            if ( overwrite || !data.hasArtifactStore( store.getKey() ) )
                            {
                                if ( store instanceof RemoteRepository )
                                {
                                    setProxyAttributes( ( (RemoteRepository) store ), action );
                                }

                                data.storeArtifactStore( store, new ChangeSummary( user,
                                                                                   "REPLICATION: Mirroring remote aprox store: "
                                                                                       + store.getKey() ) );
                                replicated.add( store.getKey() );
                            }
                        }
                    }
                }
            }
            catch ( final AproxDataException e )
            {
                logger.error( e.getMessage(), e );
                throw new AproxWorkflowException( e.getMessage(), e );
            }
        }

        return replicated;

    }

    private void setProxyAttributes( final RemoteRepository repo, final ReplicationAction action )
    {
        if ( action.getProxyHost() != null )
        {
            repo.setProxyHost( action.getProxyHost() );

            if ( action.getProxyPort() > 0 )
            {
                repo.setProxyPort( action.getProxyPort() );
            }

            if ( action.getProxyUser() != null )
            {
                repo.setProxyUser( action.getProxyUser() );
            }

            if ( action.getProxyPass() != null )
            {
                repo.setProxyPassword( action.getProxyPass() );
            }
        }
    }

    // FIXME: Find a better solution to the passed-in generic deserialization problem...erasure is a mother...
    private List<? extends ArtifactStore> getRemoteStores( final ReplicationDTO dto )
        throws AproxWorkflowException
    {
        final String apiUrl = dto.getApiUrl();

        String remotesUrl = null;
        String groupsUrl = null;
        String hostedUrl = null;
        try
        {
            remotesUrl = buildUrl( apiUrl, "/admin/remotes" );
            groupsUrl = buildUrl( apiUrl, "/admin/groups" );
            hostedUrl = buildUrl( apiUrl, "/admin/hosted" );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to construct store definition-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: {} from:", new Throwable(), url );
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();

        HttpGet req = newGet( remotesUrl, dto );
        HttpResponse response = null;
        try
        {
            response = http.getClient()
                           .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );

                final StoreListingDTO<RemoteRepository> listing =
                    serializer.readValue( json,
                                          serializer.getTypeFactory()
                                                    .constructParametricType( StoreListingDTO.class,
                                                                              RemoteRepository.class ) );

                if ( listing != null )
                {
                    for ( final RemoteRepository store : listing.getItems() )
                    {
                        result.add( store );
                    }
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", remotesUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, remotesUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, remotesUrl,
                                              e.getMessage() );
        }
        finally
        {
            HttpResources.cleanupResources( req, response, null, http );
        }

        req = newGet( groupsUrl, dto );
        response = null;
        try
        {
            response = http.getClient()
                           .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );

                final StoreListingDTO<Group> listing =
                    serializer.readValue( json, serializer.getTypeFactory()
                                                          .constructParametricType( StoreListingDTO.class, Group.class ) );

                for ( final Group store : listing.getItems() )
                {
                    result.add( store );
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: {} failed: {}", groupsUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: {}. Reason: {}", e, groupsUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: {}. Reason: {}", e, groupsUrl,
                                              e.getMessage() );
        }
        finally
        {
            HttpResources.cleanupResources( req, response, null, http );
        }

        req = newGet( hostedUrl, dto );
        response = null;
        try
        {
            response = http.getClient()
                           .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );

                final StoreListingDTO<HostedRepository> listing =
                    serializer.readValue( json,
                                          serializer.getTypeFactory()
                                                    .constructParametricType( StoreListingDTO.class,
                                                                              HostedRepository.class ) );

                for ( final HostedRepository store : listing.getItems() )
                {
                    result.add( store );
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", hostedUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, hostedUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, hostedUrl,
                                              e.getMessage() );
        }
        finally
        {
            HttpResources.cleanupResources( req, response, null, http );
        }

        return result;
    }

    private HttpGet newGet( final String url, final ReplicationDTO dto )
    {
        final HttpGet get = new HttpGet( url );
        final int proxyPort = dto.getProxyPort();
        HttpHost proxy;
        if ( proxyPort < 1 )
        {
            proxy = new HttpHost( dto.getProxyHost(), -1, "http" );
        }
        else
        {
            proxy = new HttpHost( dto.getProxyHost(), dto.getProxyPort(), "http" );
        }

        final RequestConfig config = RequestConfig.custom()
                                                  .setProxy( proxy )
                                                  .build();
        get.setConfig( config );

        return get;
    }

    private List<EndpointView> getEndpoints( final ReplicationDTO dto )
        throws AproxWorkflowException
    {
        final String apiUrl = dto.getApiUrl();
        String url = null;
        try
        {
            url = buildUrl( apiUrl, "/stats/all-endpoints" );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to construct endpoint-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: {} from:", new Throwable(), url );
        final HttpGet req = newGet( url, dto );
        HttpResponse response = null;
        try
        {
            response = http.getClient()
                           .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );
                final EndpointViewListing listing = serializer.readValue( json, EndpointViewListing.class );
                return listing.getItems();
            }

            throw new AproxWorkflowException( status, "Endpoint request failed: {}", statusLine );
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: {}. Reason: {}", e, url,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: {}. Reason: {}", e, url, e.getMessage() );
        }
        finally
        {
            HttpResources.cleanupResources( req, response, null, http );
        }
    }

}
