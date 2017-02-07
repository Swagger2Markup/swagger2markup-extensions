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
import io.github.swagger2markup.spi.PathsDocumentExtension;
import io.github.swagger2markup.utils.IOUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Dynamically search for markup files in {@code contentPath} to append to Paths document.
 * <p>
 * Markup files are appended in the natural order of their names, for each category.
 */
public final class DynamicPathsDocumentExtension extends PathsDocumentExtension {

    private static final Logger logger = LoggerFactory.getLogger(DynamicPathsDocumentExtension.class);

    protected List<Path> contentPath;

    private static final String DEFAULT_EXTENSION_ID = "dynamicPaths";
    private static final String PROPERTY_CONTENT_PATH = "contentPath";
    private static final String PROPERTY_MARKUP_LANGUAGE = "markupLanguage";
    private String extensionId = DEFAULT_EXTENSION_ID;
    private MarkupLanguage extensionMarkupLanguage = MarkupLanguage.ASCIIDOC;

    /**
     * Instantiate extension with the default extension id.
     * @param contentPath the base Path where the content is stored
     * @param extensionMarkupLanguage the MarkupLanguage of the extension content
     */
    public DynamicPathsDocumentExtension(List<Path> contentPath, MarkupLanguage extensionMarkupLanguage) {
        this(null, contentPath, extensionMarkupLanguage);
    }

    /**
     * Instantiate extension
     * @param extensionId the unique ID of the extension
     * @param contentPath the base Path where the content is stored
     * @param extensionMarkupLanguage the MarkupLanguage of the extension content
     */
    public DynamicPathsDocumentExtension(String extensionId, List<Path> contentPath, MarkupLanguage extensionMarkupLanguage) {
        super();
        Validate.notNull(extensionMarkupLanguage);
        Validate.notNull(contentPath);
        if(StringUtils.isNoneBlank(extensionId)) {
            this.extensionId = extensionId;
        }
        this.contentPath = contentPath;
        this.extensionMarkupLanguage = extensionMarkupLanguage;
    }

    public DynamicPathsDocumentExtension() {
        super();
    }

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        Swagger2MarkupProperties extensionsProperties = globalContext.getConfig().getExtensionsProperties();
        contentPath = extensionsProperties.getPathList(extensionId + "." + PROPERTY_CONTENT_PATH);
        
        if (contentPath.isEmpty()) {
            if (globalContext.getSwaggerLocation() == null || !globalContext.getSwaggerLocation().getScheme().equals("file")) {
                if (logger.isWarnEnabled())
                    logger.warn("Disable DynamicOperationsContentExtension > Can't set default contentPath from swaggerLocation. You have to explicitly configure the content path.");
            } else {
                contentPath.add(Paths.get(globalContext.getSwaggerLocation()).getParent());
            }
        }

        Optional<MarkupLanguage> extensionMarkupLanguageProperty = extensionsProperties.getMarkupLanguage(extensionId + "." + PROPERTY_MARKUP_LANGUAGE);
        if (extensionMarkupLanguageProperty.isPresent()) {
            extensionMarkupLanguage = extensionMarkupLanguageProperty.get();
        }
    }

    @Override
    public void apply(Context context) {
        Validate.notNull(context);

        if (contentPath != null) {
            DynamicContentExtension dynamicContent = new DynamicContentExtension(globalContext, context);
            PathsDocumentExtension.Position position = context.getPosition();
            switch (position) {
            case DOCUMENT_BEFORE:
            case DOCUMENT_AFTER:
            case DOCUMENT_BEGIN:
            case DOCUMENT_END:
                dynamicContent.extensionsSection(extensionMarkupLanguage, contentPath, contentPrefix(position), levelOffset(context));
                break;
            case OPERATION_BEFORE:
            case OPERATION_BEGIN:
            case OPERATION_END:
            case OPERATION_AFTER:
            case OPERATION_DESCRIPTION_BEFORE:
            case OPERATION_DESCRIPTION_AFTER:
            case OPERATION_PARAMETERS_BEFORE:
            case OPERATION_PARAMETERS_AFTER:
            case OPERATION_RESPONSES_BEFORE:
            case OPERATION_RESPONSES_AFTER:
            case OPERATION_SECURITY_BEFORE:
            case OPERATION_SECURITY_AFTER:
            case OPERATION_DESCRIPTION_BEGIN:
            case OPERATION_DESCRIPTION_END:
            case OPERATION_PARAMETERS_BEGIN:
            case OPERATION_PARAMETERS_END:
            case OPERATION_RESPONSES_BEGIN:
            case OPERATION_RESPONSES_END:
            case OPERATION_SECURITY_BEGIN:
            case OPERATION_SECURITY_END:
                List<Path> resolvedPaths = contentPath.stream().map(
                        p -> p.resolve(IOUtils.normalizeName(context.getOperation().get().getId())))
                        .collect(Collectors.toList());
                dynamicContent.extensionsSection(extensionMarkupLanguage, resolvedPaths, contentPrefix(position), levelOffset(context));
                break;
            }
        }
    }

    private String contentPrefix(Position position) {
        return position.name().toLowerCase().replace('_', '-');
    }

}
