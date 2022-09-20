# Description

This repository contains a plugin that includes additional AWS powered AI capabilities for the Nuxeo Platform

# How to build

```
git clone https://github.com/nuxeo-sandbox/nuxeo-ai-ext
cd nuxeo-ai-ext
mvn clean install
```

# Features

- Automated AI based translation of Closed Caption file
- AI Based Translate function for Automation Script and Chains
- A filter for AI pipelines to select a given image or video rendition

# How to Use
## Automated AI based translation of Closed Caption file
- Set the destination language for CC file translation in nuxeo.conf

```
closed.caption.ai.translation.languages=en,fr,es,ja
```

The list of supported languages is available [here](https://docs.aws.amazon.com/translate/latest/dg/what-is.html#what-is-languages)

## Translate function for Automation Script and Chains

```
Translate.translate(aString, srcLang, destLang)
```

## Use a given picture or video conversion in pipelines

For Videos

```xml
<extension point="pipes" target="org.nuxeo.ai.Pipeline">
  <pipe id="pipe.video" enabled="${nuxeo.ai.video.enabled:=}" postCommit="true">
    ...
    <transformer class="org.nuxeo.labs.ai.pipes.MediaConversion2Stream">
      <option name="blobProperties">MP4 1080p</option>
      <option name="blobPropertiesType">video</option>
    </transformer>
  </pipe>
</extension>
```

For Images

```xml
<extension point="pipes" target="org.nuxeo.ai.Pipeline">
  <pipe id="pipe.images" enabled="${nuxeo.ai.images.enabled}" postCommit="true">
    ...
    <transformer class="org.nuxeo.labs.ai.pipes.MediaConversion2Stream">
      <option name="blobProperties">FullHD</option>
      <option name="blobPropertiesType">img</option>
    </transformer>
  </pipe>
</extension>
```

# Known limitations
None

# Support

**These features are not part of the Nuxeo Production platform.**

These solutions are provided for inspiration, and we encourage customers to use them as code samples and learning resources.

This is a moving project (no API maintenance, no deprecation process, etc.) If any of these solutions are found to be useful for the Nuxeo Platform in general, they will be integrated directly into platform, not maintained here.

# Nuxeo Marketplace
This plugin is published on the [marketplace](https://connect.nuxeo.com/nuxeo/site/marketplace/package/nuxeo-ai-ext)

# License

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

# About Nuxeo

Nuxeo Platform is an open source Content Services platform, written in Java. Data can be stored in both SQL & NoSQL databases.

The development of the Nuxeo Platform is mostly done by Nuxeo employees with an open development model.

The source code, documentation, roadmap, issue tracker, testing, benchmarks are all public.

Typically, Nuxeo users build different types of information management solutions for [document management](https://www.nuxeo.com/solutions/document-management/), [case management](https://www.nuxeo.com/solutions/case-management/), and [digital asset management](https://www.nuxeo.com/solutions/dam-digital-asset-management/), use cases. It uses schema-flexible metadata & content models that allows content to be repurposed to fulfill future use cases.

More information is available at [www.nuxeo.com](https://www.nuxeo.com)
