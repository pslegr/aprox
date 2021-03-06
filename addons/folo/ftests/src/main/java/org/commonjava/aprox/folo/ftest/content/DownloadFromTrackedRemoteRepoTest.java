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
package org.commonjava.aprox.folo.ftest.content;

import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.junit.Test;

public class DownloadFromTrackedRemoteRepoTest
    extends AbstractFoloContentManagementTest
{

    @Test
    public void downloadFileFromRemoteRepository()
        throws Exception
    {
        final String trackingId = newName();
        
        final InputStream result = client.module( AproxFoloContentClientModule.class )
                                         .get( trackingId, remote, CENTRAL, "org/commonjava/commonjava/2/commonjava-2.pom" );
        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        result.close();
        assertThat( pom.contains( "<groupId>org.commonjava</groupId>" ), equalTo( true ) );
    }

}
