package cn.cccs7.cynexus.ai.infra.ai.rag;

import cn.cccs7.cynexus.ai.domain.port.KnowledgeDocumentChunker;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentChunkerImpl implements KnowledgeDocumentChunker {
    @Override public List<String> chunk(String content, int maxCharacters, int overlapCharacters) {
        String text = content == null ? "" : content.trim();
        int max = Math.max(128, maxCharacters), overlap = Math.max(0, Math.min(overlapCharacters, max - 1));
        List<String> result = new ArrayList<>();
        for (int start = 0; start < text.length();) {
            int end = Math.min(text.length(), start + max);
            if (end < text.length()) {
                int boundary = text.lastIndexOf("\n\n", end);
                if (boundary > start + max / 2) end = boundary;
            }
            result.add(text.substring(start, end).trim());
            if (end >= text.length()) break;
            start = Math.max(start + 1, end - overlap);
        }
        return List.copyOf(result);
    }
}
