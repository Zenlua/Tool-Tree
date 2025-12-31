# Reference: https://setuptools.pypa.io/en/latest/build_meta.html#dynamic-build-dependencies-and-other-build-meta-tweaks
from __future__ import annotations

import os
from typing import Any

import setuptools.build_meta
from setuptools.build_meta import *  # noqa: F403


def get_requires_for_build_wheel(config_settings: dict[str, Any] | None = None) -> list[str]:
    base = setuptools.build_meta.get_requires_for_build_wheel(config_settings)

    if os.getenv("BUILD_RUST", "").lower() in {"1", "true"} or (config_settings and "--build-rust" in config_settings):
        return [*base, "setuptools-rust"]

    return base
