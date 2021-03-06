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
package org.commonjava.aprox.autoprox.ftest.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.autoprox.client.AutoProxCatalogModule;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.junit.Assert;

public abstract class AbstractAutoproxCatalogTest
    extends AbstractAproxFunctionalTest
{

    protected AutoProxCatalogModule module;

    protected RuleDTO getRule( final String named, final String ruleScriptResource )
        throws IOException
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( ruleScriptResource );
        if ( resource == null )
        {
            Assert.fail( "Cannot find classpath resource: " + ruleScriptResource );
        }

        final String spec = IOUtils.toString( resource );

        return new RuleDTO( named, spec );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        module = new AutoProxCatalogModule();
        return Arrays.<AproxClientModule> asList( module );
    }

}
