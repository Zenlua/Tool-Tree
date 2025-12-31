# system configuration generated and used by the sysconfig module
build_time_vars = {'ABIFLAGS': '',
 'AC_APPLE_UNIVERSAL_BUILD': 0,
 'AIX_BUILDDATE': 0,
 'AIX_GENUINE_CPLUSPLUS': 0,
 'ALIGNOF_LONG': 8,
 'ALIGNOF_MAX_ALIGN_T': 16,
 'ALIGNOF_SIZE_T': 8,
 'ALT_SOABI': 0,
 'ANDROID_API_LEVEL': 24,
 'AR': 'llvm-ar',
 'ARFLAGS': 'rcs',
 'BASECFLAGS': '-fno-strict-overflow -Wsign-compare -Wunreachable-code',
 'BASECPPFLAGS': '-IObjects -IInclude -IPython',
 'BASEMODLIBS': '',
 'BINDIR': '/data/data/com.termux/files/usr/bin',
 'BINLIBDEST': '/data/data/com.termux/files/usr/lib/python3.12',
 'BLDLIBRARY': '-L. -lpython3.12',
 'BLDSHARED': 'aarch64-linux-android-clang -shared '
              '-L/data/data/com.termux/files/usr/lib '
              '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
              '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
              '-Wl,--no-as-needed,-landroid-support,--as-needed '
              '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
              '-landroid-posix-semaphore -L/data/data/com.termux/files/usr/lib '
              '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
              '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
              '-Wl,--no-as-needed,-landroid-support,--as-needed '
              '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
              '-landroid-posix-semaphore',
 'BOOTSTRAP_HEADERS': '\\',
 'BUILDEXE': '',
 'BUILDPYTHON': 'python',
 'BUILD_GNU_TYPE': 'x86_64-pc-linux-gnu',
 'BUILD_SCRIPTS_DIR': 'build/scripts-3.12',
 'BYTESTR_DEPS': '\\',
 'CC': 'aarch64-linux-android-clang',
 'CCSHARED': '-fPIC',
 'CFLAGS': '-fno-strict-overflow -Wsign-compare -Wunreachable-code -DNDEBUG -g '
           '-O3 -Wall -fstack-protector-strong -O3  -fstack-protector-strong '
           '-O3',
 'CFLAGSFORSHARED': '-fPIC',
 'CFLAGS_ALIASING': '-fno-strict-aliasing',
 'CFLAGS_NODIST': '',
 'CODECS_COMMON_HEADERS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                          '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'COMPILEALL_OPTS': '-j0',
 'CONFIGFILES': 'configure configure.ac acconfig.h pyconfig.h.in '
                'Makefile.pre.in',
 'CONFIGURE_CFLAGS': '-fstack-protector-strong -O3',
 'CONFIGURE_CFLAGS_NODIST': '-std=c11 -Wextra -Wno-unused-parameter '
                            '-Wno-missing-field-initializers '
                            '-Wstrict-prototypes '
                            '-Werror=implicit-function-declaration '
                            '-fvisibility=hidden',
 'CONFIGURE_CPPFLAGS': '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                       '-isystem/data/data/com.termux/files/usr/include '
                       '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include',
 'CONFIGURE_LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
                      '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                      '-Wl,--enable-new-dtags -Wl,--as-needed '
                      '-Wl,-z,relro,-z,now '
                      '-Wl,--no-as-needed,-landroid-support,--as-needed '
                      '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                      '-landroid-posix-semaphore',
 'CONFIGURE_LDFLAGS_NODIST': '',
 'CONFIGURE_LDFLAGS_NOLTO': '',
 'CONFIG_ARGS': "'--disable-dependency-tracking' "
                "'--prefix=/data/data/com.termux/files/usr' "
                "'--libdir=/data/data/com.termux/files/usr/lib' "
                "'--includedir=/data/data/com.termux/files/usr/include' "
                "'--sbindir=/data/data/com.termux/files/usr/bin' "
                "'--disable-rpath' '--disable-rpath-hack' "
                "'--host=aarch64-linux-android' 'ac_cv_file__dev_ptmx=yes' "
                "'ac_cv_file__dev_ptc=no' 'ac_cv_func_wcsftime=no' "
                "'ac_cv_func_ftime=no' 'ac_cv_func_faccessat=no' "
                "'--build=x86_64-pc-linux-gnu' '--with-system-ffi' "
                "'--with-system-expat' '--without-ensurepip' "
                "'ac_cv_func_linkat=no' 'ac_cv_buggy_getaddrinfo=no' "
                "'--enable-loadable-sqlite-extensions' "
                "'ac_cv_little_endian_double=yes' "
                "'ac_cv_posix_semaphores_enabled=yes' "
                "'ac_cv_func_sem_open=yes' 'ac_cv_func_sem_timedwait=yes' "
                "'ac_cv_func_sem_getvalue=yes' 'ac_cv_func_sem_unlink=yes' "
                "'ac_cv_func_shm_open=yes' 'ac_cv_func_shm_unlink=yes' "
                "'ac_cv_working_tzset=yes' '--with-build-python=python3.12' "
                "'--disable-nls' '--enable-shared' '--enable-static' "
                "'--libexecdir=/data/data/com.termux/files/usr/libexec' "
                "'build_alias=x86_64-pc-linux-gnu' "
                "'host_alias=aarch64-linux-android' "
                "'PKG_CONFIG=/home/builder/.termux-build/_cache/android-r28c-api-24-v2/bin/pkg-config' "
                "'PKG_CONFIG_LIBDIR=/data/data/com.termux/files/usr/lib/pkgconfig:/data/data/com.termux/files/usr/share/pkgconfig' "
                "'CC=aarch64-linux-android-clang' 'CFLAGS= "
                "-fstack-protector-strong -O3' "
                "'LDFLAGS=-L/data/data/com.termux/files/usr/lib "
                '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
                '-Wl,--no-as-needed,-landroid-support,--as-needed '
                '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                "-landroid-posix-semaphore' 'CPPFLAGS= "
                '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                '-isystem/data/data/com.termux/files/usr/include '
                "-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include' "
                "'CPP=aarch64-linux-android-cpp' 'LIBCRYPT_LIBS=-lcrypt'",
 'CONFINCLUDEDIR': '/data/data/com.termux/files/usr/include',
 'CONFINCLUDEPY': '/data/data/com.termux/files/usr/include/python3.12',
 'COREPYTHONPATH': '',
 'COVERAGE_INFO': '/home/builder/.termux-build/python/build/coverage.info',
 'COVERAGE_LCOV_OPTIONS': '--rc lcov_branch_coverage=1',
 'COVERAGE_REPORT': '/home/builder/.termux-build/python/build/lcov-report',
 'COVERAGE_REPORT_OPTIONS': '--rc lcov_branch_coverage=1 --branch-coverage '
                            '--title "CPython 3.12 LCOV report [commit $(shell '
                            ')]"',
 'CPPFLAGS': '-IObjects -IInclude -IPython -I. '
             '-I/home/builder/.termux-build/python/src/Include '
             '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
             '-isystem/data/data/com.termux/files/usr/include '
             '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
             '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
             '-isystem/data/data/com.termux/files/usr/include '
             '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include',
 'CXX': 'aarch64-linux-android-clang++',
 'DEEPFREEZE_C': 'Python/deepfreeze/deepfreeze.c',
 'DEEPFREEZE_DEPS': '/home/builder/.termux-build/python/src/Tools/build/deepfreeze.py '
                    '/home/builder/.termux-build/python/src/Programs/_freeze_module.py '
                    '\\',
 'DEEPFREEZE_OBJS': 'Python/deepfreeze/deepfreeze.o',
 'DESTDIRS': '/data/data/com.termux/files/usr '
             '/data/data/com.termux/files/usr/lib '
             '/data/data/com.termux/files/usr/lib/python3.12 '
             '/data/data/com.termux/files/usr/lib/python3.12/lib-dynload',
 'DESTLIB': '/data/data/com.termux/files/usr/lib/python3.12',
 'DESTPATH': '',
 'DESTSHARED': '/data/data/com.termux/files/usr/lib/python3.12/lib-dynload',
 'DFLAGS': '',
 'DIRMODE': 755,
 'DIST': 'README.rst ChangeLog configure configure.ac acconfig.h pyconfig.h.in '
         'Makefile.pre.in Include Lib Misc Ext-dummy',
 'DISTDIRS': 'Include Lib Misc Ext-dummy',
 'DISTFILES': 'README.rst ChangeLog configure configure.ac acconfig.h '
              'pyconfig.h.in Makefile.pre.in',
 'DLINCLDIR': '.',
 'DLLLIBRARY': '',
 'DOUBLE_IS_ARM_MIXED_ENDIAN_IEEE754': 0,
 'DOUBLE_IS_BIG_ENDIAN_IEEE754': 0,
 'DOUBLE_IS_LITTLE_ENDIAN_IEEE754': 1,
 'DSYMUTIL': '',
 'DSYMUTIL_PATH': '',
 'DTRACE': '',
 'DTRACE_DEPS': '\\',
 'DTRACE_HEADERS': '',
 'DTRACE_OBJS': '',
 'DYNLOADFILE': 'dynload_shlib.o',
 'ENABLE_IPV6': 1,
 'ENSUREPIP': 'no',
 'EXE': '',
 'EXEMODE': 755,
 'EXENAME': '/data/data/com.termux/files/usr/bin/python3.12',
 'EXPORTSFROM': '',
 'EXPORTSYMS': '',
 'EXTRATESTOPTS': '',
 'EXTRA_CFLAGS': '',
 'EXT_SUFFIX': '.cpython-312.so',
 'FILEMODE': 644,
 'FREEZE_MODULE': 'python3.12 '
                  '/home/builder/.termux-build/python/src/Programs/_freeze_module.py',
 'FREEZE_MODULE_BOOTSTRAP': 'python3.12 '
                            '/home/builder/.termux-build/python/src/Programs/_freeze_module.py',
 'FREEZE_MODULE_BOOTSTRAP_DEPS': '/home/builder/.termux-build/python/src/Programs/_freeze_module.py',
 'FREEZE_MODULE_DEPS': '/home/builder/.termux-build/python/src/Programs/_freeze_module.py',
 'FROZEN_FILES_IN': '\\',
 'FROZEN_FILES_OUT': '\\',
 'GETPGRP_HAVE_ARG': 0,
 'GITBRANCH': '',
 'GITTAG': '',
 'GITVERSION': '',
 'GNULD': 'yes',
 'HAVE_ACCEPT': 1,
 'HAVE_ACCEPT4': 1,
 'HAVE_ACOSH': 1,
 'HAVE_ADDRINFO': 1,
 'HAVE_ALARM': 1,
 'HAVE_ALIGNED_REQUIRED': 1,
 'HAVE_ALLOCA_H': 1,
 'HAVE_ALTZONE': 0,
 'HAVE_ASINH': 1,
 'HAVE_ASM_TYPES_H': 1,
 'HAVE_ATANH': 1,
 'HAVE_BIND': 1,
 'HAVE_BIND_TEXTDOMAIN_CODESET': 0,
 'HAVE_BLUETOOTH_BLUETOOTH_H': 0,
 'HAVE_BLUETOOTH_H': 0,
 'HAVE_BROKEN_MBSTOWCS': 0,
 'HAVE_BROKEN_NICE': 0,
 'HAVE_BROKEN_PIPE_BUF': 0,
 'HAVE_BROKEN_POLL': 0,
 'HAVE_BROKEN_POSIX_SEMAPHORES': 0,
 'HAVE_BROKEN_PTHREAD_SIGMASK': 0,
 'HAVE_BROKEN_SEM_GETVALUE': 1,
 'HAVE_BROKEN_UNSETENV': 0,
 'HAVE_BUILTIN_ATOMIC': 1,
 'HAVE_BZLIB_H': 1,
 'HAVE_CHFLAGS': 0,
 'HAVE_CHMOD': 1,
 'HAVE_CHOWN': 1,
 'HAVE_CHROOT': 1,
 'HAVE_CLOCK': 1,
 'HAVE_CLOCK_GETRES': 1,
 'HAVE_CLOCK_GETTIME': 1,
 'HAVE_CLOCK_NANOSLEEP': 1,
 'HAVE_CLOCK_SETTIME': 1,
 'HAVE_CLOSE_RANGE': 0,
 'HAVE_COMPUTED_GOTOS': 0,
 'HAVE_CONFSTR': 0,
 'HAVE_CONIO_H': 0,
 'HAVE_CONNECT': 1,
 'HAVE_COPY_FILE_RANGE': 0,
 'HAVE_CRYPT_H': 1,
 'HAVE_CRYPT_R': 0,
 'HAVE_CTERMID': 0,
 'HAVE_CTERMID_R': 0,
 'HAVE_CURSES_FILTER': 1,
 'HAVE_CURSES_H': 1,
 'HAVE_CURSES_HAS_KEY': 1,
 'HAVE_CURSES_IMMEDOK': 1,
 'HAVE_CURSES_IS_PAD': 1,
 'HAVE_CURSES_IS_TERM_RESIZED': 1,
 'HAVE_CURSES_RESIZETERM': 1,
 'HAVE_CURSES_RESIZE_TERM': 1,
 'HAVE_CURSES_SYNCOK': 1,
 'HAVE_CURSES_TYPEAHEAD': 1,
 'HAVE_CURSES_USE_ENV': 1,
 'HAVE_CURSES_WCHGAT': 1,
 'HAVE_DB_H': 0,
 'HAVE_DECL_RTLD_DEEPBIND': 0,
 'HAVE_DECL_RTLD_GLOBAL': 1,
 'HAVE_DECL_RTLD_LAZY': 1,
 'HAVE_DECL_RTLD_LOCAL': 1,
 'HAVE_DECL_RTLD_MEMBER': 0,
 'HAVE_DECL_RTLD_NODELETE': 1,
 'HAVE_DECL_RTLD_NOLOAD': 1,
 'HAVE_DECL_RTLD_NOW': 1,
 'HAVE_DECL_TZNAME': 0,
 'HAVE_DEVICE_MACROS': 1,
 'HAVE_DEV_PTC': 0,
 'HAVE_DEV_PTMX': 1,
 'HAVE_DIRECT_H': 0,
 'HAVE_DIRENT_D_TYPE': 1,
 'HAVE_DIRENT_H': 1,
 'HAVE_DIRFD': 1,
 'HAVE_DLFCN_H': 1,
 'HAVE_DLOPEN': 1,
 'HAVE_DUP': 1,
 'HAVE_DUP2': 1,
 'HAVE_DUP3': 1,
 'HAVE_DYLD_SHARED_CACHE_CONTAINS_PATH': 0,
 'HAVE_DYNAMIC_LOADING': 1,
 'HAVE_EDITLINE_READLINE_H': 0,
 'HAVE_ENDIAN_H': 1,
 'HAVE_EPOLL': 1,
 'HAVE_EPOLL_CREATE1': 1,
 'HAVE_ERF': 1,
 'HAVE_ERFC': 1,
 'HAVE_ERRNO_H': 1,
 'HAVE_EVENTFD': 1,
 'HAVE_EXECV': 1,
 'HAVE_EXPLICIT_BZERO': 0,
 'HAVE_EXPLICIT_MEMSET': 0,
 'HAVE_EXPM1': 1,
 'HAVE_FACCESSAT': 0,
 'HAVE_FCHDIR': 1,
 'HAVE_FCHMOD': 1,
 'HAVE_FCHMODAT': 1,
 'HAVE_FCHOWN': 1,
 'HAVE_FCHOWNAT': 1,
 'HAVE_FCNTL_H': 1,
 'HAVE_FDATASYNC': 1,
 'HAVE_FDOPENDIR': 1,
 'HAVE_FDWALK': 0,
 'HAVE_FEXECVE': 0,
 'HAVE_FFI_CLOSURE_ALLOC': 1,
 'HAVE_FFI_PREP_CIF_VAR': 1,
 'HAVE_FFI_PREP_CLOSURE_LOC': 1,
 'HAVE_FLOCK': 1,
 'HAVE_FORK': 1,
 'HAVE_FORK1': 0,
 'HAVE_FORKPTY': 1,
 'HAVE_FPATHCONF': 1,
 'HAVE_FSEEK64': 0,
 'HAVE_FSEEKO': 1,
 'HAVE_FSTATAT': 1,
 'HAVE_FSTATVFS': 1,
 'HAVE_FSYNC': 1,
 'HAVE_FTELL64': 0,
 'HAVE_FTELLO': 1,
 'HAVE_FTIME': 0,
 'HAVE_FTRUNCATE': 1,
 'HAVE_FUTIMENS': 1,
 'HAVE_FUTIMES': 0,
 'HAVE_FUTIMESAT': 0,
 'HAVE_GAI_STRERROR': 1,
 'HAVE_GCC_ASM_FOR_MC68881': 0,
 'HAVE_GCC_ASM_FOR_X64': 0,
 'HAVE_GCC_ASM_FOR_X87': 0,
 'HAVE_GCC_UINT128_T': 1,
 'HAVE_GDBM_DASH_NDBM_H': 0,
 'HAVE_GDBM_H': 1,
 'HAVE_GDBM_NDBM_H': 0,
 'HAVE_GETADDRINFO': 1,
 'HAVE_GETC_UNLOCKED': 1,
 'HAVE_GETEGID': 1,
 'HAVE_GETENTROPY': 0,
 'HAVE_GETEUID': 1,
 'HAVE_GETGID': 1,
 'HAVE_GETGRGID': 1,
 'HAVE_GETGRGID_R': 1,
 'HAVE_GETGRNAM_R': 1,
 'HAVE_GETGROUPLIST': 1,
 'HAVE_GETGROUPS': 1,
 'HAVE_GETHOSTBYADDR': 1,
 'HAVE_GETHOSTBYNAME': 1,
 'HAVE_GETHOSTBYNAME_R': 1,
 'HAVE_GETHOSTBYNAME_R_3_ARG': 0,
 'HAVE_GETHOSTBYNAME_R_5_ARG': 0,
 'HAVE_GETHOSTBYNAME_R_6_ARG': 1,
 'HAVE_GETHOSTNAME': 1,
 'HAVE_GETITIMER': 1,
 'HAVE_GETLOADAVG': 0,
 'HAVE_GETLOGIN': 1,
 'HAVE_GETNAMEINFO': 1,
 'HAVE_GETPAGESIZE': 1,
 'HAVE_GETPEERNAME': 1,
 'HAVE_GETPGID': 1,
 'HAVE_GETPGRP': 1,
 'HAVE_GETPID': 1,
 'HAVE_GETPPID': 1,
 'HAVE_GETPRIORITY': 1,
 'HAVE_GETPROTOBYNAME': 1,
 'HAVE_GETPWENT': 0,
 'HAVE_GETPWNAM_R': 1,
 'HAVE_GETPWUID': 0,
 'HAVE_GETPWUID_R': 1,
 'HAVE_GETRANDOM': 0,
 'HAVE_GETRANDOM_SYSCALL': 1,
 'HAVE_GETRESGID': 1,
 'HAVE_GETRESUID': 1,
 'HAVE_GETRUSAGE': 1,
 'HAVE_GETSERVBYNAME': 1,
 'HAVE_GETSERVBYPORT': 1,
 'HAVE_GETSID': 1,
 'HAVE_GETSOCKNAME': 1,
 'HAVE_GETSPENT': 0,
 'HAVE_GETSPNAM': 0,
 'HAVE_GETUID': 1,
 'HAVE_GETWD': 0,
 'HAVE_GLIBC_MEMMOVE_BUG': 0,
 'HAVE_GRP_H': 1,
 'HAVE_HSTRERROR': 1,
 'HAVE_HTOLE64': 1,
 'HAVE_IEEEFP_H': 0,
 'HAVE_IF_NAMEINDEX': 1,
 'HAVE_INET_ATON': 1,
 'HAVE_INET_NTOA': 1,
 'HAVE_INET_PTON': 1,
 'HAVE_INITGROUPS': 1,
 'HAVE_INTTYPES_H': 1,
 'HAVE_IO_H': 0,
 'HAVE_IPA_PURE_CONST_BUG': 0,
 'HAVE_KILL': 1,
 'HAVE_KILLPG': 1,
 'HAVE_KQUEUE': 0,
 'HAVE_LANGINFO_H': 1,
 'HAVE_LARGEFILE_SUPPORT': 0,
 'HAVE_LCHFLAGS': 0,
 'HAVE_LCHMOD': 0,
 'HAVE_LCHOWN': 1,
 'HAVE_LIBB2': 0,
 'HAVE_LIBDB': 0,
 'HAVE_LIBDL': 1,
 'HAVE_LIBDLD': 0,
 'HAVE_LIBIEEE': 0,
 'HAVE_LIBINTL_H': 1,
 'HAVE_LIBRESOLV': 0,
 'HAVE_LIBSENDFILE': 0,
 'HAVE_LIBSQLITE3': 1,
 'HAVE_LIBUTIL_H': 0,
 'HAVE_LINK': 0,
 'HAVE_LINKAT': 0,
 'HAVE_LINUX_AUXVEC_H': 1,
 'HAVE_LINUX_CAN_BCM_H': 1,
 'HAVE_LINUX_CAN_H': 1,
 'HAVE_LINUX_CAN_J1939_H': 1,
 'HAVE_LINUX_CAN_RAW_FD_FRAMES': 1,
 'HAVE_LINUX_CAN_RAW_H': 1,
 'HAVE_LINUX_CAN_RAW_JOIN_FILTERS': 1,
 'HAVE_LINUX_FS_H': 1,
 'HAVE_LINUX_LIMITS_H': 1,
 'HAVE_LINUX_MEMFD_H': 1,
 'HAVE_LINUX_NETLINK_H': 1,
 'HAVE_LINUX_QRTR_H': 1,
 'HAVE_LINUX_RANDOM_H': 1,
 'HAVE_LINUX_SOUNDCARD_H': 1,
 'HAVE_LINUX_TIPC_H': 1,
 'HAVE_LINUX_VM_SOCKETS_H': 1,
 'HAVE_LINUX_WAIT_H': 1,
 'HAVE_LISTEN': 1,
 'HAVE_LOCKF': 1,
 'HAVE_LOG1P': 1,
 'HAVE_LOG2': 1,
 'HAVE_LOGIN_TTY': 1,
 'HAVE_LONG_DOUBLE': 1,
 'HAVE_LSTAT': 1,
 'HAVE_LUTIMES': 0,
 'HAVE_LZMA_H': 0,
 'HAVE_MADVISE': 1,
 'HAVE_MAKEDEV': 1,
 'HAVE_MBRTOWC': 1,
 'HAVE_MEMFD_CREATE': 0,
 'HAVE_MEMRCHR': 1,
 'HAVE_MINIX_CONFIG_H': 0,
 'HAVE_MKDIRAT': 1,
 'HAVE_MKFIFO': 1,
 'HAVE_MKFIFOAT': 1,
 'HAVE_MKNOD': 1,
 'HAVE_MKNODAT': 1,
 'HAVE_MKTIME': 1,
 'HAVE_MMAP': 1,
 'HAVE_MREMAP': 1,
 'HAVE_NANOSLEEP': 1,
 'HAVE_NCURSESW': 1,
 'HAVE_NCURSES_H': 1,
 'HAVE_NDBM_H': 1,
 'HAVE_NDIR_H': 0,
 'HAVE_NETCAN_CAN_H': 0,
 'HAVE_NETDB_H': 1,
 'HAVE_NETINET_IN_H': 1,
 'HAVE_NETPACKET_PACKET_H': 1,
 'HAVE_NET_ETHERNET_H': 1,
 'HAVE_NET_IF_H': 1,
 'HAVE_NICE': 1,
 'HAVE_NON_UNICODE_WCHAR_T_REPRESENTATION': 0,
 'HAVE_OPENAT': 1,
 'HAVE_OPENDIR': 1,
 'HAVE_OPENPTY': 1,
 'HAVE_PANEL_H': 1,
 'HAVE_PATHCONF': 1,
 'HAVE_PAUSE': 1,
 'HAVE_PIPE': 1,
 'HAVE_PIPE2': 1,
 'HAVE_PLOCK': 0,
 'HAVE_POLL': 1,
 'HAVE_POLL_H': 1,
 'HAVE_POSIX_FADVISE': 1,
 'HAVE_POSIX_FALLOCATE': 1,
 'HAVE_POSIX_SPAWN': 0,
 'HAVE_POSIX_SPAWNP': 0,
 'HAVE_PREAD': 1,
 'HAVE_PREADV': 1,
 'HAVE_PREADV2': 0,
 'HAVE_PRLIMIT': 1,
 'HAVE_PROCESS_H': 0,
 'HAVE_PROTOTYPES': 1,
 'HAVE_PTHREAD_CONDATTR_SETCLOCK': 1,
 'HAVE_PTHREAD_DESTRUCTOR': 0,
 'HAVE_PTHREAD_GETCPUCLOCKID': 1,
 'HAVE_PTHREAD_H': 1,
 'HAVE_PTHREAD_INIT': 0,
 'HAVE_PTHREAD_KILL': 1,
 'HAVE_PTHREAD_SIGMASK': 1,
 'HAVE_PTHREAD_STUBS': 0,
 'HAVE_PTY_H': 1,
 'HAVE_PWRITE': 1,
 'HAVE_PWRITEV': 1,
 'HAVE_PWRITEV2': 0,
 'HAVE_READLINE_READLINE_H': 0,
 'HAVE_READLINK': 1,
 'HAVE_READLINKAT': 1,
 'HAVE_READV': 1,
 'HAVE_REALPATH': 1,
 'HAVE_RECVFROM': 1,
 'HAVE_RENAMEAT': 1,
 'HAVE_RL_APPEND_HISTORY': 1,
 'HAVE_RL_CATCH_SIGNAL': 1,
 'HAVE_RL_COMPDISP_FUNC_T': 1,
 'HAVE_RL_COMPLETION_APPEND_CHARACTER': 1,
 'HAVE_RL_COMPLETION_DISPLAY_MATCHES_HOOK': 1,
 'HAVE_RL_COMPLETION_MATCHES': 1,
 'HAVE_RL_COMPLETION_SUPPRESS_APPEND': 1,
 'HAVE_RL_PRE_INPUT_HOOK': 1,
 'HAVE_RL_RESIZE_TERMINAL': 1,
 'HAVE_RPC_RPC_H': 0,
 'HAVE_RTPSPAWN': 0,
 'HAVE_SCHED_GET_PRIORITY_MAX': 1,
 'HAVE_SCHED_H': 1,
 'HAVE_SCHED_RR_GET_INTERVAL': 1,
 'HAVE_SCHED_SETAFFINITY': 1,
 'HAVE_SCHED_SETPARAM': 1,
 'HAVE_SCHED_SETSCHEDULER': 1,
 'HAVE_SEM_CLOCKWAIT': 0,
 'HAVE_SEM_GETVALUE': 1,
 'HAVE_SEM_OPEN': 1,
 'HAVE_SEM_TIMEDWAIT': 1,
 'HAVE_SEM_UNLINK': 1,
 'HAVE_SENDFILE': 1,
 'HAVE_SENDTO': 1,
 'HAVE_SETEGID': 1,
 'HAVE_SETEUID': 1,
 'HAVE_SETGID': 1,
 'HAVE_SETGROUPS': 1,
 'HAVE_SETHOSTNAME': 1,
 'HAVE_SETITIMER': 1,
 'HAVE_SETJMP_H': 1,
 'HAVE_SETLOCALE': 1,
 'HAVE_SETNS': 1,
 'HAVE_SETPGID': 1,
 'HAVE_SETPGRP': 1,
 'HAVE_SETPRIORITY': 1,
 'HAVE_SETREGID': 1,
 'HAVE_SETRESGID': 1,
 'HAVE_SETRESUID': 1,
 'HAVE_SETREUID': 1,
 'HAVE_SETSID': 1,
 'HAVE_SETSOCKOPT': 1,
 'HAVE_SETUID': 1,
 'HAVE_SETVBUF': 1,
 'HAVE_SHADOW_H': 0,
 'HAVE_SHM_OPEN': 1,
 'HAVE_SHM_UNLINK': 1,
 'HAVE_SHUTDOWN': 1,
 'HAVE_SIGACTION': 1,
 'HAVE_SIGALTSTACK': 1,
 'HAVE_SIGFILLSET': 1,
 'HAVE_SIGINFO_T_SI_BAND': 1,
 'HAVE_SIGINTERRUPT': 1,
 'HAVE_SIGNAL_H': 1,
 'HAVE_SIGPENDING': 1,
 'HAVE_SIGRELSE': 0,
 'HAVE_SIGTIMEDWAIT': 1,
 'HAVE_SIGWAIT': 1,
 'HAVE_SIGWAITINFO': 1,
 'HAVE_SNPRINTF': 1,
 'HAVE_SOCKADDR_ALG': 1,
 'HAVE_SOCKADDR_SA_LEN': 0,
 'HAVE_SOCKADDR_STORAGE': 1,
 'HAVE_SOCKET': 1,
 'HAVE_SOCKETPAIR': 1,
 'HAVE_SPAWN_H': 0,
 'HAVE_SPLICE': 1,
 'HAVE_SSIZE_T': 1,
 'HAVE_STATVFS': 1,
 'HAVE_STAT_TV_NSEC': 1,
 'HAVE_STAT_TV_NSEC2': 0,
 'HAVE_STDINT_H': 1,
 'HAVE_STDIO_H': 1,
 'HAVE_STDLIB_H': 1,
 'HAVE_STD_ATOMIC': 1,
 'HAVE_STRFTIME': 1,
 'HAVE_STRINGS_H': 1,
 'HAVE_STRING_H': 1,
 'HAVE_STRLCPY': 1,
 'HAVE_STROPTS_H': 0,
 'HAVE_STRSIGNAL': 1,
 'HAVE_STRUCT_PASSWD_PW_GECOS': 1,
 'HAVE_STRUCT_PASSWD_PW_PASSWD': 1,
 'HAVE_STRUCT_STAT_ST_BIRTHTIME': 0,
 'HAVE_STRUCT_STAT_ST_BLKSIZE': 1,
 'HAVE_STRUCT_STAT_ST_BLOCKS': 1,
 'HAVE_STRUCT_STAT_ST_FLAGS': 0,
 'HAVE_STRUCT_STAT_ST_GEN': 0,
 'HAVE_STRUCT_STAT_ST_RDEV': 1,
 'HAVE_STRUCT_TM_TM_ZONE': 1,
 'HAVE_SYMLINK': 1,
 'HAVE_SYMLINKAT': 1,
 'HAVE_SYNC': 1,
 'HAVE_SYSCONF': 1,
 'HAVE_SYSEXITS_H': 1,
 'HAVE_SYSLOG_H': 1,
 'HAVE_SYSTEM': 1,
 'HAVE_SYS_AUDIOIO_H': 0,
 'HAVE_SYS_AUXV_H': 1,
 'HAVE_SYS_BSDTTY_H': 0,
 'HAVE_SYS_DEVPOLL_H': 0,
 'HAVE_SYS_DIR_H': 0,
 'HAVE_SYS_ENDIAN_H': 1,
 'HAVE_SYS_EPOLL_H': 1,
 'HAVE_SYS_EVENTFD_H': 1,
 'HAVE_SYS_EVENT_H': 0,
 'HAVE_SYS_FILE_H': 1,
 'HAVE_SYS_IOCTL_H': 1,
 'HAVE_SYS_KERN_CONTROL_H': 0,
 'HAVE_SYS_LOADAVG_H': 0,
 'HAVE_SYS_LOCK_H': 0,
 'HAVE_SYS_MEMFD_H': 0,
 'HAVE_SYS_MKDEV_H': 0,
 'HAVE_SYS_MMAN_H': 1,
 'HAVE_SYS_MODEM_H': 0,
 'HAVE_SYS_NDIR_H': 0,
 'HAVE_SYS_PARAM_H': 1,
 'HAVE_SYS_PIDFD_H': 1,
 'HAVE_SYS_POLL_H': 1,
 'HAVE_SYS_RANDOM_H': 1,
 'HAVE_SYS_RESOURCE_H': 1,
 'HAVE_SYS_SELECT_H': 1,
 'HAVE_SYS_SENDFILE_H': 1,
 'HAVE_SYS_SOCKET_H': 1,
 'HAVE_SYS_SOUNDCARD_H': 0,
 'HAVE_SYS_STATVFS_H': 1,
 'HAVE_SYS_STAT_H': 1,
 'HAVE_SYS_SYSCALL_H': 1,
 'HAVE_SYS_SYSMACROS_H': 1,
 'HAVE_SYS_SYS_DOMAIN_H': 0,
 'HAVE_SYS_TERMIO_H': 0,
 'HAVE_SYS_TIMES_H': 1,
 'HAVE_SYS_TIME_H': 1,
 'HAVE_SYS_TYPES_H': 1,
 'HAVE_SYS_UIO_H': 1,
 'HAVE_SYS_UN_H': 1,
 'HAVE_SYS_UTSNAME_H': 1,
 'HAVE_SYS_WAIT_H': 1,
 'HAVE_SYS_XATTR_H': 1,
 'HAVE_TCGETPGRP': 1,
 'HAVE_TCSETPGRP': 1,
 'HAVE_TEMPNAM': 1,
 'HAVE_TERMIOS_H': 1,
 'HAVE_TERM_H': 1,
 'HAVE_TIMEGM': 1,
 'HAVE_TIMES': 1,
 'HAVE_TMPFILE': 1,
 'HAVE_TMPNAM': 1,
 'HAVE_TMPNAM_R': 0,
 'HAVE_TM_ZONE': 1,
 'HAVE_TRUNCATE': 1,
 'HAVE_TTYNAME_R': 1,
 'HAVE_TZNAME': 0,
 'HAVE_UMASK': 1,
 'HAVE_UNAME': 1,
 'HAVE_UNISTD_H': 1,
 'HAVE_UNLINKAT': 1,
 'HAVE_UNSHARE': 1,
 'HAVE_USABLE_WCHAR_T': 0,
 'HAVE_UTIL_H': 0,
 'HAVE_UTIMENSAT': 1,
 'HAVE_UTIMES': 1,
 'HAVE_UTIME_H': 1,
 'HAVE_UTMP_H': 1,
 'HAVE_UUID_CREATE': 0,
 'HAVE_UUID_ENC_BE': 0,
 'HAVE_UUID_GENERATE_TIME_SAFE': 0,
 'HAVE_UUID_H': 0,
 'HAVE_UUID_UUID_H': 0,
 'HAVE_VFORK': 1,
 'HAVE_WAIT': 1,
 'HAVE_WAIT3': 0,
 'HAVE_WAIT4': 1,
 'HAVE_WAITID': 1,
 'HAVE_WAITPID': 1,
 'HAVE_WCHAR_H': 1,
 'HAVE_WCSCOLL': 1,
 'HAVE_WCSFTIME': 0,
 'HAVE_WCSXFRM': 1,
 'HAVE_WMEMCMP': 1,
 'HAVE_WORKING_TZSET': 1,
 'HAVE_WRITEV': 1,
 'HAVE_ZLIB_COPY': 1,
 'HAVE_ZLIB_H': 0,
 'HAVE__GETPTY': 0,
 'HOSTRUNNER': '',
 'HOST_GNU_TYPE': 'aarch64-unknown-linux-android',
 'INCLDIRSTOMAKE': '/data/data/com.termux/files/usr/include '
                   '/data/data/com.termux/files/usr/include '
                   '/data/data/com.termux/files/usr/include/python3.12 '
                   '/data/data/com.termux/files/usr/include/python3.12',
 'INCLUDEDIR': '/data/data/com.termux/files/usr/include',
 'INCLUDEPY': '/data/data/com.termux/files/usr/include/python3.12',
 'INSTALL': '/usr/bin/install -c',
 'INSTALL_DATA': '/usr/bin/install -c -m 644',
 'INSTALL_PROGRAM': '/usr/bin/install -c',
 'INSTALL_SCRIPT': '/usr/bin/install -c',
 'INSTALL_SHARED': '/usr/bin/install -c -m 755',
 'INSTSONAME': 'libpython3.12.so.1.0',
 'IO_H': 'Modules/_io/_iomodule.h',
 'IO_OBJS': '\\',
 'LDCXXSHARED': 'aarch64-linux-android-clang++ -shared '
                '-L/data/data/com.termux/files/usr/lib '
                '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
                '-Wl,--no-as-needed,-landroid-support,--as-needed '
                '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                '-landroid-posix-semaphore '
                '-L/data/data/com.termux/files/usr/lib '
                '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
                '-Wl,--no-as-needed,-landroid-support,--as-needed '
                '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                '-landroid-posix-semaphore',
 'LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
            '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
            '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
            '-Wl,--no-as-needed,-landroid-support,--as-needed '
            '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
            '-landroid-posix-semaphore -L/data/data/com.termux/files/usr/lib '
            '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
            '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
            '-Wl,--no-as-needed,-landroid-support,--as-needed '
            '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
            '-landroid-posix-semaphore',
 'LDFLAGS_NODIST': '',
 'LDLIBRARY': 'libpython3.12.so',
 'LDLIBRARYDIR': '',
 'LDSHARED': 'aarch64-linux-android-clang -shared '
             '-L/data/data/com.termux/files/usr/lib '
             '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
             '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
             '-Wl,--no-as-needed,-landroid-support,--as-needed '
             '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
             '-landroid-posix-semaphore -L/data/data/com.termux/files/usr/lib '
             '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
             '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
             '-Wl,--no-as-needed,-landroid-support,--as-needed '
             '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
             '-landroid-posix-semaphore',
 'LDVERSION': '3.12',
 'LIBC': '',
 'LIBDEST': '/data/data/com.termux/files/usr/lib/python3.12',
 'LIBDIR': '/data/data/com.termux/files/usr/lib',
 'LIBEXPAT_A': 'Modules/expat/libexpat.a',
 'LIBEXPAT_CFLAGS': '-fno-strict-overflow -Wsign-compare -Wunreachable-code '
                    '-DNDEBUG -g -O3 -Wall -fstack-protector-strong -O3  '
                    '-fstack-protector-strong -O3 -std=c11 -Wextra '
                    '-Wno-unused-parameter -Wno-missing-field-initializers '
                    '-Wstrict-prototypes -Werror=implicit-function-declaration '
                    '-fvisibility=hidden  '
                    '-I/home/builder/.termux-build/python/src/Include/internal '
                    '-IObjects -IInclude -IPython -I. '
                    '-I/home/builder/.termux-build/python/src/Include '
                    '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                    '-isystem/data/data/com.termux/files/usr/include '
                    '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                    '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                    '-isystem/data/data/com.termux/files/usr/include '
                    '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                    '-fPIC -fPIC',
 'LIBEXPAT_HEADERS': '\\',
 'LIBEXPAT_OBJS': '\\',
 'LIBHACL_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_hacl/include '
                   '-D_BSD_SOURCE -D_DEFAULT_SOURCE -fno-strict-overflow '
                   '-Wsign-compare -Wunreachable-code -DNDEBUG -g -O3 -Wall '
                   '-fstack-protector-strong -O3  -fstack-protector-strong -O3 '
                   '-std=c11 -Wextra -Wno-unused-parameter '
                   '-Wno-missing-field-initializers -Wstrict-prototypes '
                   '-Werror=implicit-function-declaration -fvisibility=hidden  '
                   '-I/home/builder/.termux-build/python/src/Include/internal '
                   '-IObjects -IInclude -IPython -I. '
                   '-I/home/builder/.termux-build/python/src/Include '
                   '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                   '-isystem/data/data/com.termux/files/usr/include '
                   '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                   '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                   '-isystem/data/data/com.termux/files/usr/include '
                   '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                   '-fPIC -fPIC',
 'LIBHACL_HEADERS': '\\',
 'LIBHACL_SHA2_A': 'Modules/_hacl/libHacl_Hash_SHA2.a',
 'LIBHACL_SHA2_HEADERS': '\\',
 'LIBHACL_SHA2_OBJS': '\\',
 'LIBM': '-lm',
 'LIBMPDEC_A': 'Modules/_decimal/libmpdec/libmpdec.a',
 'LIBMPDEC_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_decimal/libmpdec '
                    '-DCONFIG_64=1 -DANSI=1 -DHAVE_UINT128_T=1 '
                    '-fno-strict-overflow -Wsign-compare -Wunreachable-code '
                    '-DNDEBUG -g -O3 -Wall -fstack-protector-strong -O3  '
                    '-fstack-protector-strong -O3 -std=c11 -Wextra '
                    '-Wno-unused-parameter -Wno-missing-field-initializers '
                    '-Wstrict-prototypes -Werror=implicit-function-declaration '
                    '-fvisibility=hidden  '
                    '-I/home/builder/.termux-build/python/src/Include/internal '
                    '-IObjects -IInclude -IPython -I. '
                    '-I/home/builder/.termux-build/python/src/Include '
                    '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                    '-isystem/data/data/com.termux/files/usr/include '
                    '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                    '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                    '-isystem/data/data/com.termux/files/usr/include '
                    '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                    '-fPIC -fPIC',
 'LIBMPDEC_HEADERS': '\\',
 'LIBMPDEC_OBJS': '\\',
 'LIBOBJDIR': 'Python/',
 'LIBOBJS': '',
 'LIBPC': '/data/data/com.termux/files/usr/lib/pkgconfig',
 'LIBPL': '/data/data/com.termux/files/usr/lib/python3.12/config-3.12',
 'LIBPYTHON': '-lpython3.12',
 'LIBRARY': 'libpython3.12.a',
 'LIBRARY_DEPS': 'libpython3.12.a libpython3.12.so libpython3.so',
 'LIBRARY_OBJS': '\\',
 'LIBRARY_OBJS_OMIT_FROZEN': '\\',
 'LIBS': '-ldl  -lpthread',
 'LIBSUBDIRS': 'asyncio \\',
 'LINKCC': 'aarch64-linux-android-clang',
 'LINKFORSHARED': '-pie -Xlinker -export-dynamic',
 'LINK_PYTHON_DEPS': 'libpython3.12.a libpython3.12.so libpython3.so',
 'LINK_PYTHON_OBJS': '-L. -lpython3.12',
 'LIPO_32BIT_FLAGS': '',
 'LIPO_INTEL64_FLAGS': '',
 'LLVM_PROF_ERR': 'no',
 'LLVM_PROF_FILE': 'LLVM_PROFILE_FILE="$(shell pwd)/code-%p.profclangr"',
 'LLVM_PROF_MERGER': '/home/builder/.termux-build/_cache/android-r28c-api-24-v2/bin/llvm-profdata '
                     'merge -output="$(shell pwd)/code.profclangd" "$(shell '
                     'pwd)"/*.profclangr',
 'LN': 'ln',
 'LOCALMODLIBS': '',
 'MACHDEP': 'linux',
 'MACHDEP_OBJS': '',
 'MACHDESTLIB': '/data/data/com.termux/files/usr/lib/python3.12',
 'MACOSX_DEPLOYMENT_TARGET': '',
 'MAJOR_IN_MKDEV': 0,
 'MAJOR_IN_SYSMACROS': 1,
 'MAKESETUP': '/home/builder/.termux-build/python/src/Modules/makesetup',
 'MANDIR': '/data/data/com.termux/files/usr/share/man',
 'MKDIR_P': '/usr/bin/mkdir -p',
 'MODBUILT_NAMES': 'array  _asyncio  _bisect  _contextvars  _csv  _heapq  '
                   '_json  _lsprof  _opcode  _pickle  _queue  _random  '
                   '_struct  _xxsubinterpreters  _xxinterpchannels  _zoneinfo  '
                   'audioop  math  cmath  _statistics  _datetime  _decimal  '
                   'binascii  _bz2  _lzma  zlib  _dbm  _gdbm  readline  _md5  '
                   '_sha1  _sha2  _sha3  _blake2  pyexpat  _elementtree  '
                   '_codecs_cn  _codecs_hk  _codecs_iso2022  _codecs_jp  '
                   '_codecs_kr  _codecs_tw  _multibytecodec  unicodedata  '
                   '_crypt  fcntl  grp  mmap  ossaudiodev  _posixsubprocess  '
                   'resource  select  _socket  syslog  termios  _posixshmem  '
                   '_multiprocessing  _ctypes  _curses  _curses_panel  '
                   '_sqlite3  _ssl  _hashlib  _tkinter  xxsubtype  '
                   '_xxtestfuzz  _testbuffer  _testinternalcapi  _testcapi  '
                   '_testclinic  _testimportmultiple  _testmultiphase  '
                   '_testsinglephase  _ctypes_test  xxlimited  xxlimited_35  '
                   'atexit  faulthandler  posix  _signal  _tracemalloc  '
                   '_codecs  _collections  errno  _io  itertools  _sre  '
                   '_thread  time  _typing  _weakref  _abc  _functools  '
                   '_locale  _operator  _stat  _symtable  pwd',
 'MODDISABLED_NAMES': '',
 'MODLIBS': '',
 'MODOBJS': 'Modules/atexitmodule.o  Modules/faulthandler.o  '
            'Modules/posixmodule.o  Modules/signalmodule.o  '
            'Modules/_tracemalloc.o  Modules/_codecsmodule.o  '
            'Modules/_collectionsmodule.o  Modules/errnomodule.o  '
            'Modules/_io/_iomodule.o Modules/_io/iobase.o Modules/_io/fileio.o '
            'Modules/_io/bytesio.o Modules/_io/bufferedio.o '
            'Modules/_io/textio.o Modules/_io/stringio.o  '
            'Modules/itertoolsmodule.o  Modules/_sre/sre.o  '
            'Modules/_threadmodule.o  Modules/timemodule.o  '
            'Modules/_typingmodule.o  Modules/_weakref.o  Modules/_abc.o  '
            'Modules/_functoolsmodule.o  Modules/_localemodule.o  '
            'Modules/_operator.o  Modules/_stat.o  Modules/symtablemodule.o  '
            'Modules/pwdmodule.o',
 'MODSHARED_NAMES': 'array _asyncio _bisect _contextvars _csv _heapq _json '
                    '_lsprof _opcode _pickle _queue _random _struct '
                    '_xxsubinterpreters _xxinterpchannels _zoneinfo audioop '
                    'math cmath _statistics _datetime _decimal binascii _bz2 '
                    '_lzma zlib _dbm _gdbm readline _md5 _sha1 _sha2 _sha3 '
                    '_blake2 pyexpat _elementtree _codecs_cn _codecs_hk '
                    '_codecs_iso2022 _codecs_jp _codecs_kr _codecs_tw '
                    '_multibytecodec unicodedata _crypt fcntl grp mmap '
                    'ossaudiodev _posixsubprocess resource select _socket '
                    'syslog termios _posixshmem _multiprocessing _ctypes '
                    '_curses _curses_panel _sqlite3 _ssl _hashlib _tkinter '
                    'xxsubtype _xxtestfuzz _testbuffer _testinternalcapi '
                    '_testcapi _testclinic _testimportmultiple _testmultiphase '
                    '_testsinglephase _ctypes_test xxlimited xxlimited_35',
 'MODULE_ARRAY_STATE': 'yes',
 'MODULE_ATEXIT_LDFLAGS': '',
 'MODULE_AUDIOOP_LDFLAGS': '-lm',
 'MODULE_AUDIOOP_STATE': 'yes',
 'MODULE_BINASCII_CFLAGS': '-DUSE_ZLIB_CRC32 '
                           '-I/data/data/com.termux/files/usr/include',
 'MODULE_BINASCII_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -lz',
 'MODULE_BINASCII_STATE': 'yes',
 'MODULE_CMATH_DEPS': '/home/builder/.termux-build/python/src/Modules/_math.h',
 'MODULE_CMATH_LDFLAGS': '-lm',
 'MODULE_CMATH_STATE': 'yes',
 'MODULE_DEPS_SHARED': 'Modules/config.c',
 'MODULE_DEPS_STATIC': 'Modules/config.c',
 'MODULE_ERRNO_LDFLAGS': '',
 'MODULE_FAULTHANDLER_LDFLAGS': '',
 'MODULE_FCNTL_LDFLAGS': '',
 'MODULE_FCNTL_STATE': 'yes',
 'MODULE_GRP_STATE': 'yes',
 'MODULE_ITERTOOLS_LDFLAGS': '',
 'MODULE_MATH_DEPS': '/home/builder/.termux-build/python/src/Modules/_math.h',
 'MODULE_MATH_LDFLAGS': '-lm',
 'MODULE_MATH_STATE': 'yes',
 'MODULE_MMAP_STATE': 'yes',
 'MODULE_NIS_STATE': 'missing',
 'MODULE_OBJS': '\\',
 'MODULE_OSSAUDIODEV_LDFLAGS': '',
 'MODULE_OSSAUDIODEV_STATE': 'yes',
 'MODULE_POSIX_LDFLAGS': '',
 'MODULE_PWD_LDFLAGS': '',
 'MODULE_PWD_STATE': 'yes',
 'MODULE_PYEXPAT_CFLAGS': '',
 'MODULE_PYEXPAT_DEPS': '',
 'MODULE_PYEXPAT_LDFLAGS': '-lexpat',
 'MODULE_PYEXPAT_STATE': 'yes',
 'MODULE_READLINE_CFLAGS': '-I/data/data/com.termux/files/usr/include '
                           '-DNCURSES_WIDECHAR',
 'MODULE_READLINE_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -lreadline',
 'MODULE_READLINE_STATE': 'yes',
 'MODULE_RESOURCE_STATE': 'yes',
 'MODULE_SELECT_STATE': 'yes',
 'MODULE_SPWD_STATE': 'missing',
 'MODULE_SYSLOG_STATE': 'yes',
 'MODULE_TERMIOS_STATE': 'yes',
 'MODULE_TIME_LDFLAGS': '',
 'MODULE_TIME_STATE': 'yes',
 'MODULE_UNICODEDATA_DEPS': '/home/builder/.termux-build/python/src/Modules/unicodedata_db.h '
                            '/home/builder/.termux-build/python/src/Modules/unicodename_db.h',
 'MODULE_UNICODEDATA_STATE': 'yes',
 'MODULE_XXLIMITED_35_STATE': 'yes',
 'MODULE_XXLIMITED_STATE': 'yes',
 'MODULE_XXSUBTYPE_STATE': 'yes',
 'MODULE_ZLIB_CFLAGS': '-I/data/data/com.termux/files/usr/include',
 'MODULE_ZLIB_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -lz',
 'MODULE_ZLIB_STATE': 'yes',
 'MODULE__ABC_LDFLAGS': '',
 'MODULE__ASYNCIO_STATE': 'yes',
 'MODULE__BISECT_STATE': 'yes',
 'MODULE__BLAKE2_CFLAGS': '',
 'MODULE__BLAKE2_DEPS': '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2-config.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2-impl.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2b-load-sse2.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2b-load-sse41.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2b-ref.c '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2b-round.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2b.c '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s-load-sse2.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s-load-sse41.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s-load-xop.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s-ref.c '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s-round.h '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/impl/blake2s.c '
                        '/home/builder/.termux-build/python/src/Modules/_blake2/blake2module.h '
                        '/home/builder/.termux-build/python/src/Modules/hashlib.h',
 'MODULE__BLAKE2_LDFLAGS': '',
 'MODULE__BLAKE2_STATE': 'yes',
 'MODULE__BZ2_CFLAGS': '',
 'MODULE__BZ2_LDFLAGS': '-lbz2',
 'MODULE__BZ2_STATE': 'yes',
 'MODULE__CODECS_CN_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_cn.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_CN_STATE': 'yes',
 'MODULE__CODECS_HK_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_hk.h  '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_HK_STATE': 'yes',
 'MODULE__CODECS_ISO2022_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_jisx0213_pair.h '
                                '/home/builder/.termux-build/python/src/Modules/cjkcodecs/alg_jisx0201.h '
                                '/home/builder/.termux-build/python/src/Modules/cjkcodecs/emu_jisx0213_2000.h '
                                '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                                '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_ISO2022_STATE': 'yes',
 'MODULE__CODECS_JP_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_jisx0213_pair.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/alg_jisx0201.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/emu_jisx0213_2000.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_jp.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_JP_STATE': 'yes',
 'MODULE__CODECS_KR_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_kr.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_KR_STATE': 'yes',
 'MODULE__CODECS_LDFLAGS': '',
 'MODULE__CODECS_TW_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/mappings_tw.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h '
                           '/home/builder/.termux-build/python/src/Modules/cjkcodecs/cjkcodecs.h',
 'MODULE__CODECS_TW_STATE': 'yes',
 'MODULE__COLLECTIONS_LDFLAGS': '',
 'MODULE__CONTEXTVARS_STATE': 'yes',
 'MODULE__CRYPT_CFLAGS': '',
 'MODULE__CRYPT_LDFLAGS': '-lcrypt',
 'MODULE__CRYPT_STATE': 'yes',
 'MODULE__CSV_STATE': 'yes',
 'MODULE__CTYPES_CFLAGS': '-fno-strict-overflow '
                          '-I/data/data/com.termux/files/usr/include',
 'MODULE__CTYPES_DEPS': '/home/builder/.termux-build/python/src/Modules/_ctypes/ctypes.h',
 'MODULE__CTYPES_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -lffi -ldl',
 'MODULE__CTYPES_MALLOC_CLOSURE': '',
 'MODULE__CTYPES_STATE': 'yes',
 'MODULE__CTYPES_TEST_LDFLAGS': '-lm',
 'MODULE__CTYPES_TEST_STATE': 'yes',
 'MODULE__CURSES_CFLAGS': '-DNCURSES_WIDECHAR '
                          '-I/data/data/com.termux/files/usr/include',
 'MODULE__CURSES_LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
                           '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                           '-Wl,--enable-new-dtags -lncursesw',
 'MODULE__CURSES_PANEL_CFLAGS': '-I/data/data/com.termux/files/usr/include '
                                '-DNCURSES_WIDECHAR -DNCURSES_WIDECHAR '
                                '-I/data/data/com.termux/files/usr/include',
 'MODULE__CURSES_PANEL_LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
                                 '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                                 '-Wl,--enable-new-dtags -lpanelw '
                                 '-L/data/data/com.termux/files/usr/lib '
                                 '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                                 '-Wl,--enable-new-dtags -lncursesw',
 'MODULE__CURSES_PANEL_STATE': 'yes',
 'MODULE__CURSES_STATE': 'yes',
 'MODULE__DATETIME_LDFLAGS': '-lm',
 'MODULE__DATETIME_STATE': 'yes',
 'MODULE__DBM_CFLAGS': '-DUSE_NDBM',
 'MODULE__DBM_LDFLAGS': '-lgdbm_compat',
 'MODULE__DBM_STATE': 'yes',
 'MODULE__DECIMAL_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_decimal/libmpdec '
                           '-DCONFIG_64=1 -DANSI=1 -DHAVE_UINT128_T=1',
 'MODULE__DECIMAL_DEPS': '/home/builder/.termux-build/python/src/Modules/_decimal/docstrings.h '
                         '\\ Modules/_decimal/libmpdec/libmpdec.a',
 'MODULE__DECIMAL_LDFLAGS': '-lm Modules/_decimal/libmpdec/libmpdec.a',
 'MODULE__DECIMAL_STATE': 'yes',
 'MODULE__ELEMENTTREE_CFLAGS': '',
 'MODULE__ELEMENTTREE_DEPS': '/home/builder/.termux-build/python/src/Modules/pyexpat.c',
 'MODULE__ELEMENTTREE_STATE': 'yes',
 'MODULE__FUNCTOOLS_LDFLAGS': '',
 'MODULE__GDBM_CFLAGS': '',
 'MODULE__GDBM_LDFLAGS': '-lgdbm',
 'MODULE__GDBM_STATE': 'yes',
 'MODULE__HASHLIB_CFLAGS': '-I/data/data/com.termux/files/usr/include',
 'MODULE__HASHLIB_DEPS': '/home/builder/.termux-build/python/src/Modules/hashlib.h',
 'MODULE__HASHLIB_LDFLAGS': '-L/data/data/com.termux/files/usr/lib   -lcrypto',
 'MODULE__HASHLIB_STATE': 'yes',
 'MODULE__HEAPQ_STATE': 'yes',
 'MODULE__IO_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_io',
 'MODULE__IO_DEPS': '/home/builder/.termux-build/python/src/Modules/_io/_iomodule.h',
 'MODULE__IO_LDFLAGS': '',
 'MODULE__IO_STATE': 'yes',
 'MODULE__JSON_STATE': 'yes',
 'MODULE__LOCALE_LDFLAGS': '',
 'MODULE__LSPROF_STATE': 'yes',
 'MODULE__LZMA_CFLAGS': '-I/data/data/com.termux/files/usr/include',
 'MODULE__LZMA_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -llzma',
 'MODULE__LZMA_STATE': 'yes',
 'MODULE__MD5_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_hacl/include '
                       '-I/home/builder/.termux-build/python/src/Modules/_hacl/internal '
                       '-D_BSD_SOURCE -D_DEFAULT_SOURCE',
 'MODULE__MD5_DEPS': '/home/builder/.termux-build/python/src/Modules/hashlib.h '
                     '\\ Modules/_hacl/Hacl_Hash_MD5.h '
                     'Modules/_hacl/Hacl_Hash_MD5.c',
 'MODULE__MD5_STATE': 'yes',
 'MODULE__MULTIBYTECODEC_DEPS': '/home/builder/.termux-build/python/src/Modules/cjkcodecs/multibytecodec.h',
 'MODULE__MULTIBYTECODEC_STATE': 'yes',
 'MODULE__MULTIPROCESSING_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_multiprocessing',
 'MODULE__MULTIPROCESSING_STATE': 'yes',
 'MODULE__OPCODE_STATE': 'yes',
 'MODULE__OPERATOR_LDFLAGS': '',
 'MODULE__PICKLE_STATE': 'yes',
 'MODULE__POSIXSHMEM_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_multiprocessing',
 'MODULE__POSIXSHMEM_LDFLAGS': '',
 'MODULE__POSIXSHMEM_STATE': 'yes',
 'MODULE__POSIXSUBPROCESS_STATE': 'yes',
 'MODULE__QUEUE_STATE': 'yes',
 'MODULE__RANDOM_STATE': 'yes',
 'MODULE__SCPROXY_STATE': 'n/a',
 'MODULE__SHA1_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_hacl/include '
                        '-I/home/builder/.termux-build/python/src/Modules/_hacl/internal '
                        '-D_BSD_SOURCE -D_DEFAULT_SOURCE',
 'MODULE__SHA1_DEPS': '/home/builder/.termux-build/python/src/Modules/hashlib.h '
                      '\\ Modules/_hacl/Hacl_Hash_SHA1.h '
                      'Modules/_hacl/Hacl_Hash_SHA1.c',
 'MODULE__SHA1_STATE': 'yes',
 'MODULE__SHA2_CFLAGS': '-I/home/builder/.termux-build/python/src/Modules/_hacl/include '
                        '-I/home/builder/.termux-build/python/src/Modules/_hacl/internal '
                        '-D_BSD_SOURCE -D_DEFAULT_SOURCE',
 'MODULE__SHA2_DEPS': '/home/builder/.termux-build/python/src/Modules/hashlib.h '
                      '\\ Modules/_hacl/libHacl_Hash_SHA2.a',
 'MODULE__SHA2_STATE': 'yes',
 'MODULE__SHA3_DEPS': '/home/builder/.termux-build/python/src/Modules/hashlib.h '
                      '\\ Modules/_hacl/Hacl_Hash_SHA3.h '
                      'Modules/_hacl/Hacl_Hash_SHA3.c',
 'MODULE__SHA3_STATE': 'yes',
 'MODULE__SIGNAL_LDFLAGS': '',
 'MODULE__SOCKET_DEPS': '/home/builder/.termux-build/python/src/Modules/socketmodule.h '
                        '/home/builder/.termux-build/python/src/Modules/addrinfo.h '
                        '/home/builder/.termux-build/python/src/Modules/getaddrinfo.c '
                        '/home/builder/.termux-build/python/src/Modules/getnameinfo.c',
 'MODULE__SOCKET_STATE': 'yes',
 'MODULE__SQLITE3_CFLAGS': '-I/data/data/com.termux/files/usr/include '
                           '-I/home/builder/.termux-build/python/src/Modules/_sqlite',
 'MODULE__SQLITE3_DEPS': '/home/builder/.termux-build/python/src/Modules/_sqlite/connection.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/cursor.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/microprotocols.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/module.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/prepare_protocol.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/row.h '
                         '/home/builder/.termux-build/python/src/Modules/_sqlite/util.h',
 'MODULE__SQLITE3_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -lsqlite3',
 'MODULE__SQLITE3_STATE': 'yes',
 'MODULE__SRE_LDFLAGS': '',
 'MODULE__SSL_CFLAGS': '-I/data/data/com.termux/files/usr/include',
 'MODULE__SSL_DEPS': '/home/builder/.termux-build/python/src/Modules/_ssl.h '
                     '/home/builder/.termux-build/python/src/Modules/_ssl/cert.c '
                     '/home/builder/.termux-build/python/src/Modules/_ssl/debughelpers.c '
                     '/home/builder/.termux-build/python/src/Modules/_ssl/misc.c '
                     '/home/builder/.termux-build/python/src/Modules/_ssl_data.h '
                     '/home/builder/.termux-build/python/src/Modules/_ssl_data_111.h '
                     '/home/builder/.termux-build/python/src/Modules/_ssl_data_300.h '
                     '/home/builder/.termux-build/python/src/Modules/socketmodule.h',
 'MODULE__SSL_LDFLAGS': '-L/data/data/com.termux/files/usr/lib  -lssl -lcrypto',
 'MODULE__SSL_STATE': 'yes',
 'MODULE__STATISTICS_LDFLAGS': '-lm',
 'MODULE__STATISTICS_STATE': 'yes',
 'MODULE__STAT_LDFLAGS': '',
 'MODULE__STRUCT_STATE': 'yes',
 'MODULE__SYMTABLE_LDFLAGS': '',
 'MODULE__TESTBUFFER_STATE': 'yes',
 'MODULE__TESTCAPI_DEPS': '/home/builder/.termux-build/python/src/Modules/_testcapi/testcapi_long.h '
                          '/home/builder/.termux-build/python/src/Modules/_testcapi/parts.h '
                          '/home/builder/.termux-build/python/src/Modules/_testcapi/util.h',
 'MODULE__TESTCAPI_STATE': 'yes',
 'MODULE__TESTCLINIC_STATE': 'yes',
 'MODULE__TESTIMPORTMULTIPLE_STATE': 'yes',
 'MODULE__TESTINTERNALCAPI_STATE': 'yes',
 'MODULE__TESTMULTIPHASE_STATE': 'yes',
 'MODULE__THREAD_LDFLAGS': '',
 'MODULE__TKINTER_CFLAGS': '-I/data/data/com.termux/files/usr/include '
                           '-Wno-strict-prototypes -DWITH_APPINIT=1',
 'MODULE__TKINTER_LDFLAGS': '-L/data/data/com.termux/files/usr/lib -ltk8.6 '
                            '-ltkstub8.6 -ltcl8.6 -ltclstub8.6',
 'MODULE__TKINTER_STATE': 'yes',
 'MODULE__TRACEMALLOC_LDFLAGS': '',
 'MODULE__TYPING_LDFLAGS': '',
 'MODULE__TYPING_STATE': 'yes',
 'MODULE__UUID_STATE': 'missing',
 'MODULE__WEAKREF_LDFLAGS': '',
 'MODULE__XXINTERPCHANNELS_STATE': 'yes',
 'MODULE__XXSUBINTERPRETERS_STATE': 'yes',
 'MODULE__XXTESTFUZZ_STATE': 'yes',
 'MODULE__ZONEINFO_STATE': 'yes',
 'MULTIARCH': '',
 'MULTIARCH_CPPFLAGS': '',
 'MVWDELCH_IS_EXPRESSION': 1,
 'NO_AS_NEEDED': '-Wl,--no-as-needed',
 'OBJECT_OBJS': '\\',
 'OPT': '-DNDEBUG -g -O3 -Wall',
 'PACKAGE_BUGREPORT': 0,
 'PACKAGE_NAME': 0,
 'PACKAGE_STRING': 0,
 'PACKAGE_TARNAME': 0,
 'PACKAGE_URL': 0,
 'PACKAGE_VERSION': 0,
 'PARSER_HEADERS': '\\',
 'PARSER_OBJS': '\\ \\ Parser/myreadline.o Parser/tokenizer.o',
 'PEGEN_HEADERS': '\\',
 'PEGEN_OBJS': '\\',
 'PGO_PROF_GEN_FLAG': '-fprofile-instr-generate',
 'PGO_PROF_USE_FLAG': '-fprofile-instr-use="$(shell pwd)/code.profclangd"',
 'PLATLIBDIR': 'lib',
 'POBJS': '\\',
 'POSIX_SEMAPHORES_NOT_ENABLED': 0,
 'PROFILE_TASK': '-m test --pgo --timeout=1200',
 'PTHREAD_KEY_T_IS_COMPATIBLE_WITH_INT': 1,
 'PTHREAD_SYSTEM_SCHED_SUPPORTED': 0,
 'PURIFY': '',
 'PY3LIBRARY': 'libpython3.so',
 'PYLONG_BITS_IN_DIGIT': 0,
 'PYTHON': 'python',
 'PYTHONFRAMEWORK': '',
 'PYTHONFRAMEWORKDIR': 'no-framework',
 'PYTHONFRAMEWORKINSTALLDIR': '',
 'PYTHONFRAMEWORKPREFIX': '',
 'PYTHONPATH': '',
 'PYTHON_FOR_BUILD': '_PYTHON_PROJECT_BASE=/home/builder/.termux-build/python/build '
                     '_PYTHON_HOST_PLATFORM=$(_PYTHON_HOST_PLATFORM) '
                     'PYTHONPATH=$(shell test -f pybuilddir.txt && echo '
                     '/home/builder/.termux-build/python/build/`cat '
                     'pybuilddir.txt`:)/home/builder/.termux-build/python/src/Lib '
                     '_PYTHON_SYSCONFIGDATA_NAME=_sysconfigdata__linux_ '
                     'python3.12',
 'PYTHON_FOR_BUILD_DEPS': '',
 'PYTHON_FOR_FREEZE': 'python3.12',
 'PYTHON_FOR_REGEN': '',
 'PYTHON_HEADERS': '\\',
 'PYTHON_OBJS': '\\',
 'PY_BUILTIN_HASHLIB_HASHES': '"md5,sha1,sha2,sha3,blake2"',
 'PY_BUILTIN_MODULE_CFLAGS': '-fno-strict-overflow -Wsign-compare '
                             '-Wunreachable-code -DNDEBUG -g -O3 -Wall '
                             '-fstack-protector-strong -O3  '
                             '-fstack-protector-strong -O3 -std=c11 -Wextra '
                             '-Wno-unused-parameter '
                             '-Wno-missing-field-initializers '
                             '-Wstrict-prototypes '
                             '-Werror=implicit-function-declaration '
                             '-fvisibility=hidden  '
                             '-I/home/builder/.termux-build/python/src/Include/internal '
                             '-IObjects -IInclude -IPython -I. '
                             '-I/home/builder/.termux-build/python/src/Include '
                             '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                             '-isystem/data/data/com.termux/files/usr/include '
                             '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                             '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                             '-isystem/data/data/com.termux/files/usr/include '
                             '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                             '-fPIC -DPy_BUILD_CORE_BUILTIN',
 'PY_CFLAGS': '-fno-strict-overflow -Wsign-compare -Wunreachable-code -DNDEBUG '
              '-g -O3 -Wall -fstack-protector-strong -O3  '
              '-fstack-protector-strong -O3',
 'PY_CFLAGS_NODIST': '-std=c11 -Wextra -Wno-unused-parameter '
                     '-Wno-missing-field-initializers -Wstrict-prototypes '
                     '-Werror=implicit-function-declaration '
                     '-fvisibility=hidden  '
                     '-I/home/builder/.termux-build/python/src/Include/internal',
 'PY_COERCE_C_LOCALE': 1,
 'PY_CORE_CFLAGS': '-fno-strict-overflow -Wsign-compare -Wunreachable-code '
                   '-DNDEBUG -g -O3 -Wall -fstack-protector-strong -O3  '
                   '-fstack-protector-strong -O3 -std=c11 -Wextra '
                   '-Wno-unused-parameter -Wno-missing-field-initializers '
                   '-Wstrict-prototypes -Werror=implicit-function-declaration '
                   '-fvisibility=hidden  '
                   '-I/home/builder/.termux-build/python/src/Include/internal '
                   '-IObjects -IInclude -IPython -I. '
                   '-I/home/builder/.termux-build/python/src/Include '
                   '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                   '-isystem/data/data/com.termux/files/usr/include '
                   '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                   '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                   '-isystem/data/data/com.termux/files/usr/include '
                   '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                   '-fPIC -DPy_BUILD_CORE',
 'PY_CORE_LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
                    '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                    '-Wl,--enable-new-dtags -Wl,--as-needed '
                    '-Wl,-z,relro,-z,now '
                    '-Wl,--no-as-needed,-landroid-support,--as-needed '
                    '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                    '-landroid-posix-semaphore '
                    '-L/data/data/com.termux/files/usr/lib '
                    '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                    '-Wl,--enable-new-dtags -Wl,--as-needed '
                    '-Wl,-z,relro,-z,now '
                    '-Wl,--no-as-needed,-landroid-support,--as-needed '
                    '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                    '-landroid-posix-semaphore',
 'PY_CPPFLAGS': '-IObjects -IInclude -IPython -I. '
                '-I/home/builder/.termux-build/python/src/Include '
                '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                '-isystem/data/data/com.termux/files/usr/include '
                '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                '-isystem/data/data/com.termux/files/usr/include '
                '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include',
 'PY_ENABLE_SHARED': 1,
 'PY_HAVE_PERF_TRAMPOLINE': 0,
 'PY_LDFLAGS': '-L/data/data/com.termux/files/usr/lib '
               '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
               '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
               '-Wl,--no-as-needed,-landroid-support,--as-needed '
               '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
               '-landroid-posix-semaphore '
               '-L/data/data/com.termux/files/usr/lib '
               '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
               '-Wl,--enable-new-dtags -Wl,--as-needed -Wl,-z,relro,-z,now '
               '-Wl,--no-as-needed,-landroid-support,--as-needed '
               '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
               '-landroid-posix-semaphore',
 'PY_LDFLAGS_NODIST': '',
 'PY_LDFLAGS_NOLTO': '-L/data/data/com.termux/files/usr/lib '
                     '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                     '-Wl,--enable-new-dtags -Wl,--as-needed '
                     '-Wl,-z,relro,-z,now '
                     '-Wl,--no-as-needed,-landroid-support,--as-needed '
                     '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                     '-landroid-posix-semaphore '
                     '-L/data/data/com.termux/files/usr/lib '
                     '-Wl,-rpath=/data/data/com.termux/files/usr/lib '
                     '-Wl,--enable-new-dtags -Wl,--as-needed '
                     '-Wl,-z,relro,-z,now '
                     '-Wl,--no-as-needed,-landroid-support,--as-needed '
                     '-L/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/lib '
                     '-landroid-posix-semaphore',
 'PY_SQLITE_ENABLE_LOAD_EXTENSION': 1,
 'PY_SQLITE_HAVE_SERIALIZE': 1,
 'PY_SSL_DEFAULT_CIPHERS': 1,
 'PY_SSL_DEFAULT_CIPHER_STRING': 0,
 'PY_STDMODULE_CFLAGS': '-fno-strict-overflow -Wsign-compare '
                        '-Wunreachable-code -DNDEBUG -g -O3 -Wall '
                        '-fstack-protector-strong -O3  '
                        '-fstack-protector-strong -O3 -std=c11 -Wextra '
                        '-Wno-unused-parameter -Wno-missing-field-initializers '
                        '-Wstrict-prototypes '
                        '-Werror=implicit-function-declaration '
                        '-fvisibility=hidden  '
                        '-I/home/builder/.termux-build/python/src/Include/internal '
                        '-IObjects -IInclude -IPython -I. '
                        '-I/home/builder/.termux-build/python/src/Include '
                        '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                        '-isystem/data/data/com.termux/files/usr/include '
                        '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include  '
                        '-isystem/data/data/com.termux/files/usr/include/c++/v1 '
                        '-isystem/data/data/com.termux/files/usr/include '
                        '-I/home/builder/.termux-build/_cache/android-r28c-api-24-v2/sysroot/usr/include '
                        '-fPIC',
 'PY_SUPPORT_TIER': 0,
 'Py_DEBUG': 0,
 'Py_ENABLE_SHARED': 1,
 'Py_HASH_ALGORITHM': 0,
 'Py_STATS': 0,
 'Py_SUNOS_VERSION': 0,
 'Py_TRACE_REFS': 0,
 'QUICKTESTOPTS': '-x test_subprocess test_io test_lib2to3 \\',
 'READELF': '@READELF@',
 'RESSRCDIR': 'Mac/Resources/framework',
 'RETSIGTYPE': 'void',
 'RUNSHARED': '',
 'SCRIPTDIR': '/data/data/com.termux/files/usr/lib',
 'SCRIPT_2TO3': 'build/scripts-3.12/2to3-3.12',
 'SCRIPT_IDLE': 'build/scripts-3.12/idle3.12',
 'SCRIPT_PYDOC': 'build/scripts-3.12/pydoc3.12',
 'SETPGRP_HAVE_ARG': 0,
 'SHAREDMODS': 'Modules/array.cpython-312.so Modules/_asyncio.cpython-312.so '
               'Modules/_bisect.cpython-312.so '
               'Modules/_contextvars.cpython-312.so '
               'Modules/_csv.cpython-312.so Modules/_heapq.cpython-312.so '
               'Modules/_json.cpython-312.so Modules/_lsprof.cpython-312.so '
               'Modules/_opcode.cpython-312.so Modules/_pickle.cpython-312.so '
               'Modules/_queue.cpython-312.so Modules/_random.cpython-312.so '
               'Modules/_struct.cpython-312.so '
               'Modules/_xxsubinterpreters.cpython-312.so '
               'Modules/_xxinterpchannels.cpython-312.so '
               'Modules/_zoneinfo.cpython-312.so '
               'Modules/audioop.cpython-312.so Modules/math.cpython-312.so '
               'Modules/cmath.cpython-312.so '
               'Modules/_statistics.cpython-312.so '
               'Modules/_datetime.cpython-312.so '
               'Modules/_decimal.cpython-312.so '
               'Modules/binascii.cpython-312.so Modules/_bz2.cpython-312.so '
               'Modules/_lzma.cpython-312.so Modules/zlib.cpython-312.so '
               'Modules/_dbm.cpython-312.so Modules/_gdbm.cpython-312.so '
               'Modules/readline.cpython-312.so Modules/_md5.cpython-312.so '
               'Modules/_sha1.cpython-312.so Modules/_sha2.cpython-312.so '
               'Modules/_sha3.cpython-312.so Modules/_blake2.cpython-312.so '
               'Modules/pyexpat.cpython-312.so '
               'Modules/_elementtree.cpython-312.so '
               'Modules/_codecs_cn.cpython-312.so '
               'Modules/_codecs_hk.cpython-312.so '
               'Modules/_codecs_iso2022.cpython-312.so '
               'Modules/_codecs_jp.cpython-312.so '
               'Modules/_codecs_kr.cpython-312.so '
               'Modules/_codecs_tw.cpython-312.so '
               'Modules/_multibytecodec.cpython-312.so '
               'Modules/unicodedata.cpython-312.so '
               'Modules/_crypt.cpython-312.so Modules/fcntl.cpython-312.so '
               'Modules/grp.cpython-312.so Modules/mmap.cpython-312.so '
               'Modules/ossaudiodev.cpython-312.so '
               'Modules/_posixsubprocess.cpython-312.so '
               'Modules/resource.cpython-312.so Modules/select.cpython-312.so '
               'Modules/_socket.cpython-312.so Modules/syslog.cpython-312.so '
               'Modules/termios.cpython-312.so '
               'Modules/_posixshmem.cpython-312.so '
               'Modules/_multiprocessing.cpython-312.so '
               'Modules/_ctypes.cpython-312.so Modules/_curses.cpython-312.so '
               'Modules/_curses_panel.cpython-312.so '
               'Modules/_sqlite3.cpython-312.so Modules/_ssl.cpython-312.so '
               'Modules/_hashlib.cpython-312.so '
               'Modules/_tkinter.cpython-312.so '
               'Modules/xxsubtype.cpython-312.so '
               'Modules/_xxtestfuzz.cpython-312.so '
               'Modules/_testbuffer.cpython-312.so '
               'Modules/_testinternalcapi.cpython-312.so '
               'Modules/_testcapi.cpython-312.so '
               'Modules/_testclinic.cpython-312.so '
               'Modules/_testimportmultiple.cpython-312.so '
               'Modules/_testmultiphase.cpython-312.so '
               'Modules/_testsinglephase.cpython-312.so '
               'Modules/_ctypes_test.cpython-312.so '
               'Modules/xxlimited.cpython-312.so '
               'Modules/xxlimited_35.cpython-312.so',
 'SHELL': '/bin/sh -e',
 'SHLIBS': '-ldl  -lpthread',
 'SHLIB_SUFFIX': '.so',
 'SIGNED_RIGHT_SHIFT_ZERO_FILLS': 0,
 'SITEPATH': '',
 'SIZEOF_DOUBLE': 8,
 'SIZEOF_FLOAT': 4,
 'SIZEOF_FPOS_T': 8,
 'SIZEOF_INT': 4,
 'SIZEOF_LONG': 8,
 'SIZEOF_LONG_DOUBLE': 16,
 'SIZEOF_LONG_LONG': 8,
 'SIZEOF_OFF_T': 8,
 'SIZEOF_PID_T': 4,
 'SIZEOF_PTHREAD_KEY_T': 4,
 'SIZEOF_PTHREAD_T': 8,
 'SIZEOF_SHORT': 2,
 'SIZEOF_SIZE_T': 8,
 'SIZEOF_TIME_T': 8,
 'SIZEOF_UINTPTR_T': 8,
 'SIZEOF_VOID_P': 8,
 'SIZEOF_WCHAR_T': 4,
 'SIZEOF__BOOL': 1,
 'SOABI': 'cpython-312',
 'SRCDIRS': 'Modules   Modules/_blake2   Modules/_ctypes   Modules/_decimal   '
            'Modules/_decimal/libmpdec   Modules/_hacl   Modules/_io   '
            'Modules/_multiprocessing   Modules/_sqlite   Modules/_sre   '
            'Modules/_testcapi   Modules/_xxtestfuzz   Modules/cjkcodecs   '
            'Modules/expat   Objects   Parser   Programs   Python   '
            'Python/frozen_modules   Python/deepfreeze',
 'SRC_GDB_HOOKS': '/home/builder/.termux-build/python/src/Tools/gdb/libpython.py',
 'STATIC_LIBPYTHON': 1,
 'STDC_HEADERS': 1,
 'STRICT_SYSV_CURSES': "/* Don't use ncurses extensions */",
 'STRIPFLAG': '-s',
 'SUBDIRS': '',
 'SUBDIRSTOO': 'Include Lib Misc',
 'SYSLIBS': '-lm',
 'SYS_SELECT_WITH_SYS_TIME': 1,
 'TESTOPTS': '',
 'TESTPATH': '',
 'TESTPYTHON': '_PYTHON_PROJECT_BASE=/home/builder/.termux-build/python/build '
               '_PYTHON_HOST_PLATFORM=$(_PYTHON_HOST_PLATFORM) '
               'PYTHONPATH=$(shell test -f pybuilddir.txt && echo '
               '/home/builder/.termux-build/python/build/`cat '
               'pybuilddir.txt`:)/home/builder/.termux-build/python/src/Lib '
               '_PYTHON_SYSCONFIGDATA_NAME=_sysconfigdata__linux_ python3.12',
 'TESTPYTHONOPTS': '',
 'TESTRUNNER': '_PYTHON_PROJECT_BASE=/home/builder/.termux-build/python/build '
               '_PYTHON_HOST_PLATFORM=$(_PYTHON_HOST_PLATFORM) '
               'PYTHONPATH=$(shell test -f pybuilddir.txt && echo '
               '/home/builder/.termux-build/python/build/`cat '
               'pybuilddir.txt`:)/home/builder/.termux-build/python/src/Lib '
               '_PYTHON_SYSCONFIGDATA_NAME=_sysconfigdata__linux_ python3.12 '
               '/home/builder/.termux-build/python/src/Tools/scripts/run_tests.py',
 'TESTSUBDIRS': 'idlelib/idle_test \\',
 'TESTTIMEOUT': 1200,
 'TEST_MODULES': 'yes',
 'THREAD_STACK_SIZE': 0,
 'TIMEMODULE_LIB': 0,
 'TM_IN_SYS_TIME': 0,
 'TZPATH': '/usr/share/zoneinfo:/usr/lib/zoneinfo:/usr/share/lib/zoneinfo:/etc/zoneinfo',
 'UNICODE_DEPS': '\\',
 'UNIVERSALSDK': '',
 'UPDATE_FILE': '/home/builder/.termux-build/python/src/Tools/build/update_file.py',
 'USE_COMPUTED_GOTOS': 0,
 'VERSION': '3.12',
 'VPATH': '/home/builder/.termux-build/python/src',
 'WASM_ASSETS_DIR': './data/data/com.termux/files/usr',
 'WASM_STDLIB': './data/data/com.termux/files/usr/lib/python3.12/os.py',
 'WHEEL_PKG_DIR': '',
 'WINDOW_HAS_FLAGS': 0,
 'WITH_DECIMAL_CONTEXTVAR': 1,
 'WITH_DOC_STRINGS': 1,
 'WITH_DTRACE': 0,
 'WITH_DYLD': 0,
 'WITH_EDITLINE': 0,
 'WITH_FREELISTS': 1,
 'WITH_LIBINTL': 0,
 'WITH_NEXT_FRAMEWORK': 0,
 'WITH_PYMALLOC': 1,
 'WITH_VALGRIND': 0,
 'X87_DOUBLE_ROUNDING': 0,
 'XMLLIBSUBDIRS': 'xml xml/dom xml/etree xml/parsers xml/sax',
 'abs_builddir': '/home/builder/.termux-build/python/build',
 'abs_srcdir': '/home/builder/.termux-build/python/src',
 'datarootdir': '/data/data/com.termux/files/usr/share',
 'exec_prefix': '/data/data/com.termux/files/usr',
 'prefix': '/data/data/com.termux/files/usr',
 'srcdir': '/home/builder/.termux-build/python/src'}
