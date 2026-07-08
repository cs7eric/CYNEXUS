package cn.cccs7.cynexus.knowledgebase.application.service;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentChunkBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentIndexTaskBO;
import cn.cccs7.cynexus.knowledgebase.domain.repository.KnowledgeBaseIndexRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseIndexAppService {

    private static final int CHUNK_MAX_LENGTH = 1200;

    private final KnowledgeBaseIndexRepository indexRepository;

    public KnowledgeBaseIndexAppService(KnowledgeBaseIndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    public void refreshDocumentIndex(KnowledgeDocumentBO document) {
        if (document == null || document.getId() == null) {
            return;
        }
        List<KnowledgeDocumentChunkBO> chunks = buildChunks(document);
        List<KnowledgeDocumentIndexTaskBO> tasks = List.of(
                buildTask(document.getId(), "pgvector", chunks.size()),
                buildTask(document.getId(), "elasticsearch", chunks.size())
        );
        indexRepository.replaceDocumentIndex(document.getId(), chunks, tasks);
    }

    public void deleteDocumentIndex(Long documentId) {
        indexRepository.deleteDocumentIndex(documentId);
    }

    private List<KnowledgeDocumentChunkBO> buildChunks(KnowledgeDocumentBO document) {
        List<String> blocks = splitMarkdownBlocks(document.getContent());
        List<KnowledgeDocumentChunkBO> chunks = new ArrayList<>();
        String currentHeading = "";
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (block.startsWith("#")) {
                currentHeading = block.replaceFirst("^#+\\s*", "").trim();
            }
            if (current.length() + block.length() + 2 > CHUNK_MAX_LENGTH && current.length() > 0) {
                chunks.add(buildChunk(document.getId(), chunks.size(), currentHeading, current.toString()));
                current.setLength(0);
            }
            if (current.length() > 0) {
                current.append(System.lineSeparator()).append(System.lineSeparator());
            }
            current.append(block);
        }
        if (current.length() > 0) {
            chunks.add(buildChunk(document.getId(), chunks.size(), currentHeading, current.toString()));
        }
        if (chunks.isEmpty()) {
            chunks.add(buildChunk(document.getId(), 0, document.getTitle(), ""));
        }
        return chunks;
    }

    private KnowledgeDocumentChunkBO buildChunk(Long documentId, int index, String heading, String content) {
        String normalizedContent = content == null ? "" : content.trim();
        return KnowledgeDocumentChunkBO.builder()
                .documentId(documentId)
                .chunkIndex(index)
                .heading(blankToNull(heading))
                .content(normalizedContent)
                .contentHash(sha256(normalizedContent))
                .tokenEstimate(estimateTokens(normalizedContent))
                .embeddingStatus("pending")
                .extra("{\"source\":\"markdown\"}")
                .build();
    }

    private KnowledgeDocumentIndexTaskBO buildTask(Long documentId, String target, int chunkCount) {
        return KnowledgeDocumentIndexTaskBO.builder()
                .documentId(documentId)
                .target(target)
                .status("pending")
                .payload("{\"chunkCount\":" + chunkCount + "}")
                .build();
    }

    private List<String> splitMarkdownBlocks(String content) {
        String normalized = content == null ? "" : content.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split("\\n\\s*\\n"));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private int estimateTokens(String value) {
        return value == null || value.isBlank() ? 0 : Math.max(1, value.length() / 4);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
