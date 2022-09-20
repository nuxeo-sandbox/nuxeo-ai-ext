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

import org.nuxeo.ai.pipes.functions.PropertiesToStream;
import org.nuxeo.ai.pipes.types.BlobTextFromDocument;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.lib.stream.computation.Record;

import java.util.Collection;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.nuxeo.ai.pipes.services.JacksonUtil.toRecord;

public class MediaConversion2Stream extends PropertiesToStream {

    @Override
    protected Function<Event, Collection<Record>> setupTransformation() {
        Function<Event, Collection<BlobTextFromDocument>> func = new MediaDocEvent2Stream(blobProperties, textProperties, customProperties);
        return func.andThen(items -> items.stream().map(i -> toRecord(i.getKey(), i)).collect(toList()));
    }


}
