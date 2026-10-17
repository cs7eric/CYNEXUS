package cn.cccs7.cynexus.ai.domain.port;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BasicAiStructuredOutputParserTest {
    private final AiStructuredOutputParser parser = AiStructuredOutputParser.shared();

    @Test
    void extractsFencedJsonAfterProviderPrefix() {
        assertEquals("{\"ok\":true}", parser.extractObject("Here is the result:\n```json\n{\"ok\":true}\n```"));
    }

    @Test
    void extractsArrayAndRejectsEmptyOrOversizedOutput() {
        assertEquals("[1,2]", parser.extractObject("prefix [1,2] suffix"));
        assertThrows(IllegalArgumentException.class, () -> parser.extractObject(" "));
        assertThrows(IllegalArgumentException.class, () -> parser.extractObject("x".repeat(256_001)));
    }

    @Test
    void validatesSchemaEnumsRequiredFieldsAndBounds() {
        String schema = "{\"type\":\"object\",\"required\":[\"kind\"],\"properties\":{\"kind\":{\"type\":\"string\",\"enum\":[\"review\",\"technique\"]},\"items\":{\"type\":\"array\",\"maxItems\":2}}}";
        parser.validateSchema("{\"kind\":\"review\",\"items\":[]}", schema);
        assertThrows(IllegalArgumentException.class, () -> parser.validateSchema("{\"kind\":\"unknown\"}", schema));
        assertThrows(IllegalArgumentException.class, () -> parser.validateSchema("{}", schema));
        assertThrows(IllegalArgumentException.class, () -> parser.validateSchema("{\"kind\":\"review\",\"items\":[1,2,3]}", schema));
    }

    @Test
    void sanitizesUnknownFieldsStringsAndArrays() {
        assertEquals("{\"keep\":\"abc\",\"items\":[1,2]}", parser.sanitize(
                "{\"keep\":\"abcdef\",\"drop\":1,\"items\":[1,2,3]}",
                java.util.Set.of("keep", "items"), 3, 2));
    }
}
