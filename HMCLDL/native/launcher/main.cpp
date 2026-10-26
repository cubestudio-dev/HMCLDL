#include "stdafx.h"
#include "main.h"
#include "os.h"
#include "java.h"
#include "lang.h"
#include <windows.h>
#include <wctype.h>

// HMCLDL (JavaFX build) requires Java 17 or later to run.
Version J17(TEXT("17"));

extern "C" {
__declspec(dllexport) DWORD NvOptimusEnablement = 0x00000001;
__declspec(dllexport) DWORD AmdPowerXpressRequestHighPerformance = 0x00000001;
}

LPCWSTR VENDOR_DIRS[] = {
  L"Java", L"Microsoft", L"BellSoft", L"Zulu", L"Eclipse Foundation", L"AdoptOpenJDK", L"Semeru"
};

void RawLaunchJVM(const std::wstring &javaPath, const std::wstring &workdir,
                  const std::wstring &jarPath, const std::wstring &jvmOptions) {
  if (MyCreateProcess(L"\"" + javaPath + L"\" " + jvmOptions + L" -jar \"" + jarPath + L"\"", workdir))
    exit(EXIT_SUCCESS);
}

void LaunchJVM(const std::wstring &javaPath, const std::wstring &workdir,
               const std::wstring &jarPath, const std::wstring &jvmOptions) {
  Version javaVersion(L"");
  if (!MyGetFileVersionInfo(javaPath, javaVersion)) return;

  if (J17 <= javaVersion) {
    RawLaunchJVM(javaPath, workdir, jarPath, jvmOptions);
  }
}

void FindJavaInDirAndLaunchJVM(const std::wstring &baseDir, const std::wstring &workdir,
                               const std::wstring &jarPath, const std::wstring &jvmOptions) {
  std::wstring pattern = baseDir + L"*";

  WIN32_FIND_DATA data;
  HANDLE hFind = FindFirstFile(pattern.c_str(), &data);  // Search all subdirectory

  if (hFind != INVALID_HANDLE_VALUE) {
    do {
      std::wstring javaw = baseDir + data.cFileName + std::wstring(L"\\bin\\javaw.exe");
      if (FindFirstFileExists(javaw.c_str(), 0)) {
        LaunchJVM(javaw, workdir, jarPath, jvmOptions);
      }
    } while (FindNextFile(hFind, &data));
    FindClose(hFind);
  }
}

// Scan every directory listed in PATH for java.exe / javaw.exe.
void FindJavaInPathAndLaunchJVM(const std::wstring &workdir,
                                const std::wstring &jarPath, const std::wstring &jvmOptions) {
  std::wstring pathEnv;
  if (ERROR_SUCCESS != MyGetEnvironmentVariable(L"PATH", pathEnv)) return;

  size_t start = 0;
  while (start <= pathEnv.size()) {
    size_t end = pathEnv.find(L';', start);
    if (end == std::wstring::npos) end = pathEnv.size();
    std::wstring dir = pathEnv.substr(start, end - start);
    start = end + 1;

    // Trim quotes and surrounding whitespace.
    if (!dir.empty() && dir.front() == L'"' && dir.back() == L'"')
      dir = dir.substr(1, dir.size() - 2);
    while (!dir.empty() && iswspace(dir.front())) dir.erase(dir.begin());
    while (!dir.empty() && iswspace(dir.back())) dir.pop_back();
    if (dir.empty()) continue;

    if (dir.back() != L'\\' && dir.back() != L'/') dir += L'\\';

    static const LPCWSTR exeNames[] = {L"javaw.exe", L"java.exe"};
    for (LPCWSTR exeName : exeNames) {
      std::wstring javaPath = dir + exeName;
      if (FindFirstFileExists(javaPath.c_str(), 0)) {
        LaunchJVM(javaPath, workdir, jarPath, jvmOptions);
      }
    }
  }
}

