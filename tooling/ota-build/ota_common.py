"""Shared CLI and ROM_TYPE helpers for Honda Civic G10 OTA package builders."""
from __future__ import annotations

import argparse
import os

DEFAULT_ROM_TYPE = "0040"


def default_rom_type() -> str:
    """Return ROM_TYPE from the environment, falling back to DEFAULT_ROM_TYPE."""
    val = os.environ.get("ROM_TYPE", "").strip()
    return val or DEFAULT_ROM_TYPE


def default_rom_types() -> list[str]:
    """Return a list of ROM types from ROM_TYPE env (comma-separated), or [DEFAULT_ROM_TYPE]."""
    raw = os.environ.get("ROM_TYPE", "").strip()
    if not raw:
        return [DEFAULT_ROM_TYPE]
    parts = [part.strip() for part in raw.split(",") if part.strip()]
    return parts or [DEFAULT_ROM_TYPE]


def add_rom_type_arg(parser: argparse.ArgumentParser, multiple: bool = False) -> None:
    """Register --rom-type option on an ArgumentParser."""
    if multiple:
        parser.add_argument(
            "--rom-type",
            dest="rom_types",
            action="append",
            help=f"Target custom_rom.type (repeatable; defaults to ROM_TYPE env or {DEFAULT_ROM_TYPE}).",
        )
    else:
        parser.add_argument(
            "--rom-type",
            default=None,
            help=f"Target custom_rom.type (defaults to ROM_TYPE env or {DEFAULT_ROM_TYPE}).",
        )
