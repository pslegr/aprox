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
package org.commonjava.aprox.data;

import java.util.List;
import java.util.Set;

import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;

/**
 * Data manager used to access and manipulate the configurations for {@link ArtifactStore} instances.
 * @author jdcasey
 *
 */
public interface StoreDataManager
{

    /**
     * Return the {@link HostedRepository} instance corresponding to the given name.
     */
    HostedRepository getHostedRepository( final String name )
        throws AproxDataException;

    /**
     * Return the {@link RemoteRepository} instance corresponding to the given name.
     */
    RemoteRepository getRemoteRepository( final String name )
        throws AproxDataException;

    /**
     * Return the {@link Group} instance corresponding to the given name.
     */
    Group getGroup( final String name )
        throws AproxDataException;

    /**
     * Return the {@link ArtifactStore} instance corresponding to the given key, where key is a composite of {@link StoreType} 
     * (hosted, remote, or group) and name.
     */
    ArtifactStore getArtifactStore( StoreKey key )
        throws AproxDataException;

    /**
     * Return the full list of {@link ArtifactStore} instances available on the system.
     */
    List<ArtifactStore> getAllArtifactStores()
        throws AproxDataException;

    /**
     * Return the full list of {@link ArtifactStore} instances of a given {@link StoreType} (hosted, remote, or group) available on the system.
     */
    List<? extends ArtifactStore> getAllArtifactStores( StoreType type )
        throws AproxDataException;

    /**
     * Return the full list of {@link Group} instances available on the system.
     */
    List<Group> getAllGroups()
        throws AproxDataException;

    /**
     * Return the full list of {@link RemoteRepository} instances available on the system.
     */
    List<RemoteRepository> getAllRemoteRepositories()
        throws AproxDataException;

    /**
     * Return the full list of {@link HostedRepository} instances available on the system.
     */
    List<HostedRepository> getAllHostedRepositories()
        throws AproxDataException;

    /**
     * Return the full list of non-{@link Group} instances available on the system.
     */
    List<ArtifactStore> getAllConcreteArtifactStores()
        throws AproxDataException;

    /**
     * For a {@link Group} with the given name, return (<b>IN ORDER</b>) the list of
     * non-{@link Group} {@link ArtifactStore} instances that are members of the {@link Group}.
     * <br/>
     * <b>NOTE:</b> If any of the group's members are themselves {@link Group}'s, the method
     * recurses and substitutes that group's place in the list with the ordered, concrete stores
     * it contains.
     */
    List<ArtifactStore> getOrderedConcreteStoresInGroup( final String groupName )
        throws AproxDataException;

    /**
     * For a {@link Group} with the given name, return (<b>IN ORDER</b>) the list of
     * non-{@link Group} {@link ArtifactStore} instances that are members of the {@link Group}.
     * <br/>
     * <b>NOTE:</b> This method does <b>not</b> perform recursion to substitute concrete stores in place
     * of any groups in the list. Groups that are members are returned along with the rest of the membership.
     */
    List<ArtifactStore> getOrderedStoresInGroup( final String groupName )
        throws AproxDataException;

    /**
     * Return the set of {@link Group}'s that contain the {@link ArtifactStore} corresponding to the given {@link StoreKey} in their membership.
     */
    Set<Group> getGroupsContaining( final StoreKey repo )
        throws AproxDataException;

    /**
     * Store a modified or new {@link HostedRepository} instance. This is equivalent to 
     * {@link StoreDataManager#storeHostedRepository(HostedRepository, boolean)} with skip flag <code>false</code>
     */
    boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Store a modified or new {@link HostedRepository} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeHostedRepository( final HostedRepository deploy, final ChangeSummary summary,
                                   final boolean skipIfExists )
        throws AproxDataException;

    /**
     * Store a modified or new {@link RemoteRepository} instance. This is equivalent to 
     * {@link StoreDataManager#storeRemoteRepository(RemoteRepository, boolean)} with skip flag <code>false</code>
     */
    boolean storeRemoteRepository( final RemoteRepository proxy, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Store a modified or new {@link RemoteRepository} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeRemoteRepository( final RemoteRepository repository, final ChangeSummary summary,
                                   final boolean skipIfExists )
        throws AproxDataException;

    /**
     * Store a modified or new {@link Group} instance. This is equivalent to 
     * {@link StoreDataManager#storeGroup(Group, boolean)} with skip flag <code>false</code>
     */
    boolean storeGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Store a modified or new {@link Group} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeGroup( final Group group, final ChangeSummary summary, final boolean skipIfExists )
        throws AproxDataException;

    /**
     * Store a modified or new {@link ArtifactStore} instance. This is equivalent to 
     * {@link StoreDataManager#storeArtifactStore(ArtifactStore, boolean)} with skip flag <code>false</code>
     */
    boolean storeArtifactStore( ArtifactStore key, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Store a modified or new {@link ArtifactStore} instance. If the store already exists, and <code>skipIfExists</code> is true, abort the
     * operation.
     */
    boolean storeArtifactStore( ArtifactStore key, final ChangeSummary summary, boolean skipIfExists )
        throws AproxDataException;

    /**
     * Delete the given {@link HostedRepository}.
     */
    void deleteHostedRepository( final HostedRepository deploy, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the {@link HostedRepository} corresponding to the given name.
     */
    void deleteHostedRepository( final String name, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the given {@link RemoteRepository}.
     */
    void deleteRemoteRepository( final RemoteRepository repo, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the {@link RemoteRepository} corresponding to the given name.
     */
    void deleteRemoteRepository( final String name, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the given {@link Group}.
     */
    void deleteGroup( final Group group, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the {@link Group} corresponding to the given name.
     */
    void deleteGroup( final String name, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete the {@link ArtifactStore} corresponding to the given {@link StoreKey}. If the store doesn't exist, simply return (don't fail).
     */
    void deleteArtifactStore( StoreKey key, final ChangeSummary summary )
        throws AproxDataException;

    /**
     * Delete all {@link ArtifactStore} instances currently in the system.
     */
    void clear( final ChangeSummary summary )
        throws AproxDataException;

    /**
     * If no {@link ArtifactStore}'s exist in the system, install a couple of defaults:
     * <ul>
     * <li>Remote <code>central</code> pointing to the Maven central repository at http://repo.maven.apache.org/maven2/</li>
     * <li>Hosted <code>local-deployments</code> that can host both releases and snapshots</li>
     * <li>Group <code>public</code> containing <code>central</code> and <code>local-deployments</code> as members</li>
     * </ul>
     */
    void install()
        throws AproxDataException;

    /**
     * Mechanism for clearing all cached {@link ArtifactStore} instances and reloading them from some backing store.
     */
    void reload()
        throws AproxDataException;

    /**
     * Return true if the system contains a {@link RemoteRepository} with the given name; false otherwise.
     */
    boolean hasRemoteRepository( String name );

    /**
     * Return true if the system contains a {@link Group} with the given name; false otherwise.
     */
    boolean hasGroup( String name );

    /**
     * Return true if the system contains a {@link HostedRepository} with the given name; false otherwise.
     */
    boolean hasHostedRepository( String name );

    /**
     * Return true if the system contains a {@link ArtifactStore} with the given key (combination of {@link StoreType} and name); false otherwise.
     */
    boolean hasArtifactStore( StoreKey key );

    /**
     * Find a remote repository with a URL that matches the given one, and return it...or null.
     */
    RemoteRepository findRemoteRepository( String url );

}
