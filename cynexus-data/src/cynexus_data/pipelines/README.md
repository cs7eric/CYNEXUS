# pipelines

Pipeline orchestration.

Pipelines wire sources, domain transforms, quality checks, and storage writers. Pipelines should receive `PipelineContext` instead of creating raw clients or reading secrets directly.

