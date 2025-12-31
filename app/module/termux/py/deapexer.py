#!/data/data/com.tool.tree/files/home/termux/bin/python
#
# Copyright (C) 2019 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""deapexer is a tool that prints out content of an APEX.

To print content of an APEX to stdout:
  deapexer list foo.apex

To extract content of an APEX to the given directory:
  deapexer extract foo.apex
"""
from __future__ import print_function

import argparse
import enum
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile

import apex_manifest

BLOCK_SIZE = 4096
current_dir = "."
tempdir = ""

# See apexd/apex_file.cpp#RetrieveFsType
FS_TYPES = [
    ('f2fs', 1024, b'\x10\x20\xf5\xf2'),
    ('ext4', 1024 + 0x38, b'\123\357'),
    ('erofs', 1024, b'\xe2\xe1\xf5\xe0'),
]


def RetrieveFileSystemType(file):
    """Returns filesystem type with magic"""
    with open(file, 'rb') as f:
        for type, offset, magic in FS_TYPES:
            buf = bytearray(len(magic))
            f.seek(offset, os.SEEK_SET)
            f.readinto(buf)
            if buf == magic:
                return type
    raise ValueError('Failed to retrieve filesystem type')


class ApexImageEntry(object):

    def __init__(self, name, base_dir, permissions, size, ino, extents,
                 is_directory, is_symlink, security_context):
        self._name = name
        self._base_dir = base_dir
        self._permissions = permissions
        self._size = size
        self._is_directory = is_directory
        self._is_symlink = is_symlink
        self._ino = ino
        self._extents = extents
        self._security_context = security_context

    @property
    def name(self):
        return self._name

    @property
    def root(self):
        return self._base_dir == './' and self._name == '.'

    @property
    def full_path(self):
        if self.root:
            return self._base_dir  # './'
        path = os.path.join(self._base_dir, self._name)
        if self.is_directory:
            path += '/'
        return path

    @property
    def is_directory(self):
        return self._is_directory

    @property
    def is_symlink(self):
        return self._is_symlink

    @property
    def is_regular_file(self):
        return not self.is_directory and not self.is_symlink

    @property
    def permissions(self):
        return self._permissions

    @property
    def size(self):
        return self._size

    @property
    def ino(self):
        return self._ino

    @property
    def extents(self):
        return self._extents

    @property
    def security_context(self):
        return self._security_context

    def __str__(self):
        ret = ''
        if self._is_directory:
            ret += 'd'
        elif self._is_symlink:
            ret += 'l'
        else:
            ret += '-'

        def mask_as_string(m):
            ret = 'r' if m & 4 == 4 else '-'
            ret += 'w' if m & 2 == 2 else '-'
            ret += 'x' if m & 1 == 1 else '-'
            return ret

        ret += mask_as_string(self._permissions >> 6)
        ret += mask_as_string((self._permissions >> 3) & 7)
        ret += mask_as_string(self._permissions & 7)

        return ret + ' ' + self._size + ' ' + self._name


class ApexImageDirectory(object):

    def __init__(self, path, entries, apex):
        self._path = path
        self._entries = sorted(entries, key=lambda e: e.name)
        self._apex = apex

    def list(self, is_recursive=False):
        for e in self._entries:
            yield e
            if e.is_directory and e.name != '.' and e.name != '..':
                for ce in self.enter_subdir(e).list(is_recursive):
                    yield ce

    def enter_subdir(self, entry):
        return self._apex._list(self._path + entry.name + '/')


class Apex(object):

    def __init__(self, args):
        self._debugfs = "debugfs"
        self._fsckerofs = "fsck.erofs"
        self._apex = args.apex
        self._tempdir = tempfile.mkdtemp()
        global tempdir
        tempdir = self._tempdir
        with zipfile.ZipFile(self._apex, 'r') as zip_ref:
            zip_ref.extractall(self._tempdir)
            self._payload = os.path.join(self._tempdir, 'apex_payload.img')
        self._payload_fs_type = RetrieveFileSystemType(self._payload)
        self._cache = {}

    def __del__(self):
        shutil.rmtree(self._tempdir)

    def __enter__(self):
        return self

    def __exit__(self, type, value, traceback):
        pass

    def list(self, is_recursive=False):
        if self._payload_fs_type not in ['ext4']:
            sys.exit(f"{self._payload_fs_type} is not supported for `list`.")

        root = self._list('./')
        return root.list(is_recursive)

    def _list(self, path):
        if path in self._cache:
            return self._cache[path]
        res = subprocess.check_output([self._debugfs, '-R', 'ls -l -p %s' % path, self._payload],
                                      text=True, stderr=subprocess.DEVNULL)
        entries = []
        for line in res.split('\n'):
            if not line:
                continue
            parts = line.split('/')
            if len(parts) != 8:
                continue
            name = parts[5]
            if not name:
                continue
            ino = parts[1]
            bits = parts[2]
            size = parts[6]
            extents = []
            is_symlink = bits[1] == '2'
            is_directory = bits[1] == '4'

            if not is_symlink and not is_directory:
                stdout = subprocess.check_output([self._debugfs, '-R', 'dump_extents <%s>' % ino,
                                                  self._payload], text=True, stderr=subprocess.DEVNULL)
                # Output of dump_extents for an inode fragmented in 3 blocks (length and addresses represent
                # block-sized sections):
                # Level Entries       Logical      Physical Length Flags
                # 0/ 0   1/  3     0 -     0    18 -    18      1
                # 0/ 0   2/  3     1 -    15    20 -    34     15
                # 0/ 0   3/  3    16 -  1863    37 -  1884   1848
                res = stdout.splitlines()
                res.pop(0)  # the first line contains only columns names
                left_length = int(size)
                try:  # dump_extents sometimes has an unexpected output
                    for line in res:
                        tokens = line.split()
                        offset = int(tokens[7]) * BLOCK_SIZE
                        length = min(int(tokens[-1]) * BLOCK_SIZE, left_length)
                        left_length -= length
                        extents.append((offset, length))
                    if left_length != 0:  # dump_extents sometimes fails to display "hole" blocks
                        raise ValueError
                except:
                    extents = []  # [] means that we failed to retrieve the file location successfully

            # get 'security.selinux' attribute
            entry_path = os.path.join(path, name)
            stdout = subprocess.check_output([
                self._debugfs,
                '-R',
                f'ea_get -V {entry_path} security.selinux',
                self._payload
            ], text=True, stderr=subprocess.DEVNULL)
            security_context = stdout.rstrip('\n\x00')

            entries.append(ApexImageEntry(name,
                                          base_dir=path,
                                          permissions=int(bits[3:], 8),
                                          size=size,
                                          is_directory=is_directory,
                                          is_symlink=is_symlink,
                                          ino=ino,
                                          extents=extents,
                                          security_context=security_context))

        return ApexImageDirectory(path, entries, self)

    def extract(self, dest):
        if self._payload_fs_type == 'erofs':
            subprocess.run([self._fsckerofs, '--extract=%s' % dest, '--overwrite', self._payload],
                           stdout=subprocess.DEVNULL, check=True)
        elif self._payload_fs_type == 'ext4':
            # Suppress stderr without failure
            try:
                subprocess.run([self._debugfs, '-R', 'rdump ./ %s' % dest, self._payload],
                               capture_output=True, check=True)
            except subprocess.CalledProcessError as e:
                sys.exit(e.stderr)
        else:
            # TODO(b/279688635) f2fs is not supported yet.
            sys.exit(f"{self._payload_fs_type} is not supported for `extract`.")


def RunList(args):
    if GetType(args.apex) == ApexType.COMPRESSED:
        with tempfile.TemporaryDirectory() as temp:
            decompressed_apex = os.path.join(temp, 'temp.apex')
            decompress(args.apex, decompressed_apex)
            args.apex = decompressed_apex

            RunList(args)
            return

    with Apex(args) as apex:
        for e in apex.list(is_recursive=True):
            # dot(., ..) directories
            if not e.root and e.name in ('.', '..'):
                continue
            res = ''
            if args.size:
                res += e.size + ' '
            res += e.full_path
            if args.extents:
                res += ' [' + '-'.join(str(x) for x in e.extents) + ']'
            if args.contexts:
                res += ' ' + e.security_context
            print(res)

def write_apex_info(dest_dir, fs_type):
    """
    Write apex_infor.ini with filesystem type
    """
    info_path = os.path.join(dest_dir, "apex_infor.ini")
    with open(info_path, "w", encoding="utf-8") as f:
        f.write(f"fscon: {fs_type}\n")

def RunExtract(args):
    if GetType(args.apex) == ApexType.COMPRESSED:
        with tempfile.TemporaryDirectory() as temp:
            decompressed_apex = os.path.join(temp, "temp.apex")
            decompress(args.apex, decompressed_apex)
            args.apex = decompressed_apex

            RunExtract(args)
            return

    with Apex(args) as apex:
        if args.dest is None:
            args.dest = current_dir
        if os.path.exists(args.dest):
            shutil.rmtree(args.dest)
        os.makedirs(args.dest, mode=0o755)
        payload_dir = os.path.join(args.dest, "payload")
        manifest_dir = os.path.join(args.dest, "manifest")
        os.makedirs(payload_dir, mode=0o755)
        os.makedirs(manifest_dir, mode=0o755)

        apex.extract(payload_dir)

        build_pb = os.path.join(tempdir, "apex_build_info.pb")
        manifest_pb = os.path.join(payload_dir, "apex_manifest.pb")
        assets_dir = os.path.join(tempdir, "assets")

        if os.path.exists(build_pb):
            target_build_pb_path = os.path.join(manifest_dir, "apex_build_info.pb")
            if os.path.exists(target_build_pb_path):
                os.remove(target_build_pb_path)
            shutil.move(build_pb, manifest_dir, copy_function=shutil.copy2)

        if os.path.exists(manifest_pb):
            target_manifest_pb_path = os.path.join(manifest_dir, "apex_manifest.pb")
            if os.path.exists(target_manifest_pb_path):
                os.remove(target_manifest_pb_path)
            shutil.move(manifest_pb, manifest_dir, copy_function=shutil.copy2)

        if os.path.exists(assets_dir):
            target_assets_path = os.path.join(manifest_dir, "assets")
            if os.path.exists(target_assets_path):
                shutil.rmtree(target_assets_path)
            shutil.move(assets_dir, manifest_dir, copy_function=shutil.copy2)

        if os.path.isdir(os.path.join(payload_dir, "lost+found")):
            shutil.rmtree(os.path.join(payload_dir, "lost+found"))
        #dest_dir = args.dest if args.dest else current_dir
        #print(f"I: Unpacking is completed, \noutput path: {dest_dir}")

        dest_dir = args.dest if args.dest else current_dir
        write_apex_info(dest_dir, apex._payload_fs_type)

        print(
            "I: Unpacking is completed\n"
            f"filesystem: {apex._payload_fs_type}\n"
            f"output path: {dest_dir}"
        )

class ApexType(enum.Enum):
    INVALID = 0
    UNCOMPRESSED = 1
    COMPRESSED = 2


def GetType(apex_path):
    with zipfile.ZipFile(apex_path, 'r') as zip_file:
        names = zip_file.namelist()
        has_payload = 'apex_payload.img' in names
        has_original_apex = 'original_apex' in names
        if has_payload and has_original_apex:
            return ApexType.INVALID
        if has_payload:
            return ApexType.UNCOMPRESSED
        if has_original_apex:
            return ApexType.COMPRESSED
        return ApexType.INVALID


def RunInfo(args):
    if args.print_type:
        res = GetType(args.apex)
        if res == ApexType.INVALID:
            print(args.apex + ' is not a valid apex')
            sys.exit(1)
        print(res.name)
    else:
        manifest = apex_manifest.fromApex(args.apex)
        print(apex_manifest.toJsonString(manifest))


def RunDecompress(args):
    """RunDecompress takes path to compressed APEX and decompresses it to
    produce the original uncompressed APEX at give output path

    See apex_compression_tool.py#RunCompress for details on compressed APEX
    structure.

    Args:
        args.input: file path to compressed APEX
        args.output: file path to where decompressed APEX will be placed
    """
    compressed_apex_fp = args.input
    decompressed_apex_fp = args.output
    return decompress(compressed_apex_fp, decompressed_apex_fp)


def decompress(compressed_apex_fp, decompressed_apex_fp):
    if os.path.exists(decompressed_apex_fp):
        print("Output path '" + decompressed_apex_fp + "' already exists")
        sys.exit(1)

    with zipfile.ZipFile(compressed_apex_fp, 'r') as zip_obj:
        if 'original_apex' not in zip_obj.namelist():
            print(compressed_apex_fp + ' is not a compressed APEX. Missing '
                                       "'original_apex' file inside it.")
            sys.exit(1)
        # Rename original_apex file to what user provided as output filename
        original_apex_info = zip_obj.getinfo('original_apex')
        original_apex_info.filename = os.path.basename(decompressed_apex_fp)
        # Extract the original_apex as desired name
        zip_obj.extract(original_apex_info,
                        path=os.path.dirname(decompressed_apex_fp))

def require_tool(name):
    if shutil.which(name) is None:
        print(f"Required tool not found in PATH: {name}", file=sys.stderr)
        sys.exit(1)

def main(argv):
    require_tool("debugfs")
    require_tool("fsck.erofs")
    parser = argparse.ArgumentParser()
    global current_dir
    current_dir = os.path.dirname(os.path.abspath(__file__))
    subparsers = parser.add_subparsers(required=True, dest='cmd')

    parser_list = subparsers.add_parser('list', help='prints content of an APEX to stdout')
    parser_list.add_argument('apex', type=str, help='APEX file')
    parser_list.add_argument('--size', help='also show the size of the files', action="store_true")
    parser_list.add_argument('--extents', help='also show the location of the files', action="store_true")
    parser_list.add_argument('-Z', '--contexts',
                             help='also show the security context of the files',
                             action='store_true')
    parser_list.set_defaults(func=RunList)

    parser_extract = subparsers.add_parser('extract', help='extracts content of an APEX to the given '
                                                           'directory')
    parser_extract.add_argument('apex', type=str, help='APEX file')
    parser_extract.add_argument('-d', '--dest', type=str, help='Directory to extract content of APEX to')
    parser_extract.set_defaults(func=RunExtract)

    parser_info = subparsers.add_parser('info', help='prints APEX manifest')
    parser_info.add_argument('apex', type=str, help='APEX file')
    parser_info.add_argument('--print-type',
                             help='Prints type of the apex (COMPRESSED or UNCOMPRESSED)',
                             action='store_true')
    parser_info.set_defaults(func=RunInfo)

    # Handle sub-command "decompress"
    parser_decompress = subparsers.add_parser('decompress',
                                              help='decompresses a compressed '
                                                   'APEX')
    parser_decompress.add_argument('--input', type=str, required=True,
                                   help='path to compressed APEX file that '
                                        'will be decompressed')
    parser_decompress.add_argument('--output', type=str, required=True,
                                   help='output directory path where '
                                        'decompressed APEX will be extracted')
    parser_decompress.set_defaults(func=RunDecompress)

    args = parser.parse_args(argv)

    args.func(args)


if __name__ == '__main__':
    main(sys.argv[1:])
