from __future__ import annotations

import plistlib
import uuid
from collections import UserDict
from typing import TYPE_CHECKING, Any, BinaryIO

from dissect.util.ts import cocoatimestamp

if TYPE_CHECKING:
    from datetime import datetime


class NSKeyedArchiver:
    def __init__(self, fh: BinaryIO):
        self.plist = plistlib.load(fh)

        if not isinstance(self.plist, dict) or not all(
            key in self.plist for key in ["$version", "$archiver", "$top", "$objects"]
        ):
            raise ValueError("File is not an NSKeyedArchiver plist")

        self._objects = self.plist.get("$objects")
        self._cache = {}

        self.top = {}
        for name, value in self.plist.get("$top", {}).items():
            self.top[name] = self._parse(value)

    def __getitem__(self, key: str) -> Any:
        return self.top[key]

    def __repr__(self) -> str:
        return f"<NSKeyedArchiver top={self.top}>"

    def get(self, key: str, default: Any | None = None) -> Any:
        return self.top.get(key, default)

    def _parse(self, uid: Any) -> Any:
        if not isinstance(uid, plistlib.UID):
            return uid

        num = uid.data
        if num in self._cache:
            return self._cache[num]
        result = self._parse_obj(self._objects[num])
        self._cache[num] = result
        return result

    def _parse_obj(self, obj: Any) -> Any:
        if isinstance(obj, dict):
            klass = obj.get("$class")
            if klass:
                klass_name = self._parse(klass).get("$classname")
                return CLASSES.get(klass_name, NSObject)(self, obj)
            return obj

        if isinstance(obj, list):
            return list(map(self._parse, obj))

        if isinstance(obj, bool | bytes | int | float) or obj is None:
            return obj

        if isinstance(obj, str):
            return None if obj == "$null" else obj

        return None


class NSObject:
    def __init__(self, nskeyed: NSKeyedArchiver, obj: dict[str, Any]):
        self.nskeyed = nskeyed
        self.obj = obj

        self._class = nskeyed._parse(obj.get("$class", {}))
        self._classname = self._class.get("$classname", "Unknown")
        self._classes = self._class.get("$classes", [])

    def __getitem__(self, attr: str) -> Any:
        obj = self.obj[attr]
        return self.nskeyed._parse(obj)

    def __getattr__(self, attr: str) -> Any:
        try:
            return self[attr]
        except KeyError:
            raise AttributeError(attr)

    def __repr__(self):
        return f"<{self._classname}>"

    def keys(self) -> list[str]:
        return self.obj.keys()

    def get(self, attr: str, default: Any | None = None) -> Any:
        try:
            return self[attr]
        except KeyError:
            return default


class NSDictionary(UserDict, NSObject):
    def __init__(self, nskeyed: NSKeyedArchiver, obj: dict[str, Any]):
        NSObject.__init__(self, nskeyed, obj)
        self.data = {nskeyed._parse(key): obj for key, obj in zip(obj["NS.keys"], obj["NS.objects"], strict=False)}

    def __repr__(self) -> str:
        return NSObject.__repr__(self)

    def __getitem__(self, key: str) -> Any:
        return self.nskeyed._parse(self.data[key])


def parse_nsarray(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> list[Any]:
    return list(map(nskeyed._parse, obj["NS.objects"]))


def parse_nsset(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> list[Any]:
    # Some values are not hashable, so return as list
    return parse_nsarray(nskeyed, obj)


def parse_nsdata(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> Any:
    return obj["NS.data"]


def parse_nsdate(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> datetime:
    return cocoatimestamp(obj["NS.time"])


def parse_nsuuid(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> uuid.UUID:
    return uuid.UUID(bytes=obj["NS.uuidbytes"])


def parse_nsurl(nskeyed: NSKeyedArchiver, obj: dict[str, Any]) -> str:
    base = nskeyed._parse(obj["NS.base"])
    relative = nskeyed._parse(obj["NS.relative"])
    if base:
        return f"{base}/{relative}"
    return relative


CLASSES = {
    "NSArray": parse_nsarray,
    "NSMutableArray": parse_nsarray,
    "NSDictionary": NSDictionary,
    "NSMutableDictionary": NSDictionary,
    "NSSet": parse_nsset,
    "NSMutableSet": parse_nsset,
    "NSData": parse_nsdata,
    "NSMutableData": parse_nsdata,
    "NSDate": parse_nsdate,
    "NSUUID": parse_nsuuid,
    "NSURL": parse_nsurl,
    "NSNull": lambda nskeyed, obj: None,
}
