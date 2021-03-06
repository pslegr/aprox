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
package org.commonjava.aprox.implrepo.skim;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.implrepo.data.ImpliedRepoMetadataManager;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.junit.Test;

public class PomWithRepoAddsRepoToGroupTest
    extends AbstractSkimFunctionalTest
{

    private static final String REPO = "repo-one";

    @Test
    public void skimPomForRepoAndAddIt() throws Exception
    {
        InputStream stream = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResourceAsStream( "one-repo.pom" );
        String pom = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );
        pom = pom.replace( "@one-repo.url@", server.formatUrl( REPO ) );
        
        final PomPeek peek = new PomPeek( pom, false );
        final ProjectVersionRef gav = peek.getKey();
        
        final String path = String.format( "%s/%s/%s/%s-%s.pom", gav.getGroupId().replace('.', '/'), gav.getArtifactId(), gav.getVersionString(), gav.getArtifactId(), gav.getVersionString() );
        
        server.expect( server.formatUrl( TEST_REPO, path ), 200, pom );

        stream = client.content().get( StoreType.group, PUBLIC, path );
        final String downloaded = IOUtils.toString( stream );
        IOUtils.closeQuietly( stream );
        
        assertThat( "SANITY: downloaded POM is wrong!", downloaded, equalTo( pom ) );
        
        // sleep while event observer runs...
        System.out.println( "Waiting 5s for events to run." );
        Thread.sleep( 5000 );

        final Group g = client.stores().load( StoreType.group, PUBLIC, Group.class );
        assertThat( "Group membership does not contain implied repository",
                    g.getConstituents()
                     .contains( new StoreKey( StoreType.remote, REPO ) ), equalTo( true ) );

        RemoteRepository r = client.stores()
                                         .load( StoreType.remote, TEST_REPO, RemoteRepository.class );

        String metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_STORES );

        assertThat( "Reference to repositories implied by POMs in this repo is missing from metadata.",
                    metadata.contains( "remote:" + REPO ), equalTo( true ) );

        r = client.stores()
                  .load( StoreType.remote, REPO, RemoteRepository.class );

        metadata = r.getMetadata( ImpliedRepoMetadataManager.IMPLIED_BY_STORES );

        assertThat( "Backref to repo with pom that implies this repo is missing from metadata.",
                    metadata.contains( "remote:" + TEST_REPO ), equalTo( true ) );
    }

}
