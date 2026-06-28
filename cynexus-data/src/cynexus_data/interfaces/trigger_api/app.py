from __future__ import annotations

import argparse

from fastapi import FastAPI

from cynexus_data.interfaces.trigger_api.routes import router


def create_app() -> FastAPI:
    app = FastAPI(title="cynexus-data internal trigger API", version="0.1.0")
    app.include_router(router)
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
