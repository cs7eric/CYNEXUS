package cn.cccs7.cynexus.ai.application.service;

record ApplicationAnalysisPromptRuntime(
        String promptKey,
        String promptVersion,
        String systemPrompt,
        String userPromptTemplate) {
}
