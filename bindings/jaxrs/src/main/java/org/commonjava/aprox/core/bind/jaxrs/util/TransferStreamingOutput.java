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
package org.commonjava.aprox.core.bind.jaxrs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.commonjava.maven.galley.model.Transfer;

public class TransferStreamingOutput
    implements StreamingOutput
{

    private final Transfer item;

    public TransferStreamingOutput( final Transfer item )
    {
        this.item = item;
    }

    @Override
    public void write( final OutputStream out )
        throws IOException, WebApplicationException
    {
        InputStream in = null;
        try
        {
            in = item.openInputStream();
            IOUtils.copy( in, out );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
    }

}
