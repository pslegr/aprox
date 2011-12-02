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
package org.commonjava.aprox.core.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.core.model.Group;
import org.commonjava.aprox.core.model.StoreType;
import org.commonjava.couch.model.DenormalizationException;
import org.junit.Test;

public class GroupTest
{

    @Test
    public void denormalizeCouchDocumentIdFromName()
        throws DenormalizationException
    {
        Group grp = new Group( "test" );
        grp.calculateDenormalizedFields();

        assertThat( grp.getCouchDocId(), equalTo( namespaceId( StoreType.group.name(), "test" ) ) );
    }

}