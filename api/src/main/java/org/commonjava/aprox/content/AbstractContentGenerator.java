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
package org.commonjava.aprox.content;

import java.util.List;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.maven.galley.model.Transfer;

/**
 * Convenience abstract class for {@link ContentGenerator} implementations to extend. This allows content generators to only implement the methods
 * they care about.
 */
public abstract class AbstractContentGenerator
    implements ContentGenerator
{

    protected AbstractContentGenerator()
    {
    }

    @Override
    public void handleContentStorage( final ArtifactStore store, final String path, final Transfer result )
        throws AproxWorkflowException
    {
    }

    @Override
    public void handleContentDeletion( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
    }

    @Override
    public Transfer generateFileContent( final ArtifactStore store, final String path )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateDirectoryContent( final ArtifactStore store, final String path,
                                                         final List<StoreResource> existing )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public Transfer generateGroupFileContent( final Group group, final List<ArtifactStore> members, final String path )
        throws AproxWorkflowException
    {
        return null;
    }

    @Override
    public List<StoreResource> generateGroupDirectoryContent( final Group group, final List<ArtifactStore> members,
                                                              final String path )
        throws AproxWorkflowException
    {
        return null;
    }

}
