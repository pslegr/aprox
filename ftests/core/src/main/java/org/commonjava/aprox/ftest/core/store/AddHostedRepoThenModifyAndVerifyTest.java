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
package org.commonjava.aprox.ftest.core.store;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class AddHostedRepoThenModifyAndVerifyTest
    extends AbstractStoreManagementTest
{

    @Test
    public void addAndModifyHostedRepositoryThenRetrieveIt()
        throws Exception
    {
        final HostedRepository repo = new HostedRepository( newName() );
        client.stores()
              .create( repo, name.getMethodName(), HostedRepository.class );

        repo.setAllowReleases( !repo.isAllowReleases() );

        assertThat( client.stores()
                          .update( repo, name.getMethodName() ), equalTo( true ) );

        final HostedRepository result = client.stores()
                                              .load( StoreType.hosted, repo.getName(), HostedRepository.class );

        assertThat( result.getName(), equalTo( repo.getName() ) );
        assertThat( result.equals( repo ), equalTo( true ) );
        assertThat( result.isAllowReleases(), equalTo( repo.isAllowReleases() ) );
    }

}
