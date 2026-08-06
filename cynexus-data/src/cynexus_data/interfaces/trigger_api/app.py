from __future__ import annotations

import argparse
import logging

from fastapi import FastAPI

from cynexus_data.interfaces.trigger_api.a_stock_routes import router as a_stock_router
from cynexus_data.interfaces.trigger_api.data_source_health_routes import router as data_source_health_router
from cynexus_data.interfaces.trigger_api.f10_routes import router as f10_router
from cynexus_data.interfaces.trigger_api.industry_application_routes import router as industry_application_router
from cynexus_data.interfaces.trigger_api.information_content_routes import router as information_content_router
from cynexus_data.interfaces.trigger_api.media_transcription_routes import router as media_transcription_router
from cynexus_data.interfaces.trigger_api.social_routes import router as social_router
from cynexus_data.interfaces.trigger_api.routes import router
from cynexus_data.interfaces.trigger_api.routes import _context


log = logging.getLogger(__name__)


def create_app() -> FastAPI:
    app = FastAPI(title="cynexus-data internal trigger API", version="0.1.0")
    app.include_router(router)
    app.include_router(a_stock_router)
    app.include_router(data_source_health_router)
    app.include_router(f10_router)
    app.include_router(industry_application_router)
    app.include_router(information_content_router)
    app.include_router(media_transcription_router)
    app.include_router(social_router)

    @app.on_event("startup")
    def recover_interrupted_runs() -> None:
        count = _context.job_store.mark_interrupted_running_jobs("interrupted by data-job-executor restart")
        if count:
            log.warning("Marked %s interrupted data job run(s) as failed on startup", count)

    return app


app = create_app()


def main() -> None:
    parser = argparse.ArgumentParser(description="Run cynexus-data trigger API.")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=18083)
    args = parser.parse_args()
    import uvicorn

    uvicorn.run("cynexus_data.interfaces.trigger_api.app:app", host=args.host, port=args.port)


if __name__ == "__main__":
    main()
