# nuxeo-ai-ext — Agent Guide

## Project

Nuxeo LTS 2025 plugin (Java 23, Maven) that extends `nuxeo-ai-aws` with:
- A custom AWS Transcribe enrichment provider (with language auto-detection)
- AI pipes transformers to select video renditions or picture views before enrichment
- An async post-commit listener that auto-translates VTT closed captions via AWS Translate
- An Automation scripting function for on-demand translation

Does NOT include any Web UI elements.

- **Parent**: `org.nuxeo:nuxeo-parent:2025.16`
- **GroupId**: `org.nuxeo.labs`
- **Version**: `2025.1.0-SNAPSHOT`
- **nuxeo-ai dependency**: `5.0.2-SNAPSHOT` (resolves from Nuxeo's private Nexus)

## Modules

| Module | Purpose |
|--------|---------|
| `nuxeo-ai-ext-core` | All Java code + OSGI-INF components; OSGi bundle |
| `nuxeo-ai-ext-package` | Nuxeo Marketplace package (assembly.xml + package.xml); no Java |

Key paths in the core module:

- Java sources: `nuxeo-ai-ext-core/src/main/java/org/nuxeo/labs/ai/`
- OSGI-INF: `nuxeo-ai-ext-core/src/main/resources/OSGI-INF/`
- Bundle manifest: `nuxeo-ai-ext-core/src/main/resources/META-INF/MANIFEST.MF`

## Build & Test Commands

```bash
# Full build (skip Docker, which is not needed locally)
mvn -B install -DskipDocker=true

# Build skipping tests
mvn clean install -DskipTests

# Run a single test class
mvn test -pl nuxeo-ai-ext-core -Dtest=TestMediaConversion2Stream
```

**Local builds require Nuxeo Nexus credentials** in `~/.m2/settings.xml` for server id `maven-private` (or `maven-public` / `maven-internal`). Without them, dependency resolution fails — `nuxeo-ai 5.0.2-SNAPSHOT` resolves from `maven-internal` only.

CI also requires `ffmpeg` and `exiftool` (installed via apt in workflows).

## Nuxeo-Specific Constraints

- **All dependencies must be `<scope>provided</scope>`** — everything is already on the Nuxeo server or provided by `nuxeo-ai-core`/`nuxeo-ai-aws` packages. The assembly explicitly excludes `provided` scope from the package ZIP.
- **Never bundle nuxeo-ai, AWS SDK, or nuxeo-platform JARs** — they are provided by the server and dependent marketplace packages.
- **NOT Spring**: No `@Autowired`, `@Component`, `@Service`. Use `Framework.getService(...)`.
- **Jakarta, not javax**: All imports use `jakarta.*` namespace.
- **JUnit 4 only**: `@RunWith(FeaturesRunner.class)` + `@Features(...)` + `@Deploy(...)`. No JUnit 5.
- **Log4j2 only**: `LogManager.getLogger(MyClass.class)`. No SLF4J, no `System.out.println`.
- **OSGi singleton**: `Bundle-SymbolicName: org.nuxeo.labs.ai.nuxeo-ai-ext-core;singleton=true` — only one instance per OSGi framework.
- **`nuxeo.skip.enforcer=true` is intentional**: Sandbox plugins skip enforcer to avoid snapshot version convergence failures. Do not remove.
- **`<require>` ordering matters**: `enrichment-provider-contrib.xml` requires `org.nuxeo.ai.transcribe.TranscribeService`; `listener-contrib.xml` requires `org.nuxeo.ai.rekognition.listener`. If `nuxeo-ai-aws` is not installed, this bundle fails to deploy at runtime.

## Java Packages

| Package | Key Classes | Role |
|---------|-------------|------|
| `org.nuxeo.labs.ai.enricher` | `TranscribeEnrichmentProviderExt` | Extends `TranscribeEnrichmentProvider`; registers as `aws.transcribeExt`; calls AWS Transcribe with language auto-identification, polls until done (up to 2h) |
| `org.nuxeo.labs.ai.pipes` | `MediaConversion2Stream`, `MediaDocEvent2Stream` | Extends nuxeo-ai-pipes; selects a named transcoded rendition (`VideoDocument`) or picture view (`MultiviewPicture`) and pushes its blob to the AI stream |
| `org.nuxeo.labs.ai.translate` | `ExtendedDocumentTranscribed`, `TranslateAutomationFunctions` | Async post-commit listener that translates VTT captions via AWS Translate; Automation scripting function `Translate.translate(str, src, dest)` |

### Key Gotchas

- **`TranscribeEnrichmentProviderExt.awaitJob` is blocking**: polls AWS in a tight loop (`Thread.sleep(5s)`) for up to 2 hours. Runs inside a Nuxeo Work (async thread) — never call from a synchronous context.
- **Language code truncation**: `srcLang.substring(0, 2)` normalizes `en-US` → `en` for AWS Translate. Intentional.
- **`ExtendedDocumentTranscribed` overrides, not replaces**: extends `DocumentTranscribed` from nuxeo-ai; both listeners run on `ENRICHMENT_MODIFIED`. Priority `999` ensures this runs after the base listener. Only activates for `PROVIDER_KIND = /tagging/transcribe`.
- **Translation languages at runtime**: read from `nuxeo.conf` via `Framework.getProperty("closed.caption.ai.translation.languages")`, e.g. `en,fr,es,ja`. Not injected at startup.

## Testing Patterns

```java
@RunWith(FeaturesRunner.class)
@Features({PlatformFeature.class})
@Deploy("org.nuxeo.labs.ai.nuxeo-ai-ext-core")  // symbolic name from MANIFEST.MF, not artifact ID
public class TestMediaConversion2Stream {

    @Inject
    protected CoreSession session;

    @Test
    public void testSomething() throws Exception { ... }
}
```

- `@Deploy("bundle:OSGI-INF/test-contrib.xml")` — deploy test-only XML contributions
- Test binary assets (`TourEiffel.mp4`, `frame.png`) live in `src/test/resources/files/`, accessed via `FileUtils.getResourceFileFromContext(...)`
- `ManagedFileBlob` — test helper that implements both `FileBlob` and `ManagedBlob` to simulate stored blobs
- AWS service calls are not mocked — tests verify registration only, not actual AWS calls

## Adding New Code

### New Enrichment Provider or Service Extension

1. Extend the relevant nuxeo-ai base class in `org.nuxeo.labs.ai.enricher`
2. Register in a new `OSGI-INF/<name>-contrib.xml` with `<require>` on the service it depends on
3. Add the XML filename to `Nuxeo-Component:` in `MANIFEST.MF`
4. All dependencies `provided` scope; no version tags in child poms (managed by `nuxeo-parent`)

### New Automation Function

1. Add a `@Function` method on a class annotated with `@Context` in `org.nuxeo.labs.ai.translate`
2. Register the class in the `org.nuxeo.automation.scripting` extension point

## Dependencies

No version tags in child pom `<dependency>` blocks — versions come from `nuxeo-parent`. Exception: `nuxeo.ai.version` property for nuxeo-ai artifacts.

## CI/CD

All workflows are **`workflow_dispatch` only** (no push/PR triggers):

| Workflow | What it does |
|----------|-------------|
| `maven_build.yml` | Build + test with `-DskipDocker=true` |
| `publish_snapshot.yml` | Build then upload ZIP to Nuxeo Connect (org: `nuxeo-presales-us`) |
| `release_and_publish.yml` | `mvn release:prepare`, then upload released ZIP to Connect |
| `update_parent_nuxeo_version.yml` | `mvn versions:update-parent`, commit + push to main |

The Marketplace ZIP path uses GitHub Actions variable `MP_TARGET_PATH`. Upload uses secrets `CONNECT_USER` / `CONNECT_TOKEN`.

## Release Process

> [!WARNING]
> Check the repository is clean before starting. Alert and stop if there are uncommitted changes.

> [!IMPORTANT]
> The version numbers below are examples only. Always read the actual current version from the POM before running any command, and derive the release and next snapshot versions from it (e.g. `2025.1.0-SNAPSHOT` → release `2025.1.0` → next snapshot `2025.2.0-SNAPSHOT`).

1. Remove `-SNAPSHOT` from the current version: `mvn versions:set -DnewVersion=<current-version-without-SNAPSHOT> -DgenerateBackupPoms=false`
2. Build: `mvn clean install -DskipTests`
3. Copy `nuxeo-ai-ext-package/target/nuxeo-ai-ext-package-<version>.zip` to `~/Downloads/`
4. Bump to next snapshot (increment minor, reset incremental to 0, add `-SNAPSHOT`): `mvn versions:set -DnewVersion=<next-version>-SNAPSHOT -DgenerateBackupPoms=false`
5. Verify: `mvn clean install -DskipTests`
6. Commit and push:
   ```bash
   git add .
   git commit -m "Post <version> release"
   git push
   ```

> [!NOTE]
> No git tag or GitHub release is created. The ZIP copied to `~/Downloads` is the deliverable.

## Code Style

- 4-space indent, no tabs, K&R braces, ~120 char lines
- Modern Java: `var`, records, text blocks, `String.formatted()`, pattern matching `instanceof`
- No wildcard imports. Import order: static, `java.*`, `jakarta.*`, `org.*`, `com.*`
- Always use braces for `if`/`else` (even single-line)
- Logging: parameterized `log.debug("Processing: {}", docId)`
- Apache 2.0 license header on all new files (with current year and `Contributors:` section)
- Javadoc `@since 2025.16` on new public API; no `@author` tag
- No checkstyle, PMD, or formatter configured — follow existing file style

## Local References

If Nuxeo LTS 2025 source or other plugin examples are available locally, prefer local files over network. Ask the user:

> "Do you have the Nuxeo LTS 2025 source cloned locally? If so, what is the path? Otherwise I'll use GitHub."

### Fallback URLs

- Nuxeo LTS 2025: https://github.com/nuxeo/nuxeo-lts (branch `2025`)
- nuxeo-ai: `modules/platform/nuxeo-ai/` in the above
- Plugin example: https://github.com/nuxeo-sandbox/nuxeo-labs-dynamic-fields
