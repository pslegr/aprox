/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.mem.model;

import java.net.MalformedURLException;
import java.net.URL;

import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.util.logging.Logger;

import com.google.gson.annotations.SerializedName;

public class MemoryRepository
    extends AbstractArtifactStore
    implements Repository
{
    private static final Logger LOGGER = new Logger( MemoryRepository.class );

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    @SerializedName( "url" )
    private String url;

    @SerializedName( "timeout_seconds" )
    private int timeoutSeconds;

    private String host;

    private int port;

    private String user;

    private String password;

    private boolean cached = true;

    private int cacheTimeoutSeconds;

    MemoryRepository()
    {
        super( StoreType.repository );
    }

    public MemoryRepository( final String name, final String remoteUrl )
    {
        super( StoreType.repository, name );
        this.url = remoteUrl;
        calculateFields();
    }

    MemoryRepository( final String name )
    {
        super( StoreType.repository, name );
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public void setUrl( final String url )
    {
        this.url = url;
    }

    @Override
    public String getUser()
    {
        return user;
    }

    @Override
    public void setUser( final String user )
    {
        this.user = user;
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public void setPassword( final String password )
    {
        this.password = password;
    }

    @Override
    public String getHost()
    {
        return host;
    }

    public void setHost( final String host )
    {
        this.host = host;
    }

    @Override
    public int getPort()
    {
        return port;
    }

    public void setPort( final int port )
    {
        this.port = port;
    }

    public void calculateFields()
    {
        URL url = null;
        try
        {
            url = new URL( this.url );
        }
        catch ( final MalformedURLException e )
        {
            LOGGER.error( "Failed to parse repository URL: '%s'. Reason: %s", e, this.url, e.getMessage() );
        }

        if ( url == null )
        {
            return;
        }

        final String userInfo = url.getUserInfo();
        if ( userInfo != null && user == null && password == null )
        {
            user = userInfo;
            password = null;

            int idx = userInfo.indexOf( ':' );
            if ( idx > 0 )
            {
                user = userInfo.substring( 0, idx );
                password = userInfo.substring( idx + 1 );

                final StringBuilder sb = new StringBuilder();
                idx = this.url.indexOf( "://" );
                sb.append( this.url.substring( 0, idx + 3 ) );

                idx = this.url.indexOf( "@" );
                if ( idx > 0 )
                {
                    sb.append( this.url.substring( idx + 1 ) );
                }

                this.url = sb.toString();
            }
        }

        host = url.getHost();
        if ( url.getPort() < 0 )
        {
            port = url.getProtocol()
                      .equals( "https" ) ? 443 : 80;
        }
        else
        {
            port = url.getPort();
        }
    }

    @Override
    public int getTimeoutSeconds()
    {
        return timeoutSeconds < 0 ? DEFAULT_TIMEOUT_SECONDS : timeoutSeconds;
    }

    @Override
    public void setTimeoutSeconds( final int timeoutSeconds )
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String toString()
    {
        return String.format( "MemoryRepository [url=%s, timeoutSeconds=%s, host=%s, port=%s, user=%s, password=%s, getName()=%s, getKey()=%s]",
                              url, timeoutSeconds, host, port, user, password, getName(), getKey() );
    }

    @Override
    public boolean isCached()
    {
        return cached;
    }

    @Override
    public void setCached( final boolean cached )
    {
        this.cached = cached;
    }

    @Override
    public int getCacheTimeoutSeconds()
    {
        return cacheTimeoutSeconds;
    }

    @Override
    public void setCacheTimeoutSeconds( final int cacheTimeoutSeconds )
    {
        this.cacheTimeoutSeconds = cacheTimeoutSeconds;
    }

}