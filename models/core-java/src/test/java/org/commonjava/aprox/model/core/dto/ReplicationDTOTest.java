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
package org.commonjava.aprox.model.core.dto;

import java.util.Arrays;

import org.commonjava.aprox.model.core.dto.ReplicationAction;
import org.commonjava.aprox.model.core.dto.ReplicationDTO;
import org.commonjava.aprox.model.core.dto.ReplicationAction.ActionType;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.junit.Test;

public class ReplicationDTOTest
{

    @Test
    public void serializeBasicReplicationRequest()
        throws Exception
    {
        final ReplicationDTO dto = new ReplicationDTO();
        dto.setApiUrl( "http://foo.com/api/1.0" );
        dto.setActions( Arrays.asList( new ReplicationAction( ActionType.PROXY ) ) );

        final String json = new AproxObjectMapper( true )
                                              .writeValueAsString( dto );
        System.out.println( json );
    }

}
