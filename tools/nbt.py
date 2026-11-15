"""Écriture NBT minimale (structures de GameTest et du sanctuaire)."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND = range(11)


class Int(int):
    pass


class Byte(int):
    pass


class List(list):
    """Liste NBT ; elem_type obligatoire si vide."""
    def __init__(self, items=(), elem_type=None):
        super().__init__(items)
        self.elem_type = elem_type


def _tag_type(v):
    if isinstance(v, Byte):
        return BYTE
    if isinstance(v, bool):
        return BYTE
    if isinstance(v, int):
        return INT
    if isinstance(v, float):
        return DOUBLE
    if isinstance(v, str):
        return STRING
    if isinstance(v, list):
        return LIST
    if isinstance(v, dict):
        return COMPOUND
    raise TypeError(type(v))


def _str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload(t, v):
    if t == BYTE:
        return struct.pack(">b", int(v))
    if t == INT:
        return struct.pack(">i", v)
    if t == DOUBLE:
        return struct.pack(">d", v)
    if t == STRING:
        return _str(v)
    if t == LIST:
        et = getattr(v, "elem_type", None)
        if et is None:
            et = _tag_type(v[0]) if v else END
        out = struct.pack(">bi", et, len(v))
        for item in v:
            out += _payload(et, item)
        return out
    if t == COMPOUND:
        out = b""
        for k, item in v.items():
            it = _tag_type(item)
            out += struct.pack(">b", it) + _str(k) + _payload(it, item)
        return out + b"\x00"
    raise ValueError(t)


def write(path, root, name=""):
    data = struct.pack(">b", COMPOUND) + _str(name) + _payload(COMPOUND, root)
    with gzip.open(path, "wb") as f:
        f.write(data)


DATA_VERSION_1_21_1 = 3955


def structure(size, blocks, palette, entities=()):
    """blocks: liste de (x, y, z, index_palette[, nbt]) ; palette: liste de dicts {Name, Properties?}."""
    bl = []
    for b in blocks:
        entry = {"pos": List([b[0], b[1], b[2]]), "state": b[3]}
        if len(b) > 4 and b[4]:
            entry["nbt"] = b[4]
        bl.append(entry)
    return {
        "DataVersion": DATA_VERSION_1_21_1,
        "size": List(list(size)),
        "palette": List(palette, COMPOUND),
        "blocks": List(bl, COMPOUND),
        "entities": List(list(entities), COMPOUND),
    }
