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
import io.github.swagger2markup.spi.DefinitionsDocumentExtension;
import io.github.swagger2markup.utils.IOUtils;
import io.github.swagger2markup.utils.URIUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Add external schemas to content.<br>
 * Supported formats are :
 * <ul>
 * <li>XML Schema (.xsd)</li>
 * <li>JSON Schema (.json)</li>
 * </ul>
 */
public final class SchemaExtension extends DefinitionsDocumentExtension {

    private static final Logger logger = LoggerFactory.getLogger(SchemaExtension.class);

    private static final List<SchemaMetadata> DEFAULT_SCHEMAS = new ArrayList<SchemaMetadata>() {{
        add(new SchemaMetadata("JSON Schema", "json", "json"));
        add(new SchemaMetadata("XML Schema", "xsd", "xml"));
    }};

    private static final String PROPERTY_SCHEMAS_BASE_URI = "schemaBaseUri";
    private static final String DEFAULT_EXTENSION_ID = "schema";
    private static final String PROPERTY_DEFAULT_SCHEMAS = "defaultSchemas";

    protected List<SchemaMetadata> schemas = new ArrayList<>();
    private String extensionId = DEFAULT_EXTENSION_ID;

    protected URI schemaBaseUri;

    /**
     * Instantiate extension with the default extension id.
     * @param schemaBaseUri base URI where the schemas are stored
     */
    public SchemaExtension(URI schemaBaseUri) {
        this(null, schemaBaseUri);
    }

    /**
     * Instantiate extension
     * @param extensionId the unique ID of the extension
     * @param schemaBaseUri base URI where the schemas are stored
     */
    public SchemaExtension(String extensionId, URI schemaBaseUri) {
        super();
        Validate.notNull(schemaBaseUri);
        if(StringUtils.isNoneBlank(extensionId)) {
            this.extensionId = extensionId;
        }
        this.schemaBaseUri = schemaBaseUri;
    }

    public SchemaExtension() {
        super();
    }

    public SchemaExtension withDefaultSchemas() {
        schemas.addAll(DEFAULT_SCHEMAS);
        return this;
    }

    public SchemaExtension withSchemas(List<SchemaMetadata> schemas) {
        this.schemas.addAll(schemas);
        return this;
    }

    @Override
    public void init(Swagger2MarkupConverter.Context globalContext) {
        Swagger2MarkupProperties extensionsProperties = globalContext.getConfig().getExtensionsProperties();
        Optional<URI> schemaBaseUriProperty = extensionsProperties.getURI(extensionId + "." + PROPERTY_SCHEMAS_BASE_URI);
        boolean withDefaultSchemas = extensionsProperties.getBoolean(extensionId + "." + PROPERTY_DEFAULT_SCHEMAS, true);
        if(withDefaultSchemas){
            withDefaultSchemas();
        }
        if (schemaBaseUriProperty.isPresent()) {
            schemaBaseUri = URIUtils.convertUriWithoutSchemeToFileScheme(schemaBaseUriProperty.get());
        }
        else {
            if (schemaBaseUri == null) {
                if (globalContext.getSwaggerLocation() == null) {
                    if (logger.isWarnEnabled())
                        logger.warn("Disable SchemaExtension > Can't set default schemaBaseUri from swaggerLocation. You have to explicitly configure the schemaBaseUri.");
                } else {
                    schemaBaseUri = URIUtils.uriParent(globalContext.getSwaggerLocation());
                }
            }
        }
    }

    @Override
    public void apply(Context context) {
        Validate.notNull(context);

        if (schemaBaseUri != null) {
            switch (context.getPosition()) {
                case DEFINITION_END:
                    for (SchemaMetadata schema : DEFAULT_SCHEMAS) {
                        schemaSection(context, schema, levelOffset(context));
                    }
                    break;
            }
        }
    }

    /**
     * Builds snippet URI for the given {@code definitionName} and {@code schema}.<br>
     * Default implementation use {@code <schemaBaseUri>/normalizeName(<definitionName>)/schema.<schema.extension>}.<br>
     * You can override this method to configure your own folder normalization.
     *
     * @param context        current context
     * @param definitionName current definition name
     * @return subdirectory normalized name
     */
    public URI definitionSchemaUri(Context context, String definitionName, SchemaMetadata schema) {
        return schemaBaseUri.resolve(IOUtils.normalizeName(definitionName) + "/").resolve("schema" + (schema.extension != null ? "." + schema.extension : ""));
    }

    private void schemaSection(Context context, SchemaMetadata schema, int levelOffset) {
        ContentExtension contentExtension = new ContentExtension(globalContext, context);
        URI schemaUri = definitionSchemaUri(context, context.getDefinitionName().get(), schema);
        logger.info("Processing schema: {}", schemaUri.toString());
        contentExtension.importContent(schemaUri, reader -> {
            context.getMarkupDocBuilder().sectionTitleLevel(1 + levelOffset, schema.title);
            try {
                context.getMarkupDocBuilder().listingBlock(org.apache.commons.io.IOUtils.toString(reader).trim(), schema.language);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to read schema URI : %s", schemaUri), e);
            }
        });
    }

    public static class SchemaMetadata {
        /**
         * Schema title
         */
        public String title;

        /**
         * Schema file extension, without dot (e.g.: xsd).<br>
         * Set to null if there's no extension
         */
        public String extension;

        /**
         * Schema content language (e.g.: xml)
         */
        public String language;

        public SchemaMetadata(String title, String extension, String language) {
            this.title = title;
            this.extension = extension;
            this.language = language;
        }
    }
}
