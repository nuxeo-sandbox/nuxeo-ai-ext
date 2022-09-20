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

package org.nuxeo.labs.ai.pipes;

import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.blob.ManagedBlob;

import java.io.File;

public class ManagedFileBlob extends FileBlob implements ManagedBlob {

    public ManagedFileBlob(File file, String mimeType) {
        super(file, mimeType);
    }

    @Override
    public String getProviderId() {
        return "default";
    }

    @Override
    public String getKey() {
        return getDigest();
    }
}
