from __future__ import annotations

import stat

from dissect.cstruct import cstruct

squashfs_def = """
#define SQUASHFS_MAGIC          0x73717368
#define SQUASHFS_MAGIC_SWAP     0x68737173

/* size of metadata (inode and directory) blocks */
#define SQUASHFS_METADATA_SIZE  8192

#define SQUASHFS_INVALID        0xffffffffffff
#define SQUASHFS_INVALID_FRAG   0xffffffff
#define SQUASHFS_INVALID_XATTR  0xffffffff
#define SQUASHFS_INVALID_BLK    -1
#define SQUASHFS_USED_BLK       -2

/* Filesystem flags */
#define SQUASHFS_NOI            0
#define SQUASHFS_NOD            1
#define SQUASHFS_CHECK          2
#define SQUASHFS_NOF            3
#define SQUASHFS_NO_FRAG        4
#define SQUASHFS_ALWAYS_FRAG    5
#define SQUASHFS_DUPLICATE      6
#define SQUASHFS_EXPORT         7
#define SQUASHFS_NOX            8
#define SQUASHFS_NO_XATTR       9
#define SQUASHFS_COMP_OPT       10
#define SQUASHFS_NOID           11

/* Max number of types and file types */
#define SQUASHFS_DIR_TYPE       1
#define SQUASHFS_FILE_TYPE      2
#define SQUASHFS_SYMLINK_TYPE   3
#define SQUASHFS_BLKDEV_TYPE    4
#define SQUASHFS_CHRDEV_TYPE    5
#define SQUASHFS_FIFO_TYPE      6
#define SQUASHFS_SOCKET_TYPE    7
#define SQUASHFS_LDIR_TYPE      8
#define SQUASHFS_LREG_TYPE      9
#define SQUASHFS_LSYMLINK_TYPE  10
#define SQUASHFS_LBLKDEV_TYPE   11
#define SQUASHFS_LCHRDEV_TYPE   12
#define SQUASHFS_LFIFO_TYPE     13
#define SQUASHFS_LSOCKET_TYPE   14

/* Xattr types */
#define SQUASHFS_XATTR_USER     0
#define SQUASHFS_XATTR_TRUSTED  1
#define SQUASHFS_XATTR_SECURITY 2
#define SQUASHFS_XATTR_VALUE_OOL    256
#define SQUASHFS_XATTR_PREFIX_MASK  0xff

/* Flag whether block is compressed or uncompressed, bit is set if block is uncompressed */
#define SQUASHFS_COMPRESSED_BIT         (1 << 15)
#define SQUASHFS_COMPRESSED_BIT_BLOCK   (1 << 24)

typedef long long squashfs_block;
typedef long long squashfs_inode;

#define ZLIB_COMPRESSION    1
#define LZMA_COMPRESSION    2
#define LZO_COMPRESSION     3
#define XZ_COMPRESSION      4
#define LZ4_COMPRESSION     5
#define ZSTD_COMPRESSION    6

struct squashfs_super_block {
    unsigned int        s_magic;
    unsigned int        inodes;
    unsigned int        mkfs_time;
    unsigned int        block_size;
    unsigned int        fragments;
    unsigned short      compression;
    unsigned short      block_log;
    unsigned short      flags;
    unsigned short      no_ids;
    unsigned short      s_major;
    unsigned short      s_minor;
    squashfs_inode      root_inode;
    long long           bytes_used;
    long long           id_table_start;
    long long           xattr_id_table_start;
    long long           inode_table_start;
    long long           directory_table_start;
    long long           fragment_table_start;
    long long           lookup_table_start;
};

struct squashfs_super_block_3 {
    unsigned int        s_magic;
    unsigned int        inodes;
    unsigned int        bytes_used_2;
    unsigned int        uid_start_2;
    unsigned int        guid_start_2;
    unsigned int        inode_table_start_2;
    unsigned int        directory_table_start_2;
    unsigned int        s_major : 16;
    unsigned int        s_minor : 16;
    unsigned int        block_size_1 : 16;
    unsigned int        block_log : 16;
    unsigned int        flags : 8;
    unsigned int        no_uids : 8;
    unsigned int        no_guids : 8;
    int                 mkfs_time;
    squashfs_inode      root_inode;
    unsigned int        block_size;
    unsigned int        fragments;
    unsigned int        fragment_table_start_2;
    long long           bytes_used;
    long long           uid_start;
    long long           guid_start;
    long long           inode_table_start;
    long long           directory_table_start;
    long long           fragment_table_start;
    long long           lookup_table_start;
};

struct squashfs_dir_index {
    unsigned int        index;
    unsigned int        start_block;
    unsigned int        size;
    unsigned char       name[0];
};

struct squashfs_base_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
};

struct squashfs_ipc_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
};

struct squashfs_lipc_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
    unsigned int        xattr;
};

struct squashfs_dev_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
    unsigned int        rdev;
};

struct squashfs_ldev_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
    unsigned int        rdev;
    unsigned int        xattr;
};

struct squashfs_symlink_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
    unsigned int        symlink_size;
    char                symlink[0];
};

struct squashfs_reg_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        start_block;
    unsigned int        fragment;
    unsigned int        offset;
    unsigned int        file_size;
    unsigned int        block_list[0];
};

struct squashfs_lreg_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    squashfs_block      start_block;
    long long           file_size;
    long long           sparse;
    unsigned int        nlink;
    unsigned int        fragment;
    unsigned int        offset;
    unsigned int        xattr;
    unsigned int        block_list[0];
};

struct squashfs_dir_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        start_block;
    unsigned int        nlink;
    unsigned short      file_size;
    unsigned short      offset;
    unsigned int        parent_inode;
};

struct squashfs_ldir_inode_header {
    unsigned short      inode_type;
    unsigned short      mode;
    unsigned short      uid;
    unsigned short      guid;
    unsigned int        mtime;
    unsigned int        inode_number;
    unsigned int        nlink;
    unsigned int        file_size;
    unsigned int        start_block;
    unsigned int        parent_inode;
    unsigned short      i_count;
    unsigned short      offset;
    unsigned int        xattr;
    struct squashfs_dir_index   index[0];
};

struct squashfs_dir_entry {
    unsigned short      offset;
    short               inode_number;
    unsigned short      type;
    unsigned short      size;
    char                name[0];
};

struct squashfs_dir_header {
    unsigned int        count;
    unsigned int        start_block;
    unsigned int        inode_number;
};

struct squashfs_fragment_entry {
    long long           start_block;
    unsigned int        size;
    unsigned int        unused;
};

struct squashfs_xattr_entry {
    unsigned short      type;
    unsigned short      size;
};

struct squashfs_xattr_val {
    unsigned int        vsize;
};

struct squashfs_xattr_id {
    long long           xattr;
    unsigned int        count;
    unsigned int        size;
};

struct squashfs_xattr_table {
    long long           xattr_table_start;
    unsigned int        xattr_ids;
    unsigned int        unused;
};

// Compression options

struct gzip_comp_opts {
    int                 compression_level;
    short               window_size;
    short               strategy;
};

struct lzo_comp_opts {
    int                 algorithm;
    int                 compression_level;
};

struct xz_comp_opts {
    int                 dictionary_size;
    int                 flags;
};

struct lz4_comp_opts {
    int                 version;
    int                 flags;
};

struct zstd_comp_opts {
    int                 compression_level;
};
"""

