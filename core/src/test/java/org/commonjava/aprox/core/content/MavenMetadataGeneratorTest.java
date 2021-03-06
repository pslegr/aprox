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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.audit.ChangeSummary;
import org.commonjava.aprox.content.AproxLocationExpander;
import org.commonjava.aprox.content.DownloadManager;
import org.commonjava.aprox.content.StoreResource;
import org.commonjava.aprox.core.content.group.GroupMergeHelper;
import org.commonjava.aprox.core.content.group.MavenMetadataMerger;
import org.commonjava.aprox.core.data.DefaultStoreEventDispatcher;
import org.commonjava.aprox.mem.data.MemoryStoreDataManager;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.LocationUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.util.SnapshotUtils;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.atlas.ident.version.part.SnapshotPart;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.model.view.meta.LatestSnapshotView;
import org.commonjava.maven.galley.maven.model.view.meta.MavenMetadataView;
import org.commonjava.maven.galley.maven.model.view.meta.SnapshotArtifactView;
import org.commonjava.maven.galley.maven.model.view.meta.VersioningView;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.ListingResult;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.testing.core.CoreFixture;
import org.commonjava.maven.galley.testing.core.transport.job.TestListing;
import org.commonjava.maven.galley.testing.maven.GalleyMavenFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MavenMetadataGeneratorTest
{

    @Rule
    public GalleyMavenFixture fixture = new GalleyMavenFixture( new CoreFixture() );

    private MavenMetadataGenerator generator;

    private MemoryStoreDataManager stores;

    private MavenMetadataReader metadataReader;

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    @Before
    public void setup()
        throws Exception
    {
        fixture.initMissingComponents();

        stores = new MemoryStoreDataManager( new DefaultStoreEventDispatcher() );

        final LocationExpander locations = new AproxLocationExpander( stores );

        final DownloadManager downloads = new DefaultDownloadManager( stores, fixture.getTransfers(), locations );

        final XMLInfrastructure xml = new XMLInfrastructure();
        final TypeMapper types = new StandardTypeMapper();
        final MavenMetadataMerger merger = new MavenMetadataMerger();
        final GroupMergeHelper helper = new GroupMergeHelper( downloads );

        generator = new MavenMetadataGenerator( downloads, xml, types, merger, helper );

        metadataReader = new MavenMetadataReader( xml, locations, fixture.getMetadata(), fixture.getXpathManager() );
    }

    @Test
    public void generateFileContent_SnapshotMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupSnapshotDirWith2Snapshots();
        final Transfer transfer =
            generator.generateFileContent( stores.getArtifactStore( resource.getStoreKey() ),
                                           ( (ConcreteResource) resource.getChild( "maven-metadata.xml" ) ).getPath() );

        assertThat( transfer, notNullValue() );

        final MavenMetadataView metadata =
            metadataReader.readMetadata( new ProjectVersionRef( "org.group", "artifact", "1.0-SNAPSHOT" ),
                                         Collections.singletonList( transfer ) );

        assertThat( metadata, notNullValue() );

        final VersioningView versioning = metadata.getVersioning();
        final LatestSnapshotView latestSnapshot = versioning.getLatestSnapshot();
        assertThat( latestSnapshot.isLocalCopy(), equalTo( false ) );

        assertThat( latestSnapshot.getTimestamp(), equalTo( SnapshotUtils.parseSnapshotTimestamp( "20140828.225800" ) ) );
        assertThat( latestSnapshot.getBuildNumber(), equalTo( 1 ) );

        final List<SnapshotArtifactView> snapshots = versioning.getSnapshotArtifacts();
        assertThat( snapshots.size(), equalTo( 4 ) );

        for ( final SnapshotArtifactView snap : snapshots )
        {
            final String extension = snap.getExtension();
            assertThat( extension.equals( "jar" ) || extension.equals( "pom" ), equalTo( true ) );

            final String version = snap.getVersion();
            System.out.println( version );

            final SingleVersion parsed = VersionUtils.createSingleVersion( version );
            assertThat( parsed.isSnapshot(), equalTo( true ) );
            assertThat( parsed.isLocalSnapshot(), equalTo( false ) );

            final SnapshotPart part = parsed.getSnapshotPart();
            final String tstamp = SnapshotUtils.generateSnapshotTimestamp( part.getTimestamp() );
            assertThat( tstamp.equals( "20140828.225800" ) || tstamp.equals( "20140828.221400" ), equalTo( true ) );
        }
    }

    @Test
    public void generateDirContent_SnapshotMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupSnapshotDirWith2Snapshots();

        final List<StoreResource> result =
            generator.generateDirectoryContent( stores.getArtifactStore( resource.getStoreKey() ), resource.getPath(),
                                                Collections.<StoreResource> emptyList() );

        System.out.println( StringUtils.join( result, "\n" ) );

        assertThat( result.size(), equalTo( 3 ) );

        for ( final StoreResource res : result )
        {
            if ( !( res.getPath()
                       .endsWith( MavenMetadataMerger.METADATA_MD5_NAME )
                || res.getPath()
                      .endsWith( MavenMetadataMerger.METADATA_NAME ) || res.getPath()
                                                                           .endsWith( MavenMetadataMerger.METADATA_SHA_NAME ) ) )
            {
                fail( "Invalid generated content: " + res );
            }
        }
    }

    @Test
    public void generateFileContent_VersionsMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupVersionsStructureWith2Versions();
        final ConcreteResource metadataFile = (ConcreteResource) resource.getChild( "maven-metadata.xml" );

        final Transfer transfer =
            generator.generateFileContent( stores.getArtifactStore( resource.getStoreKey() ), metadataFile.getPath() );

        assertThat( transfer, notNullValue() );

        final MavenMetadataView metadata =
            metadataReader.readMetadata( new ProjectVersionRef( "org.group", "artifact", "1.0-SNAPSHOT" ),
                                         Collections.singletonList( transfer ) );

        assertThat( metadata, notNullValue() );

        final VersioningView versioning = metadata.getVersioning();
        final List<SingleVersion> versions = versioning.getVersions();
        assertThat( versions, notNullValue() );

        assertThat( versions.get( 0 )
                            .renderStandard(), equalTo( "1.0" ) );

        assertThat( versions.get( 1 )
                            .renderStandard(), equalTo( "1.1" ) );

        assertThat( versioning.getReleaseVersion()
                              .renderStandard(), equalTo( "1.1" ) );
        assertThat( versioning.getLatestVersion()
                              .renderStandard(), equalTo( "1.1" ) );
    }

    @Test
    public void generateDirContent_VersionsMetadataWith2Versions()
        throws Exception
    {
        final StoreResource resource = setupVersionsStructureWith2Versions();

        final List<StoreResource> result =
            generator.generateDirectoryContent( stores.getArtifactStore( resource.getStoreKey() ), resource.getPath(),
                                                Collections.<StoreResource> emptyList() );

        System.out.println( StringUtils.join( result, "\n" ) );

        assertThat( result.size(), equalTo( 3 ) );

        for ( final StoreResource res : result )
        {
            if ( !( res.getPath()
                       .endsWith( MavenMetadataMerger.METADATA_MD5_NAME )
                || res.getPath()
                      .endsWith( MavenMetadataMerger.METADATA_NAME ) || res.getPath()
                                                                           .endsWith( MavenMetadataMerger.METADATA_SHA_NAME ) ) )
            {
                fail( "Invalid generated content: " + res );
            }
        }
    }

    private StoreResource setupVersionsStructureWith2Versions()
        throws Exception
    {
        final RemoteRepository store = new RemoteRepository( "testrepo", "http://foo.bar" );
        stores.storeArtifactStore( store, summary );

        final String path = "org/group/artifact";

        final KeyedLocation location = LocationUtils.toLocation( store );
        final StoreResource resource = new StoreResource( location, path );

        fixture.getTransport()
               .registerListing( resource,
                                 new TestListing( new ListingResult( resource, new String[] { "1.0/", "1.1/" } ) ) );

        ConcreteResource versionDir = ( (ConcreteResource) resource.getChild( "1.0/" ) );
        fixture.getTransport()
               .registerListing( versionDir,
                                 new TestListing( new ListingResult( versionDir, new String[] { "artifact-1.0.jar",
                                     "artifact-1.0.pom" } ) ) );

        versionDir = ( (ConcreteResource) resource.getChild( "1.1/" ) );
        fixture.getTransport()
               .registerListing( versionDir,
                                 new TestListing( new ListingResult( versionDir, new String[] { "artifact-1.1.jar",
                                     "artifact-1.1.pom" } ) ) );

        return resource;
    }

    private StoreResource setupSnapshotDirWith2Snapshots()
        throws Exception
    {
        final RemoteRepository store = new RemoteRepository( "testrepo", "http://foo.bar" );
        stores.storeArtifactStore( store, summary );

        final String path = "org/group/artifact/1.0-SNAPSHOT";

        final KeyedLocation location = LocationUtils.toLocation( store );
        final StoreResource resource = new StoreResource( location, path );

        final TestListing listing =
            new TestListing( new ListingResult( resource, new String[] { "artifact-1.0-20140828.221400-1.pom",
                "artifact-1.0-20140828.221400-1.jar", "artifact-1.0-20140828.225800-1.pom",
                "artifact-1.0-20140828.225800-1.jar", } ) );

        fixture.getTransport()
               .registerListing( resource, listing );

        return resource;
    }

}
