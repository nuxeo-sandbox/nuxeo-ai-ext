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

package org.nuxeo.labs.ai.translate;

import com.amazonaws.services.translate.model.TranslateTextResult;
import org.nuxeo.ai.translate.TranslateService;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.runtime.api.Framework;

public class TranslateAutomationFunctions implements ContextHelper {

    public TranslateAutomationFunctions() {}

    public String translate(String src, String srcLang, String destLang) {
        TranslateService translateService = Framework.getService(TranslateService.class);
        TranslateTextResult result = translateService.translateText(src, srcLang, destLang);
        return result.getTranslatedText();
    }

}
