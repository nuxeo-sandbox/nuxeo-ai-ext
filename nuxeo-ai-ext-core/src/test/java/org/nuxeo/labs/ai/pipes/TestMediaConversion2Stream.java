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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ai.pipes.types.BlobTextFromDocument;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.api.PictureView;
import org.nuxeo.ecm.platform.picture.api.PictureViewImpl;
import org.nuxeo.ecm.platform.picture.api.adapters.MultiviewPicture;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.video.TranscodedVideo;
import org.nuxeo.ecm.platform.video.VideoInfo;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.nuxeo.ai.pipes.services.JacksonUtil.fromRecord;
import static org.nuxeo.ecm.platform.video.VideoConstants.TRANSCODED_VIDEOS_PROPERTY;

@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class})
@Deploy({
        "org.nuxeo.labs.ai.nuxeo-ai-ext-core",
        "org.nuxeo.ai.ai-core",
        "org.nuxeo.ai.aws.aws-core",
        "org.nuxeo.ecm.platform.picture.core",
        "org.nuxeo.ecm.platform.tag",
        "org.nuxeo.ecm.platform.video"
})
public class TestMediaConversion2Stream {

    @Inject
    CoreSession session;

    @Test
    public void TestWithPicture() {
        DocumentModel doc = session.createDocumentModel("/", "Picture", "Picture");
        doc = session.createDocument(doc);

        PictureView pictureView = new PictureViewImpl();
        pictureView.setTitle("testRendition");
        pictureView.setImageInfo(new ImageInfo());
        Blob blob = new ManagedFileBlob(FileUtils.getResourceFileFromContext("files/frame.png"), "image/png");
        pictureView.setBlob(blob);

        MultiviewPicture multiviewPicture = doc.getAdapter(MultiviewPicture.class);
        multiviewPicture.addView(pictureView);


        Map<String, String> options = new HashMap<>();
        options.put("blobPropertiesType", "img");
        options.put("blobProperties", "testRendition");

        MediaConversion2Stream filter = new MediaConversion2Stream();
        filter.init(options);

        EventContextImpl evctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = evctx.newEvent("myDocEvent");
        event.setInline(true);

        Collection<Record> records = filter.apply(event);

        Assert.assertEquals(1,records.size());

        BlobTextFromDocument blobTextFromDoc = fromRecord(records.iterator().next(), BlobTextFromDocument.class);

        Assert.assertEquals(1,blobTextFromDoc.getBlobs().size());

        Blob deserializedBlob = blobTextFromDoc.getBlobs().get("testRendition");
        Assert.assertNotNull(deserializedBlob);
        Assert.assertEquals(blob.getMimeType(),deserializedBlob.getMimeType());
        Assert.assertEquals(blob.getLength(),deserializedBlob.getLength());

    }

    @Test
    public void TestWithVideo() {
        DocumentModel doc = session.createDocumentModel("/", "Video", "Video");
        doc = session.createDocument(doc);

        List<Map<String, Serializable>> transcodedVideos = new ArrayList<>();
        Blob blob = new ManagedFileBlob(FileUtils.getResourceFileFromContext("files/TourEiffel.mp4"), "video/mp4");
        TranscodedVideo transcodedVideo = TranscodedVideo.fromBlobAndInfo("MP4",blob, VideoInfo.EMPTY_INFO);
        transcodedVideos.add(transcodedVideo.toMap());

        doc.setPropertyValue(TRANSCODED_VIDEOS_PROPERTY, (Serializable) transcodedVideos);

        Map<String, String> options = new HashMap<>();
        options.put("blobPropertiesType", "video");
        options.put("blobProperties", "MP4");

        MediaConversion2Stream filter = new MediaConversion2Stream();
        filter.init(options);

        EventContextImpl evctx = new DocumentEventContext(session, session.getPrincipal(), doc);
        Event event = evctx.newEvent("myDocEvent");
        event.setInline(true);

        Collection<Record> records = filter.apply(event);

        Assert.assertEquals(1,records.size());

        BlobTextFromDocument blobTextFromDoc = fromRecord(records.iterator().next(), BlobTextFromDocument.class);

        Assert.assertEquals(1,blobTextFromDoc.getBlobs().size());

        Blob deserializedBlob = blobTextFromDoc.getBlobs().get("MP4");
        Assert.assertNotNull(deserializedBlob);
        Assert.assertEquals(blob.getMimeType(),deserializedBlob.getMimeType());
        Assert.assertEquals(blob.getLength(),deserializedBlob.getLength());
    }

}