int APIENTRY wWinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance,
                      LPWSTR lpCmdLine, int nCmdShow) {
  std::wstring path, exeName, jvmOptions;

  // Since Jar file is appended to this executable, we should first get the
  // location of JAR file.
  if (ERROR_SUCCESS != MyGetModuleFileName(NULL, exeName)) return 1;

  std::wstring workdir;
  size_t last_slash = exeName.find_last_of(L"/\\");
  if (last_slash != std::wstring::npos && last_slash + 1 < exeName.size()) {
    workdir = exeName.substr(0, last_slash);
    exeName = exeName.substr(last_slash + 1);
  }

  if (ERROR_SUCCESS != MyGetEnvironmentVariable(L"HMCLDL_JAVA_OPTS", jvmOptions)) {
    jvmOptions = L"-XX:MinHeapFreeRatio=5 -XX:MaxHeapFreeRatio=15"; // Default Options
  }

  bool useChinese = GetUserDefaultUILanguage() == 2052; // zh-CN

  MyArchitecture architecture = MyGetArchitecture();

  // First try the Java packaged together.
  bool isX64   = architecture == MyArchitecture::X86_64;
  bool isARM64 = architecture == MyArchitecture::ARM64;

  if (isARM64) {
    RawLaunchJVM(L"jre-arm64\\bin\\javaw.exe", workdir, exeName, jvmOptions);
  }
  if (isX64) {
    RawLaunchJVM(L"jre-x64\\bin\\javaw.exe", workdir, exeName, jvmOptions);
  }
  RawLaunchJVM(L"jre-x86\\bin\\javaw.exe", workdir, exeName, jvmOptions);

  if (FindJava(path)) LaunchJVM(path + L"\\bin\\javaw.exe", workdir, exeName, jvmOptions);

  // Or we try to search Java in PATH (each entry, java.exe and javaw.exe)
  FindJavaInPathAndLaunchJVM(workdir, exeName, jvmOptions);

  std::wstring programFiles;

  // Or we try to search Java in C:\Program Files
  if (!SUCCEEDED(MySHGetFolderPath(CSIDL_PROGRAM_FILES, programFiles))) programFiles = L"C:\\Program Files\\";
  for (LPCWSTR vendorDir : VENDOR_DIRS) {
    std::wstring dir = programFiles;
    MyPathAppend(dir, vendorDir);
    MyPathAddBackslash(dir);

    FindJavaInDirAndLaunchJVM(dir, workdir, exeName, jvmOptions);
  }

  // Consider C:\Program Files (x86)
  if (!SUCCEEDED(MySHGetFolderPath(CSIDL_PROGRAM_FILESX86, programFiles))) programFiles = L"C:\\Program Files (x86)\\";
  for (LPCWSTR vendorDir : VENDOR_DIRS) {
    std::wstring dir = programFiles;
    MyPathAppend(dir, vendorDir);
    MyPathAddBackslash(dir);

    FindJavaInDirAndLaunchJVM(dir, workdir, exeName, jvmOptions);
  }

  // Consider ProgramW6432 (Program Files seen from a 32-bit process)
  {
    std::wstring programW6432;
    if (ERROR_SUCCESS == MyGetEnvironmentVariable(L"ProgramW6432", programW6432) && !programW6432.empty()) {
      if (programW6432.back() != L'\\') programW6432 += L'\\';
      if (_wcsicmp(programW6432.c_str(), programFiles.c_str()) != 0) {
        for (LPCWSTR vendorDir : VENDOR_DIRS) {
          std::wstring dir = programW6432;
          MyPathAppend(dir, vendorDir);
          MyPathAddBackslash(dir);

          FindJavaInDirAndLaunchJVM(dir, workdir, exeName, jvmOptions);
        }
      }
    }
  }

  // Try java in PATH directly (rely on CreateProcess PATH lookup)
  RawLaunchJVM(L"javaw", workdir, exeName, jvmOptions);
  RawLaunchJVM(L"java", workdir, exeName, jvmOptions);

  // HMCLDL-managed Java runtimes (and the legacy HMCLDL 1.0 location).
  std::wstring javaRoot;
  {
    std::wstring buffer;
    if (SUCCEEDED(MySHGetFolderPath(CSIDL_APPDATA, buffer)) || SUCCEEDED(MySHGetFolderPath(CSIDL_PROFILE, buffer))) {
      javaRoot = buffer;
    }
  }

  LPCWSTR javaRootNames[] = {L".hmcldl", L".hmcl"};
  for (LPCWSTR javaRootName : javaRootNames) {
    std::wstring buffer = javaRoot;
    if (buffer.empty()) continue;
    MyPathAppend(buffer, javaRootName);
    MyPathAppend(buffer, L"java");
    if (isARM64) {
      MyPathAppend(buffer, L"windows-arm64");
    } else if (isX64) {
      MyPathAppend(buffer, L"windows-x86_64");
    } else {
      MyPathAppend(buffer, L"windows-x86");
    }
    MyPathAddBackslash(buffer);

    FindJavaInDirAndLaunchJVM(buffer, workdir, exeName, jvmOptions);
  }

  // Java download guidance page: point users to the HMCLDL release notes.
  LPCWSTR downloadLink = L"https://github.com/cubestudio-dev/HMCLDL/releases/tag/v1.1";

  if (IDOK == MessageBox(NULL, useChinese ? ERROR_PROMPT_ZH : ERROR_PROMPT, useChinese ? ERROR_TITLE_ZH : ERROR_TITLE, MB_ICONWARNING | MB_OKCANCEL)) {
    ShellExecute(0, 0, downloadLink, 0, 0, SW_SHOW);
  }
  return 1;
}
