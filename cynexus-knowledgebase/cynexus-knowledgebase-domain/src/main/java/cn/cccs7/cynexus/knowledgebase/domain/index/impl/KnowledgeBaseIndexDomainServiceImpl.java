package cn.cccs7.cynexus.knowledgebase.domain.index.impl;

import cn.cccs7.cynexus.knowledgebase.common.constant.KnowledgeBaseConstants;

import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentChunkBO;
import cn.cccs7.cynexus.knowledgebase.domain.entity.KnowledgeDocumentIndexTaskBO;
import cn.cccs7.cynexus.knowledgebase.domain.repository.KnowledgeBaseIndexRepository;
import cn.cccs7.cynexus.knowledgebase.domain.index.KnowledgeBaseIndexDomainService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseIndexDomainServiceImpl implements KnowledgeBaseIndexDomainService {

    private final KnowledgeBaseIndexRepository indexRepository;

    public KnowledgeBaseIndexDomainServiceImpl(KnowledgeBaseIndexRepository indexRepository) {
        this.indexRepository = indexRepository;
    }

    @Override
    public void refreshDocumentIndex(KnowledgeDocumentBO document) {
        if (document == null || document.getId() == null) {
            return;
        }
        List<KnowledgeDocumentChunkBO> chunks = buildChunks(document);
        List<KnowledgeDocumentIndexTaskBO> tasks = List.of(
                buildTask(document.getId(), KnowledgeBaseConstants.INDEX_TARGET_PGVECTOR, chunks.size()),
                buildTask(document.getId(), KnowledgeBaseConstants.INDEX_TARGET_ELASTICSEARCH, chunks.size())
        );
        indexRepository.replaceDocumentIndex(document.getId(), chunks, tasks);
    }

    @Override
    public void deleteDocumentIndex(Long documentId) {
        if (documentId != null) {
            indexRepository.deleteDocumentIndex(documentId);
        }
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
            if (current.length() + block.length() + 2 > KnowledgeBaseConstants.MAX_CHUNK_LENGTH && current.length() > 0) {
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
                .embeddingStatus(KnowledgeBaseConstants.INDEX_STATUS_PENDING)
                .extra("{\"source\":\"markdown\"}")
                .build();
    }

    private KnowledgeDocumentIndexTaskBO buildTask(Long documentId, String target, int chunkCount) {
        return KnowledgeDocumentIndexTaskBO.builder()
                .documentId(documentId)
                .target(target)
                .status(KnowledgeBaseConstants.INDEX_STATUS_PENDING)
                .payload("{\"chunkCount\":" + chunkCount + "}")
                .build();
    }

    private List<String> splitMarkdownBlocks(String content) {
        String normalized = content == null ? "" : content.replace("\\r\\n", "\\n").trim();
        return normalized.isBlank() ? List.of() : List.of(normalized.split("\\n\\s*\\n"));
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
