#!/usr/bin/env bash

set -e

# Switch message language
if [ -z "${LANG##zh_*}" ]; then
  _HMCLDL_USE_CHINESE=true
else
  _HMCLDL_USE_CHINESE=false
fi

# _HMCLDL_OS
case "$OSTYPE" in
  linux*)
    _HMCLDL_OS="linux";;
  darwin*)
    _HMCLDL_OS="macos";;
  freebsd*)
    _HMCLDL_OS="freebsd";;
  msys*|cygwin*)
    _HMCLDL_OS="windows";;
  *)
    _HMCLDL_OS="unknown";;
esac

# Normalize _HMCLDL_ARCH
case "$(uname -m)" in
  x86_64|x86-64|amd64|em64t|x64)
    _HMCLDL_ARCH="x86_64";;
  x86_32|x86-32|x86|ia32|i386|i486|i586|i686|i86pc|x32)
    _HMCLDL_ARCH="x86";;
  arm64|aarch64|armv8*|armv9*)
    _HMCLDL_ARCH="arm64";;
  arm|arm32|aarch32|armv7*)
    _HMCLDL_ARCH="arm32";;
  riscv64)
    _HMCLDL_ARCH="riscv64";;
  loongarch64)
    _HMCLDL_ARCH="loongarch64";;
  mips64)
    _HMCLDL_ARCH="mips64el";;
  *)
    _HMCLDL_ARCH="unknown";;
esac

# Self path
_HMCLDL_PATH="${BASH_SOURCE[0]}"
_HMCLDL_DIR=$(dirname "$_HMCLDL_PATH")

if [ "$_HMCLDL_OS" == "windows" ]; then
  _HMCLDL_JAVA_EXE_NAME="java.exe"
else
  _HMCLDL_JAVA_EXE_NAME="java"
fi

# _HMCLDL_VM_OPTIONS
if [ -n "${HMCLDL_JAVA_OPTS+x}" ]; then
  _HMCLDL_VM_OPTIONS=${HMCLDL_JAVA_OPTS}
else
  _HMCLDL_VM_OPTIONS="-XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15 -XX:G1PeriodicGCInterval=900000 -XX:G1PeriodicGCSystemLoadThreshold=0.6 -XX:+UseStringDeduplication"
fi

function show_warning_console() {
    echo -e "\033[1;31m$1\033[0m" >&2
}

function show_warning_dialog() {
  if [ -n "$3" ]; then
    if [ -n "$(command -v xdg-open)" ]; then
      if [ -n "$(command -v zenity)" ]; then
        zenity --question --title="$1" --text="$2" && xdg-open "$3"
      elif [ -n "$(command -v kdialog)" ]; then
        kdialog --title "$1" --warningyesno "$2" && xdg-open "$3"
      fi
    fi
  else
    if [ -n "$(command -v zenity)" ]; then
      zenity --info --title="$1" --text="$2"
    elif [ -n "$(command -v kdialog)" ]; then
      kdialog --title "$1" --msgbox "$2"
    fi
  fi
}

function show_warning() {
    show_warning_console "$1: $2"
    show_warning_dialog "$1" "$2"
}