c_squashfs = cstruct().load(squashfs_def)

INODE_STRUCT_MAP = {
    c_squashfs.SQUASHFS_DIR_TYPE: c_squashfs.squashfs_dir_inode_header,
    c_squashfs.SQUASHFS_FILE_TYPE: c_squashfs.squashfs_reg_inode_header,
    c_squashfs.SQUASHFS_SYMLINK_TYPE: c_squashfs.squashfs_symlink_inode_header,
    c_squashfs.SQUASHFS_BLKDEV_TYPE: c_squashfs.squashfs_dev_inode_header,
    c_squashfs.SQUASHFS_CHRDEV_TYPE: c_squashfs.squashfs_dev_inode_header,
    c_squashfs.SQUASHFS_FIFO_TYPE: c_squashfs.squashfs_base_inode_header,
    c_squashfs.SQUASHFS_SOCKET_TYPE: c_squashfs.squashfs_base_inode_header,
    c_squashfs.SQUASHFS_LDIR_TYPE: c_squashfs.squashfs_ldir_inode_header,
    c_squashfs.SQUASHFS_LREG_TYPE: c_squashfs.squashfs_lreg_inode_header,
    c_squashfs.SQUASHFS_LSYMLINK_TYPE: c_squashfs.squashfs_symlink_inode_header,
    c_squashfs.SQUASHFS_LBLKDEV_TYPE: c_squashfs.squashfs_ldev_inode_header,
    c_squashfs.SQUASHFS_LCHRDEV_TYPE: c_squashfs.squashfs_ldev_inode_header,
    c_squashfs.SQUASHFS_LFIFO_TYPE: c_squashfs.squashfs_lipc_inode_header,
    c_squashfs.SQUASHFS_LSOCKET_TYPE: c_squashfs.squashfs_lipc_inode_header,
}

TYPE_MAP = [
    None,
    stat.S_IFDIR,
    stat.S_IFREG,
    stat.S_IFLNK,
    stat.S_IFBLK,
    stat.S_IFCHR,
    stat.S_IFIFO,
    stat.S_IFSOCK,
    stat.S_IFDIR,
    stat.S_IFREG,
    stat.S_IFLNK,
    stat.S_IFBLK,
    stat.S_IFCHR,
    stat.S_IFIFO,
    stat.S_IFSOCK,
]
