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

import com.google.common.base.Optional;
import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.builder.Swagger2MarkupProperties;
import io.github.swagger2markup.spi.OverviewDocumentExtension;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Dynamically search for markup files in {@code contentPath} to append to Overview, with the format :<br>
 * - {@code document-before-*.<markup.ext>} : import before Overview document with levelOffset = 0<br>
 * - {@code document-begin-*.<markup.ext>} : import just after Overview document main title with levelOffset = 1<br>
 * - {@code document-end-*.<markup.ext>} : import at the end of Overview document with levelOffset = 1<br>
 * <p>
 * Markup files are appended in the natural order of their names, for each category.
 */
public final class DynamicOverviewDocumentExtension extends OverviewDocumentExtension {

    private static final Logger logger = LoggerFactory.getLogger(DynamicOverviewDocumentExtension.class);

    protected Path contentPath;

    private static final String DEFAULT_EXTENSION_ID = "dynamicOverview";
    private static final String PROPERTY_CONTENT_PATH = "contentPath";
    private static final String PROPERTY_MARKUP_LANGUAGE = "markupLanguage";
    private String extensionId = DEFAULT_EXTENSION_ID;
    private MarkupLanguage extensionMarkupLanguage = MarkupLanguage.ASCIIDOC;

    /**
     * Instantiate extension with the default extension id.
     * @param contentPath the base Path where the content is stored
     * @param extensionMarkupLanguage the MarkupLanguage of the extension content
     */
    public DynamicOverviewDocumentExtension(Path contentPath, MarkupLanguage extensionMarkupLanguage) {
        this(null, contentPath, extensionMarkupLanguage);
    }

    /**
     * Instantiate extension
     * @param extensionId the unique ID of the extension
     * @param contentPath the base Path where the content is stored
     * @param extensionMarkupLanguage the MarkupLanguage of the extension content
     */
    public DynamicOverviewDocumentExtension(String extensionId, Path contentPath, MarkupLanguage extensionMarkupLanguage) {
        super();
        Validate.notNull(extensionId);
        Validate.notNull(contentPath);
        if(StringUtils.isNoneBlank(extensionId)) {
            this.extensionId = extensionId;
        }
        this.contentPath = contentPath;
        this.extensionMarkupLanguage = extensionMarkupLanguage;
    }

    public DynamicOverviewDocumentExtension() {
        super();
    }

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        Swagger2MarkupProperties extensionsProperties = globalContext.getConfig().getExtensionsProperties();
        Optional<String> contentPathString = extensionsProperties.getString(extensionId + "." + PROPERTY_CONTENT_PATH);
        Optional<Path> contentPathProperty;
        if(contentPathString.isPresent()){
            contentPathProperty = Optional.of(Paths.get(contentPathString.get()));
        }else{
            contentPathProperty = Optional.absent();
        }

        if (contentPathProperty.isPresent()) {
            contentPath = contentPathProperty.get();
        }
        else {
            if (contentPath == null) {
                if (globalContext.getSwaggerLocation() == null || !globalContext.getSwaggerLocation().getScheme().equals("file")) {
                    if (logger.isWarnEnabled())
                        logger.warn("Disable > DynamicOverviewContentExtension > Can't set default contentPath from swaggerLocation. You have to explicitly configure the content path.");
                } else {
                    contentPath = Paths.get(globalContext.getSwaggerLocation()).getParent();
                }
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
            OverviewDocumentExtension.Position position = context.getPosition();
            switch (position) {
                case DOCUMENT_BEFORE:
                    dynamicContent.extensionsSection(extensionMarkupLanguage, contentPath, contentPrefix(position), levelOffset(context));
                    break;
                case DOCUMENT_BEGIN:
                    dynamicContent.extensionsSection(extensionMarkupLanguage, contentPath, contentPrefix(position), levelOffset(context));
                    break;
                case DOCUMENT_END:
                    dynamicContent.extensionsSection(extensionMarkupLanguage, contentPath, contentPrefix(position), levelOffset(context));
                    break;
                default:
                    throw new RuntimeException(String.format("Unknown position '%s'", position));
            }
        }
    }

    private String contentPrefix(Position position) {
        return position.name().toLowerCase().replace('_', '-');
    }

}
