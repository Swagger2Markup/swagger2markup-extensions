/*
 * Copyright 2016 Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.swagger2markup.extensions;

import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.Swagger2MarkupProperties;
import io.github.swagger2markup.markup.builder.MarkupLanguage;
import io.github.swagger2markup.model.PathOperation;
import io.github.swagger2markup.spi.PathsDocumentExtension;
import io.github.swagger2markup.utils.IOUtils;
import io.github.swagger2markup.utils.URIUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Append Spring Rest docs generated snippets to Operations content.
 */
public final class SpringRestDocsExtension extends PathsDocumentExtension {

    private static final Logger logger = LoggerFactory.getLogger(SpringRestDocsExtension.class);

    private static final Map<String, String> DEFAULT_SNIPPETS = new LinkedHashMap<String, String>() {{
        put("http-request", "Example HTTP request");
        put("http-response", "Example HTTP response");
        put("curl-request", "Example Curl request");
    }};
    private static final String PROPERTY_SNIPPET_BASE_URI = "snippetBaseUri";
    private static final String DEFAULT_EXTENSION_ID = "springRestDocs";
    private static final String PROPERTY_DEFAULT_SNIPPETS = "defaultSnippets";
    private static final String PROPERTY_MARKUP_LANGUAGE = "markupLanguage";

    protected URI snippetBaseUri;
    protected Map<String, String> snippets = new LinkedHashMap<>();
    private String extensionId = DEFAULT_EXTENSION_ID;
    private MarkupLanguage extensionMarkupLanguage = MarkupLanguage.ASCIIDOC;

    /**
     * Instantiate extension with the default extension id.
     * @param snippetBaseUri base URI where the snippets are stored
     * @param extensionMarkupLanguage the MarkupLanguage of the snippets content
     */
    public SpringRestDocsExtension(URI snippetBaseUri, MarkupLanguage extensionMarkupLanguage) {
        this(null, snippetBaseUri, extensionMarkupLanguage);
    }

    /**
     * Instantiate extension
     * @param extensionId the unique ID of the extension
     * @param snippetBaseUri base URI where the snippets are stored
     * @param extensionMarkupLanguage the MarkupLanguage of the snippets content
     */
    public SpringRestDocsExtension(String extensionId, URI snippetBaseUri, MarkupLanguage extensionMarkupLanguage) {
        super();
        Validate.notNull(extensionId);
        Validate.notNull(snippetBaseUri);
        if(StringUtils.isNoneBlank(extensionId)) {
            this.extensionId = extensionId;
        }
        this.snippetBaseUri = snippetBaseUri;
        this.extensionMarkupLanguage = extensionMarkupLanguage;
    }

    public SpringRestDocsExtension() {
        super();
    }

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        Swagger2MarkupProperties extensionsProperties = globalContext.getConfig().getExtensionsProperties();
        Optional<URI> snippetBaseUriProperty = extensionsProperties.getURI(extensionId + "." + PROPERTY_SNIPPET_BASE_URI);
        boolean withDefaultSnippets = extensionsProperties.getBoolean(extensionId + "." + PROPERTY_DEFAULT_SNIPPETS, true);
        if(withDefaultSnippets){
            withDefaultSnippets();
        }
        if (snippetBaseUriProperty.isPresent()) {
            snippetBaseUri = URIUtils.convertUriWithoutSchemeToFileScheme(snippetBaseUriProperty.get());
        }
        else{
            if (globalContext.getSwaggerLocation() == null) {
                if (logger.isWarnEnabled())
                    logger.warn("Disable SpringRestDocsExtension > Can't set default snippetBaseUri from swaggerLocation. You have to explicitly configure the snippetBaseUri.");
            } else {
                snippetBaseUri = URIUtils.uriParent(globalContext.getSwaggerLocation());
            }
        }

        Optional<MarkupLanguage> extensionMarkupLanguageProperty = extensionsProperties.getMarkupLanguage(extensionId + "." + PROPERTY_MARKUP_LANGUAGE);
        if (extensionMarkupLanguageProperty.isPresent()) {
            extensionMarkupLanguage = extensionMarkupLanguageProperty.get();
        }
    }

    /**
     * Add SpringRestDocs default snippets to list
     * @return this instance
     */
    public SpringRestDocsExtension withDefaultSnippets() {
        snippets.putAll(DEFAULT_SNIPPETS);
        return this;
    }

    /**
     * Add an explicit list of snippets to display.
     * @param snippets snippets to add. key is snippet name (without extension, e.g.: 'http-request'), value is a custom section title for the snippet.
     * @return this instance
     */
    public SpringRestDocsExtension withExplicitSnippets(Map<String, String> snippets) {
        this.snippets.putAll(snippets);
        return this;
    }

    /**
     * Builds snippet URI for the given {@code operation} and {@code snippetName}.<br>
     * Default implementation use {@code <snippetBaseUri>/<normalizeName(<operation id>)>/<snippetName>.<markup ext>}.<br>
     * You can override this method to configure your own folder normalization.
     *
     * @param context current context
     * @param operation current operation
     * @return subdirectory normalized name
     */
    public URI operationSnippetUri(Context context, PathOperation operation, String snippetName) {
        return snippetBaseUri.resolve(IOUtils.normalizeName(operation.getId()) + "/").resolve(context.getMarkupDocBuilder().addFileExtension(snippetName));
    }

    @Override
    public void apply(Context context) {
        Validate.notNull(context);

        switch (context.getPosition()) {
            case OPERATION_END:
                snippets(context);
                break;
        }
    }

    public void snippets(Context context) {
        for (Map.Entry<String, String> snippets : this.snippets.entrySet()) {
            snippetSection(context, snippets.getKey(), snippets.getValue());
        }
    }

    public void snippetSection(Context context, String snippetName, String title) {
        ContentExtension content = new ContentExtension(globalContext, context);
        URI snippetUri = operationSnippetUri(context, context.getOperation().get(), snippetName);
        logger.info("Processing Spring REST Docs snippet: {}", snippetUri.toString());
        content.importContent(snippetUri, reader -> {
            context.getMarkupDocBuilder().sectionTitleLevel(1 + levelOffset(context), title);
            context.getMarkupDocBuilder().importMarkup(reader, extensionMarkupLanguage, levelOffset(context) + 1);
        });
    }
}