# First, find Java in HMCLDL_JAVA_HOME
if [ -n "${HMCLDL_JAVA_HOME+x}" ]; then
  if [ -x "$HMCLDL_JAVA_HOME/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
    exec "$HMCLDL_JAVA_HOME/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
  else
    if [ "$_HMCLDL_USE_CHINESE" == true ]; then
      show_warning "错误" "环境变量 HMCLDL_JAVA_HOME 的值无效，请设置为合法的 Java 路径。\n你可以访问 https://github.com/cubestudio-dev/HMCLDL 页面寻求帮助。"
    else
      show_warning "Error" "The value of the environment variable HMCLDL_JAVA_HOME is invalid, please set it to a valid Java path.\nYou can visit the https://github.com/cubestudio-dev/HMCLDL page for help."
    fi
    exit 1
  fi
fi

# Find Java in the script directory
case "$_HMCLDL_ARCH" in
  x86_64)
    if [ -x "$_HMCLDL_DIR/jre-x64/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-x64/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    if [ -x "$_HMCLDL_DIR/jre-x86/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-x86/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
  x86)
    if [ -x "$_HMCLDL_DIR/jre-x86/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-x86/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
  arm64)
    if [ -x "$_HMCLDL_DIR/jre-arm64/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-arm64/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
  arm32)
    if [ -x "$_HMCLDL_DIR/jre-arm32/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-arm32/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
  riscv64)
    if [ -x "$_HMCLDL_DIR/jre-riscv64/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-riscv64/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
  loongarch64)
    if [ -x "$_HMCLDL_DIR/jre-loongarch64/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
      exec "$_HMCLDL_DIR/jre-loongarch64/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
    fi
    ;;
esac

# Find Java in JAVA_HOME
if [ -f "$JAVA_HOME/bin/$_HMCLDL_JAVA_EXE_NAME" ]; then
  exec "$JAVA_HOME/bin/$_HMCLDL_JAVA_EXE_NAME" $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
fi

# Find Java in PATH
if [ -x "$(command -v $_HMCLDL_JAVA_EXE_NAME)" ]; then
  exec $_HMCLDL_JAVA_EXE_NAME $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
fi

if [ "$_HMCLDL_OS" == "freebsd" ] && [ -x "$(command -v javavm)" ]; then
  exec javavm $_HMCLDL_VM_OPTIONS -jar "$_HMCLDL_PATH"
fi

# Java not found

case "$_HMCLDL_OS-$_HMCLDL_ARCH" in
  windows-x86|windows-x86_64|windows-arm64)
    _HMCLDL_JAVA_DOWNLOAD_PAGE="https://github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1"
    ;;
  linux-x86|linux-x86_64|linux-arm64|linux-arm32|linux-riscv64|linux-loongarch64)
    _HMCLDL_JAVA_DOWNLOAD_PAGE="https://github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1"
    ;;
  macos-x86_64|macos-arm64)
    _HMCLDL_JAVA_DOWNLOAD_PAGE="https://github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1"
    ;;
esac

if [ "$_HMCLDL_USE_CHINESE" == true ]; then
  _HMCLDL_WARNING_MESSAGE="运行 HMCLDL 需要 Java 运行时环境，请安装 Java 并设置环境变量后重试。"

  if [ -n "$_HMCLDL_JAVA_DOWNLOAD_PAGE" ]; then
    show_warning_console "错误: $_HMCLDL_WARNING_MESSAGE\n你可以前往此处下载:\n$_HMCLDL_JAVA_DOWNLOAD_PAGE"
    show_warning_dialog  "错误" "$_HMCLDL_WARNING_MESSAGE\n\n是否要前往 Java 下载页面？" "$_HMCLDL_JAVA_DOWNLOAD_PAGE"
  else
    show_warning "错误" "$_HMCLDL_WARNING_MESSAGE\n你可以访问 https://github.com/cubestudio-dev/HMCLDL 页面寻求帮助。"
  fi
else
  _HMCLDL_WARNING_MESSAGE="The Java runtime environment is required to run HMCLDL.\nPlease install Java and set the environment variables and try again."
  if [ -n "$_HMCLDL_JAVA_DOWNLOAD_PAGE" ]; then
    show_warning_console "Error: $_HMCLDL_WARNING_MESSAGE\nYou can download it from here:\n$_HMCLDL_JAVA_DOWNLOAD_PAGE"
    show_warning_dialog  "Error" "$_HMCLDL_WARNING_MESSAGE\n\nDo you want to go to the Java download page?" "$_HMCLDL_JAVA_DOWNLOAD_PAGE"
  else
    show_warning "Error" "$_HMCLDL_WARNING_MESSAGE\nYou can visit the https://github.com/cubestudio-dev/HMCLDL page for help."
  fi
fi

exit 1
