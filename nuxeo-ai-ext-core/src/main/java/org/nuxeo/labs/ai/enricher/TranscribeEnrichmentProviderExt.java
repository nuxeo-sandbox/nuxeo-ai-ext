package org.nuxeo.labs.ai.enricher;

import com.amazonaws.services.transcribe.model.GetTranscriptionJobRequest;
import com.amazonaws.services.transcribe.model.GetTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.StartTranscriptionJobResult;
import com.amazonaws.services.transcribe.model.TranscriptionJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.nuxeo.ai.enrichment.EnrichmentDescriptor;
import org.nuxeo.ai.enrichment.EnrichmentMetadata;
import org.nuxeo.ai.enrichment.EnrichmentUtils;
import org.nuxeo.ai.enrichment.async.TranscribeEnrichmentProvider;
import org.nuxeo.ai.metadata.AIMetadata;
import org.nuxeo.ai.metadata.LabelSuggestion;
import org.nuxeo.ai.pipes.types.BlobTextFromDocument;
import org.nuxeo.ai.transcribe.AudioTranscription;
import org.nuxeo.ai.transcribe.TranscribeService;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.amazonaws.services.transcribe.model.TranscriptionJobStatus.FAILED;
import static com.amazonaws.services.transcribe.model.TranscriptionJobStatus.IN_PROGRESS;

public class TranscribeEnrichmentProviderExt extends TranscribeEnrichmentProvider {

    public static final String PROVIDER_NAME = "aws.transcribeExt";

    private static final long TIMEOUT = 1000 * 60 * 60 * 2; // 2h

    private static final long WAIT_TIME = 1000 * 5; // 5s

    private static final Logger log = LogManager.getLogger(TranscribeEnrichmentProvider.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected String[] languages;

    @Override
    public void init(EnrichmentDescriptor descriptor) {
        super.init(descriptor);
        languages = descriptor.options.getOrDefault(LANGUAGES_OPTION, "").split(",");
    }

    @Override
    public Collection<EnrichmentMetadata> enrich(BlobTextFromDocument blobTextFromDocument) {
        String docId = blobTextFromDocument.getId();
        Optional<ManagedBlob> blobOptional = blobTextFromDocument.getBlobs().values().stream().findFirst();

        if (blobOptional.isEmpty()) {
            throw new NuxeoException("No Audio File to transcribe; doc id = " + blobTextFromDocument.getId());
        }

        TranscribeService ts = Framework.getService(TranscribeService.class);
        StartTranscriptionJobResult result = ts.requestTranscription(blobOptional.get(), languages);
        TranscriptionJob job = result.getTranscriptionJob();
        job = awaitJob(docId, ts, job);
        if (FAILED.name().equals(job.getTranscriptionJobStatus())) {
            throw new NuxeoException("Transcribe job failed with reason: " + job.getFailureReason() + "; Job: "
                    + job.getTranscriptionJobName() + " Document Id: " + docId);
        }

        String json = getResponse(docId, job);
        AudioTranscription transcription = getAudioTranscription(docId, json);
        List<AIMetadata.Label> labels = ts.asLabels(transcription);

        List<LabelSuggestion> labelSuggestions = Collections.singletonList(
                new LabelSuggestion(UNSET + PROVIDER_NAME, labels));
        String rawKey = EnrichmentUtils.saveRawBlob(Blobs.createJSONBlob(json), "default");
        EnrichmentMetadata metadata = new EnrichmentMetadata.Builder(PROVIDER_KIND, PROVIDER_NAME, blobTextFromDocument).withLabels(
                labelSuggestions).withRawKey(rawKey).build();

        return Collections.singletonList(metadata);
    }

    private AudioTranscription getAudioTranscription(String docId, String json) {
        AudioTranscription transcription;
        try {
            transcription = OBJECT_MAPPER.readValue(json, AudioTranscription.class);
        } catch (IOException e) {
            log.error("Could not process JSON response {}", json, e);
            throw new NuxeoException("Could not read `AudioTranscription` for Document Id: " + docId);
        }

        return transcription;
    }

    private String getResponse(String docId, TranscriptionJob job) {
        String transcriptUri = job.getTranscript().getTranscriptFileUri();
        HttpGet req = new HttpGet(transcriptUri);

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse resp = httpClient.execute(req)) {
            HttpEntity entity = resp.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e);
            throw new NuxeoException(
                    "Could not retrieve result for Job " + job.getTranscriptionJobName() + " Document Id: " + docId);
        }
    }

    @NotNull
    private TranscriptionJob awaitJob(String docId, TranscribeService ts, TranscriptionJob job) {
        long timeSpent = 0;
        String jobName = job.getTranscriptionJobName();
        GetTranscriptionJobRequest jobRequest = new GetTranscriptionJobRequest().withTranscriptionJobName(jobName);
        while (IN_PROGRESS.name().equals(job.getTranscriptionJobStatus())) {
            GetTranscriptionJobResult jobResult = ts.getClient().getTranscriptionJob(jobRequest);
            job = jobResult.getTranscriptionJob();
            if (timeSpent > TIMEOUT) {
                throw new NuxeoException("Work reached timeout; Job name: " + jobName + " Document Id: " + docId);
            }
            try {
                Thread.sleep(WAIT_TIME);
                timeSpent += WAIT_TIME;
            } catch (InterruptedException e) {
                log.error(e);
                throw new NuxeoException(
                        "Transcribe was interrupted; could not get results for Job: " + jobName + " Document Id: "
                                + docId, e);
            }
        }
        return job;
    }
}
