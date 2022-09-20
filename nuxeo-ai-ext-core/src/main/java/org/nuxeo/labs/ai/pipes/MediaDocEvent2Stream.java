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

import org.nuxeo.ai.pipes.events.DocEventToStream;
import org.nuxeo.ai.pipes.types.BlobTextFromDocument;
import org.nuxeo.ai.sdk.objects.PropertyType;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.video.VideoDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MediaDocEvent2Stream extends DocEventToStream {

    public MediaDocEvent2Stream(List<PropertyType> blobProperties, List<String> textProperties, List<String> customProperties) {
        super(blobProperties, textProperties, customProperties);
    }

    @Override
    public Collection<BlobTextFromDocument> docSerialize(DocumentModel doc) {
        List<BlobTextFromDocument> items = new ArrayList<>();
        blobProperties.forEach(property -> {
            Blob blob = null;
            if ("video".equals(property.getType())) {
                VideoDocument videoDocument = doc.getAdapter(VideoDocument.class,true);
                blob = videoDocument.getTranscodedVideo(property.getName()).getBlob();
            } else if ("img".equals(property.getType())) {
                MultiviewPicture multiviewPicture = doc.getAdapter(MultiviewPicture.class, true);
                blob = multiviewPicture.getView(property.getName()).getBlob();
            }
            if (blob instanceof ManagedBlob) {
                BlobTextFromDocument blobTextFromDoc = getBlobText(doc);
                blobTextFromDoc.addBlob(property.getName(), property.getType(), (ManagedBlob) blob);
                items.add(blobTextFromDoc);
            }
        });
        return items;
    }
}
