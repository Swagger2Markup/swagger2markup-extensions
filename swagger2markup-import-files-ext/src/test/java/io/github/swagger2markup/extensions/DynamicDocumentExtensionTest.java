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

import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.swagger2markup.Swagger2MarkupConfig;
import io.github.swagger2markup.Swagger2MarkupConverter;
import io.github.swagger2markup.Swagger2MarkupExtensionRegistry;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamicDocumentExtensionTest {

    @Test
    public void testSwagger2AsciiDocExtensions() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(DynamicDocumentExtensionTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/asciidoc/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .build();
        Swagger2MarkupExtensionRegistry registry = Swagger2MarkupExtensionRegistry.ofEmpty()
                .withDefinitionsDocumentExtension(new DynamicDefinitionsDocumentExtension(Paths.get("src/test/resources/docs/asciidoc/extensions")))
                .withPathsDocumentExtension(new DynamicPathsDocumentExtension(Paths.get("src/test/resources/docs/asciidoc/extensions")))
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .withExtensionRegistry(registry)
                .build()
                .intoFolder(outputDirectory);

        //Then
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("paths.adoc")))).contains(
                "Pet update request extension");
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("definitions.adoc")))).contains(
                "Pet extension");

    }

    @Test
    public void testSwagger2MarkdownExtensions() throws IOException, URISyntaxException {
        //Given
        Path file = Paths.get(DynamicDocumentExtensionTest.class.getResource("/yaml/swagger_petstore.yaml").toURI());
        Path outputDirectory = Paths.get("build/docs/markdown/generated");
        FileUtils.deleteQuietly(outputDirectory.toFile());

        //When
        Swagger2MarkupConfig config = Swagger2MarkupConfig.ofDefaults()
                .withMarkupLanguage(MarkupLanguage.MARKDOWN)
                .build();
        Swagger2MarkupExtensionRegistry registry = Swagger2MarkupExtensionRegistry.ofEmpty()
                .withDefinitionsDocumentExtension(new DynamicDefinitionsDocumentExtension(Paths.get("src/test/resources/docs/markdown/extensions")))
                .withPathsDocumentExtension(new DynamicPathsDocumentExtension(Paths.get("src/test/resources/docs/markdown/extensions")))
                .build();
        Swagger2MarkupConverter.from(file)
                .withConfig(config)
                .withExtensionRegistry(registry)
                .build()
                .intoFolder(outputDirectory);

        //Then
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("paths.md")))).contains(
                "Pet update request extension");
        assertThat(new String(Files.readAllBytes(outputDirectory.resolve("definitions.md")))).contains(
                "Pet extension");

    }
}
