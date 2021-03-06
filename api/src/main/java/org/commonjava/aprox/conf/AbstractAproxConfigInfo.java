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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.ConfigUtils;

/**
 * Abstract implementation of {@link AproxConfigClassInfo} meant to make implementation as simple as providing a section name and configuration-info
 * registration class via the super() constructor.
 */
public abstract class AbstractAproxConfigInfo
    implements AproxConfigClassInfo
{

    private final Class<?> type;

    private final String sectionName;

    AbstractAproxConfigInfo()
    {
        type = Object.class;
        sectionName = null;
    }

    protected AbstractAproxConfigInfo( final Class<?> type )
    {
        this( type, null );
    }

    protected AbstractAproxConfigInfo( final Class<?> type, final String sectionName )
    {
        this.type = type;
        this.sectionName = sectionName;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getConfigurationClass()
     */
    @Override
    public Class<?> getConfigurationClass()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getSectionName()
     */
    @Override
    public String getSectionName()
    {
        return sectionName;
    }

    @Override
    public String toString()
    {
        final String key = sectionName == null ? ConfigUtils.getSectionName( type ) : sectionName;
        return key + " [" + type.getName() + "]";
    }

}
