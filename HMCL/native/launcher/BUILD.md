# HMCLDL Windows Launcher Stub

Source of the Windows `HMCLDL.exe` launcher stub, kept in-tree under
`HMCL/native/launcher`. It is derived from
[HMCLauncher](https://github.com/Glavo/HMCLauncher) by huangyuhui
(GPLv3 with additional permissions — see `LICENSE`).

`HMCLDL.exe` in this directory is the prebuilt stub (pure launcher, jar not
yet appended); the Gradle packaging task appends the HMCLDL fat jar to it
when assembling the release artifact. It can be regenerated with the MinGW
build described below.

## What it does

A tiny C++ GUI program that:

1. finds a suitable `javaw.exe` — bundled `jre-arm64` / `jre-x64` / `jre-x86`
   dir next to the exe, then `HMCLDL_JAVA_HOME` / `JAVA_HOME`, the
   `SOFTWARE\JavaSoft` registry keys, `C:\Program Files[\ (x86)]\<vendor>`
   dirs, `PATH`, and finally `%USERPROFILE%\.hmcldl\java\windows-*`;
2. checks the candidate is **Java 17 or later** through its version resource
   (the HMCLDL jar is built for Java 17, so older runtimes are rejected and
   the download prompt is shown instead);
3. launches `javaw <HMCLDL_JAVA_OPTS> -jar <itself>` — the HMCLDL fat jar is
   appended after the stub's own PE data when the release artifact is
   assembled, and the JVM's zip reader finds it through the
   End-of-Central-Directory record at the end of the file.

If no Java is found it shows a bilingual prompt and can open the Java download
guidance page: <https://github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1>.

The exe intentionally stays **32-bit (PE32 i386)** like upstream so that one
artifact runs on both 32-bit and 64-bit Windows.

## Rebranding changes vs upstream HMCLauncher

| Area | Upstream | HMCLDL |
| --- | --- | --- |
| Icon / version info (`HMCLDL.rc`, `resource.h`) | `HMCL.ico`, 3.6.0.1, "Hello Minecraft! Launcher", `HMCL.exe`, huanghongxun | `HMCLDL.ico`, 1.0.0.0, "HMCLDL", `HMCLDL.exe`, cubestudio-dev (copyright keeps the huangyuhui attribution) |
| String table | `HMCL` | `HMCLDL` |
| Error prompts (`lang.h`) | "…to run HMCL…" / "运行 HMCL…" (Java 8-era wording) | "HMCLDL requires Java 17 or later…" / "HMCLDL 需要 Java 17 或更高版本…" |
| Minimum Java version (`main.cpp`, `java.cpp`) | Java 8 (`J8`, `JAVA_8`) | Java 17 (`J17`, `JAVA_17`) — matches the HMCL jar's bytecode target and the upstream 3.7.x stub |
| JVM options env var (`main.cpp`) | `HMCL_JAVA_OPTS` | `HMCLDL_JAVA_OPTS` |
| Java home env var (`java.cpp`) | `HMCL_JAVA_HOME` | `HMCLDL_JAVA_HOME` |
| Bundled-Java dir (`main.cpp`) | `.hmcl\java` | `.hmcldl\java` |
| Java download links | `docs.hmcl.net/downloads/windows/*.html` | `github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1` |
| `OpenHelpPage()` (`main.cpp`) | unused function pointing to `docs.hmcl.net/help.html` | removed |

Portability fixes for building with MinGW on case-sensitive hosts:
`<SDKDDKVer.h>` → `<sdkddkver.h>` (`targetver.h`), `<Objbase.h>` → `<objbase.h>`
(`os.h`), `"version.h"` → `"Version.h"` (`Version.cpp`, `java.cpp`).

## Building with MinGW-w64 (i686)

Requirements (Debian/Ubuntu):
`apt install g++-mingw-w64-i686-posix binutils-mingw-w64-i686`
plus `HMCLDL.ico` (24/32/48/256 px) next to `HMCLDL.rc`.

```sh
i686-w64-mingw32-windres HMCLDL.rc -O coff -o HMCLDL.res.o --codepage=65001

i686-w64-mingw32-g++-posix -municode -mwindows -static -O2 -Wall -s \
    -Wl,--major-os-version=6,--minor-os-version=0,--major-subsystem-version=6,--minor-subsystem-version=0 \
    -o HMCLDL.exe \
    main.cpp java.cpp os.cpp stdafx.cpp Version.cpp HMCLDL.res.o \
    -lversion -lole32 -luuid
```

Notes:

- `-municode` selects the `wWinMain` entry point, `-mwindows` the GUI subsystem,
  `-static` links libstdc++/libgcc statically (no extra DLL dependencies).
- The `-Wl,--major-*-version` flags reproduce the OS/subsystem version 6.0 of
  the upstream artifact.
- On a case-sensitive host without the `i686-w64-mingw32-gcc` alias, windres
  needs an explicit preprocessor, e.g.
  `--preprocessor=i686-w64-mingw32-gcc-posix --preprocessor-arg=-E --preprocessor-arg=-x --preprocessor-arg=c`.

Or with CMake:

```sh
cmake -B build \
      -DCMAKE_SYSTEM_NAME=Windows -DCMAKE_SYSTEM_PROCESSOR=x86 \
      -DCMAKE_CXX_COMPILER=i686-w64-mingw32-g++-posix \
      -DCMAKE_RC_COMPILER=i686-w64-mingw32-windres
cmake --build build
```

## Appending the jar

The shipped exe = stub + fat jar:

```sh
cat HMCLDL-1.0.jar >> HMCLDL.exe
```

Smoke test on Linux (proves the JVM zip reader scans past the PE data — the
same mechanism the stub relies on when it runs `javaw -jar <itself>`):

```sh
jdk/bin/jar tf HMCLDL.exe | head    # lists jar entries
unzip -l HMCLDL.exe | head          # same via Info-ZIP
```
