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
package org.commonjava.aprox.core.change;

import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.core.data.ProxyDataException;
import org.commonjava.aprox.core.data.ProxyDataManager;
import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.StoreKey;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.change.CouchDocChange;
import org.commonjava.couch.change.dispatch.CouchChangeJ2EEEvent;
import org.commonjava.couch.change.dispatch.ThreadableListener;
import org.commonjava.couch.util.ChangeSynchronizer;
import org.commonjava.util.logging.Logger;

@Singleton
public class GroupConsistencyListener
    implements ThreadableListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private ProxyDataManager proxyDataManager;

    private final ChangeSynchronizer changeSync = new ChangeSynchronizer();

    @Override
    public boolean canProcess( final String id, final boolean deleted )
    {
        return deleted
            && ( id.startsWith( StoreType.repository.name() )
                || id.startsWith( StoreType.deploy_point.name() ) || id.startsWith( StoreType.group.name() ) );
    }

    @Override
    public void documentChanged( final CouchDocChange change )
    {
        String id = change.getId();
        StoreKey key = StoreKey.fromString( id );
        try
        {
            Set<Group> groups = proxyDataManager.getGroupsContaining( key );
            for ( Group group : groups )
            {
                group.removeConstituent( StoreKey.fromString( id ) );
            }

            proxyDataManager.storeGroups( groups );

            changeSync.setChanged();
        }
        catch ( ProxyDataException e )
        {
            logger.error( "Failed to remove group constituent listings for: %s. Error: %s", e, id,
                          e.getMessage() );
        }
    }

    public void storeDeleted( @Observes final CouchChangeJ2EEEvent event )
    {
        CouchDocChange change = event.getChange();
        if ( canProcess( change.getId(), change.isDeleted() ) )
        {
            documentChanged( change );
        }
    }

    @Override
    public void waitForChange( final long totalMillis, final long pollingMillis )
    {
        changeSync.waitForChange( totalMillis, pollingMillis );
    }

}