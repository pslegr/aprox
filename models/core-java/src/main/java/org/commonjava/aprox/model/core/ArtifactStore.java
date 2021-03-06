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
package org.commonjava.aprox.model.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo( use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = ArtifactStore.TYPE_ATTR )
@JsonSubTypes( { @Type( name = "remote", value = RemoteRepository.class ),
    @Type( name = "hosted", value = HostedRepository.class ), @Type( name = "group", value = Group.class ) } )
public abstract class ArtifactStore
    implements Serializable
{

    public static final String TYPE_ATTR = "type";

    public static final String KEY_ATTR = "key";

    public static final String METADATA_CHANGELOG = "changelog";

    private static final long serialVersionUID = 1L;

    private StoreKey key;

    private String description;

    private transient Map<String, String> transientMetadata;

    private Map<String, String> metadata;

    protected ArtifactStore()
    {
    }

    protected ArtifactStore( final String name )
    {
        this.key = initKey( name );
    }

    /*
     * (non-Javadoc)
     * @see org.commonjava.web.maven.proxy.model.ArtifactStore#getName()
     */
    public String getName()
    {
        return key.getName();
    }

    protected void setName( final String name )
    {
        this.key = initKey( name );
    }

    @Deprecated
    public StoreType getDoctype()
    {
        return getKey().getType();
    }

    public synchronized StoreKey getKey()
    {
        return key;
    }

    protected abstract StoreKey initKey( String name );

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactStore other = (ArtifactStore) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    public void setMetadata( final Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public synchronized String setMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( metadata == null )
        {
            metadata = new HashMap<String, String>();
        }

        return metadata.put( key, value );
    }

    public String getMetadata( final String key )
    {
        return metadata == null ? null : metadata.get( key );
    }

    public synchronized String setTransientMetadata( final String key, final String value )
    {
        if ( key == null || value == null )
        {
            return null;
        }

        if ( transientMetadata == null )
        {
            transientMetadata = new HashMap<String, String>();
        }

        return transientMetadata.put( key, value );
    }

    public String getTransientMetadata( final String key )
    {
        return transientMetadata == null ? null : transientMetadata.get( key );
    }

    @Override
    public String toString()
    {
        return String.format( "ArtifactStore [key=%s]", key );
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }

}
