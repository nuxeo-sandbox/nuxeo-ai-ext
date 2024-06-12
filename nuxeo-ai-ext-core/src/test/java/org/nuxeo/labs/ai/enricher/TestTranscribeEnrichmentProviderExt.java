/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Michael Vachette
 */

package org.nuxeo.labs.ai.enricher;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.services.AIComponent;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.labs.ai.enricher.TranscribeEnrichmentProviderExt.PROVIDER_NAME;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy({
        "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.default.config",
        "org.nuxeo.labs.ai.nuxeo-ai-ext-core",
        "org.nuxeo.ai.ai-core",
        "org.nuxeo.ai.nuxeo-ai-pipes",
        "org.nuxeo.ai.aws.aws-core",
})
public class TestTranscribeEnrichmentProviderExt {

    @Inject
    public AIComponent aiComponent;

    @Test
    public void testEnrichmentProviderIsRegistered() {
        Assert.assertNotNull(aiComponent.getEnrichmentProvider(PROVIDER_NAME));
    }

    @Test
    public void testEnrichmentProviderSupportsMimeType() {
        TranscribeEnrichmentProviderExt ep = (TranscribeEnrichmentProviderExt) aiComponent.getEnrichmentProvider(PROVIDER_NAME);
        Assert.assertTrue(ep.supportsMimeType("audio/mpeg"));
    }
}
