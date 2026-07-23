package cn.cccs7.cynexus.ai.domain.entity;

import java.util.List;

public record MediaTranscriptionMetadataBO(String title, List<String> categories, List<String> tags) {
}
