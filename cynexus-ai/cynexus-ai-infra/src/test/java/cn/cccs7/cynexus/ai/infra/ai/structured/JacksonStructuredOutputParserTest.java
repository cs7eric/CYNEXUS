package cn.cccs7.cynexus.ai.infra.ai.structured;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class JacksonStructuredOutputParserTest {
    private final JacksonStructuredOutputParser parser = new JacksonStructuredOutputParser();

    @Test void extractsMarkdownJsonObject() {
        assertEquals("{\"ok\":true}", parser.extractObject("```json\n{\"ok\":true}\n```"));
    }

    @Test void rejectsMalformedJson() {
        StructuredAiOutputAssert.assertFailure(parser.parse("{bad"));
    }

    @Test void rejectsOversizedPayload() {
        assertFalse(parser.parseBounded("12345", 4).successful());
    }

    @Test void extractsJsonEmbeddedInProviderProse() {
        assertEquals("{\"ok\":true}", parser.extractObject("Here is the result: {\"ok\":true}"));
    }

    @Test void sanitizesFieldsAndBoundsArraysAndStrings() {
        String value = parser.sanitize("{\"keep\":\"abcdef\",\"drop\":1,\"items\":[1,2,3]}",
                java.util.Set.of("keep", "items"), 3, 2);
        assertEquals("{\"keep\":\"abc\",\"items\":[1,2]}", value);
    }

    @Test void validatesSchemaEnumsAndCollectionBounds() {
        String schema = "{\"type\":\"object\",\"required\":[\"kind\",\"items\"],\"properties\":{"
                + "\"kind\":{\"type\":\"string\",\"enum\":[\"review\",\"technique\"]},"
                + "\"items\":{\"type\":\"array\",\"maxItems\":2,\"items\":{\"type\":\"string\",\"maxLength\":4}}}}";
        parser.validateSchema("{\"kind\":\"review\",\"items\":[\"good\"]}", schema);
        assertThrows(IllegalArgumentException.class,
                () -> parser.validateSchema("{\"kind\":\"other\",\"items\":[]}", schema));
        assertThrows(IllegalArgumentException.class,
                () -> parser.validateSchema("{\"kind\":\"review\",\"items\":[\"toolong\",\"ok\",\"x\"]}", schema));
    }

    private static final class StructuredAiOutputAssert {
        static void assertFailure(cn.cccs7.cynexus.ai.domain.port.StructuredAiOutput output) {
            assertFalse(output.successful());
            assertNotNull(output.error());
        }
    }
}
