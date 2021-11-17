package org.nuxeo.labs.ai.translate;

import com.amazonaws.services.translate.model.TranslateTextResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ai.listeners.DocumentTranscribed;
import org.nuxeo.ai.metadata.Caption;
import org.nuxeo.ai.services.CaptionService;
import org.nuxeo.ai.transcribe.AudioTranscription;
import org.nuxeo.ai.translate.TranslateService;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.nuxeo.ai.AIConstants.ENRICHMENT_FACET;
import static org.nuxeo.ai.AIConstants.ENRICHMENT_ITEMS;
import static org.nuxeo.ai.AIConstants.ENRICHMENT_SCHEMA_NAME;
import static org.nuxeo.ai.enrichment.async.TranscribeEnrichmentProvider.PROVIDER_NAME;
import static org.nuxeo.ai.listeners.VideoAboutToChange.CAPTIONABLE_FACET;
import static org.nuxeo.ai.metadata.Caption.CAPTIONS_PROP;
import static org.nuxeo.ai.metadata.Caption.VTT_KEY_PROP;
import static org.nuxeo.ai.transcribe.AudioTranscription.Type.PRONUNCIATION;
import static org.nuxeo.ecm.core.event.impl.DocumentEventContext.COMMENT_PROPERTY_KEY;

public class ExtendedDocumentTranscribed extends DocumentTranscribed {

    public static final String CLOSED_CAPTION_AI_TRANSLATION_LANGUAGES = "closed.caption.ai.translation.languages";
    private static final Logger log = LogManager.getLogger(ExtendedDocumentTranscribed.class);

    @Override
    protected void handleEvent(Event event) {
        EventContext ctx = event.getContext();
        if (!(ctx instanceof DocumentEventContext)) {
            return;
        }
        DocumentEventContext docCtx = (DocumentEventContext) ctx;
        DocumentModel doc = docCtx.getSourceDocument();
        if (!doc.hasFacet(CAPTIONABLE_FACET) || !doc.hasFacet(ENRICHMENT_FACET)) {
            return;
        }

        if (!PROVIDER_NAME.equals(docCtx.getProperty(COMMENT_PROPERTY_KEY))) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> enrichments = (List<Map<String, Object>>) doc.getProperty(ENRICHMENT_SCHEMA_NAME,
                ENRICHMENT_ITEMS);
        List<Blob> raws = enrichments.stream()
                .filter(en -> PROVIDER_NAME.equals(en.getOrDefault("model", "none")))
                .map(en -> (Blob) en.get("raw"))
                .collect(Collectors.toList());

        if (raws.isEmpty()) {
            log.debug("Could not find RAW transcription for document id = " + doc.getId());
            return;
        }
        Blob json = raws.get(0);
        if (json == null) {
            return;
        }

        AudioTranscription at;
        try {
            at = OBJECT_MAPPER.readValue(json.getString(), AudioTranscription.class);
        } catch (IOException e) {
            log.error(e);
            return;
        }

        String srcLang = at.getResults().getLanguageCode();
        if (srcLang.length() > 2) {
            srcLang = srcLang.substring(0, 2);
        }

        List<Element> elements = at.getResults().getItems().stream().map(item -> {
            float st = 0L;
            float et = 0L;
            AudioTranscription.Type type = item.getType();
            if (PRONUNCIATION.equals(type)) {
                st = Float.parseFloat(item.getStartTime());
                et = Float.parseFloat(item.getEndTime());
            }

            String content = item.getContent();
            return new Element((long) (st * 1000), (long) (et * 1000), type, content);
        }).collect(Collectors.toList());

        List<Caption> captions = buildCaptions(elements);

        List<String> captionsText = captions.stream().map(
                caption -> caption.getLines().get(0)).collect(Collectors.toList()
        );

        TranslateService translateService = Framework.getService(TranslateService.class);
        CaptionService cs = Framework.getService(CaptionService.class);

        List<String> languages = Arrays.asList(Framework.getProperty(CLOSED_CAPTION_AI_TRANSLATION_LANGUAGES, "").split(","));

        List<Map<String, Serializable>> allCaptions = new ArrayList<>();
        Map<String, Serializable> original = new HashMap<>();
        original.put(LANGUAGE_KEY, srcLang);
        original.put(VTT_KEY_PROP, (Serializable) cs.write(captions));
        allCaptions.add(original);

        for (String destLang : languages) {
            if (destLang.equals(srcLang)) {
                continue;
            }

            TranslateTextResult result = translateService.translateText(String.join("\n", captionsText), srcLang, destLang);
            String text = result.getTranslatedText();
            List<String> lines = List.of(text.split("\n"));
            List<Caption> translatedCaptions = IntStream
                    .range(0, lines.size())
                    .mapToObj(i -> {
                        Caption srcCaption = captions.get(i);
                        return new Caption(srcCaption.getStart(), srcCaption.getEnd(), List.of(lines.get(i)));
                    })
                    .collect(Collectors.toList());

            Blob translatedCaptionsFile = cs.write(translatedCaptions);

            Map<String, Serializable> translation = new HashMap<>();
            translation.put(LANGUAGE_KEY, destLang);
            translation.put(VTT_KEY_PROP, (Serializable) translatedCaptionsFile);

            allCaptions.add(translation);
        }

        doc.setPropertyValue(CAPTIONS_PROP, (Serializable) allCaptions);
        doc.getCoreSession().saveDocument(doc);
    }

}
