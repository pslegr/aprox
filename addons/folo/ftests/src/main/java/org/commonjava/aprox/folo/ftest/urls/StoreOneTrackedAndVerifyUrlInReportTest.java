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
package org.commonjava.aprox.folo.ftest.urls;

import static org.commonjava.aprox.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Set;

import org.commonjava.aprox.folo.dto.TrackedContentDTO;
import org.commonjava.aprox.folo.dto.TrackedContentEntryDTO;
import org.junit.Test;

public class StoreOneTrackedAndVerifyUrlInReportTest
    extends AbstractFoloUrlsTest
{

    @Test
    public void storeOneFileAndVerifyItInParentDirectoryListing()
        throws Exception
    {
        final byte[] data = "this is a test".getBytes();
        final ByteArrayInputStream stream = new ByteArrayInputStream( data );
        final String root = "/path/to/";
        final String path = root + "foo.txt";

        final String trackingId = "tracker";

        content.store( trackingId, hosted, STORE, path, stream );

        final TrackedContentDTO report = admin.getTrackingReport( trackingId, hosted, STORE );

        final Set<TrackedContentEntryDTO> uploads = report.getUploads();
        for ( final TrackedContentEntryDTO upload : uploads )
        {
            final String uploadPath = upload.getPath();
            final String localUrl = client.content()
                                          .contentUrl( hosted, STORE, uploadPath );

            assertThat( "Incorrect local URL for upload: '" + uploadPath + "'", upload.getLocalUrl(),
                        equalTo( localUrl ) );
        }
    }

}
