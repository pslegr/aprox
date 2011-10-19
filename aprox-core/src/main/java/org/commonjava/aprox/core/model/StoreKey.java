package org.commonjava.aprox.core.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;

public final class StoreKey
{
    // private static final Logger logger = new Logger( StoreKey.class );

    private final StoreType type;

    private final String name;

    public StoreKey( final StoreType type, final String name )
    {
        this.type = type;
        this.name = name;
    }

    public StoreType getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return namespaceId( type.name(), name );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        StoreKey other = (StoreKey) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        if ( type != other.type )
        {
            return false;
        }
        return true;
    }

    public static StoreKey fromString( final String id )
    {
        int idx = id.indexOf( ':' );

        String name;
        StoreType type;
        if ( idx < 1 )
        {
            name = id;
            type = StoreType.repository;
        }
        else
        {
            name = id.substring( idx + 1 );
            type = StoreType.valueOf( id.substring( 0, idx ) );
        }

        // logger.info( "parsed store-key with type: '%s' and name: '%s'", type, name );

        return new StoreKey( type, name );
    }
}