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
package org.commonjava.aprox.depgraph.dto;

import java.util.Set;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;

public class PathsDTO 
    extends WebOperationConfigDTO
{

    /** The target artifacts which we want collect paths to. */
    private Set<ProjectRef> targets;

    /**
     * @return the target artifacts which we want collect paths to
     */
    public Set<ProjectRef> getTargets()
    {
        return targets;
    }

    /**
     * @param targets
     *            the target artifacts which we want collect paths to
     */
    public void setTargets( final Set<ProjectRef> targets )
    {
        this.targets = targets;
    }

}
