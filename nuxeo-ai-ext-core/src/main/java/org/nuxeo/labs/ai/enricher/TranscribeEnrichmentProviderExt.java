/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

import org.nuxeo.ai.enrichment.EnrichmentDescriptor;
import org.nuxeo.ai.enrichment.async.TranscribeEnrichmentProvider;

/**
 * Extension of the standard AWS Transcribe enrichment provider that enables automatic language identification.
 * Registered as provider name {@code aws.transcribeExt}.
 *
 * @since 2025.16
 */
public class TranscribeEnrichmentProviderExt extends TranscribeEnrichmentProvider {

    public static final String PROVIDER_NAME = "aws.transcribeExt";

    @Override
    public void init(EnrichmentDescriptor descriptor) {
        super.init(descriptor);
        languages = descriptor.options.getOrDefault(LANGUAGES_OPTION, "").split(",");
    }
}
