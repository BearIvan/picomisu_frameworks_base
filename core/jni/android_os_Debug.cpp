/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "android.os.Debug"

#include <assert.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <inttypes.h>
#include <malloc.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

#include <atomic>
#include <iomanip>
#include <memory>
#include <string>
#include <vector>

#include <android-base/logging.h>
#include <bionic_malloc.h>
#include <debuggerd/client.h>
#include <log/log.h>
#include <utils/misc.h>
#include <utils/String8.h>
#include <utils/SystemClock.h>

#include <nativehelper/JNIHelp.h>
#include <nativehelper/ScopedUtfChars.h>
#include "jni.h"
#include <dmabufinfo/dmabufinfo.h>
#include <meminfo/procmeminfo.h>
#include <meminfo/sysmeminfo.h>
#include <meminfo_utils/sysmeminfo_utils.h>
#include <memtrack/memtrack.h>
#include <memunreachable/memunreachable.h>
#include <android-base/strings.h>
#include "android_os_Debug.h"

namespace android
{

enum {
    HEAP_UNKNOWN,
    HEAP_DALVIK,
    HEAP_NATIVE,

    HEAP_DALVIK_OTHER,
    HEAP_STACK,
    HEAP_CURSOR,
    HEAP_ASHMEM,
    HEAP_GL_DEV,
    HEAP_UNKNOWN_DEV,
    HEAP_SO,
    HEAP_JAR,
    HEAP_APK,
    HEAP_TTF,
    HEAP_DEX,
    HEAP_OAT,
    HEAP_ART,
    HEAP_UNKNOWN_MAP,
    HEAP_GRAPHICS,
    HEAP_GL,
    HEAP_OTHER_MEMTRACK,

    // Dalvik extra sections (heap).
    HEAP_DALVIK_NORMAL,
    HEAP_DALVIK_LARGE,
    HEAP_DALVIK_ZYGOTE,
    HEAP_DALVIK_NON_MOVING,

    // Dalvik other extra sections.
    HEAP_DALVIK_OTHER_LINEARALLOC,
    HEAP_DALVIK_OTHER_ACCOUNTING,
    HEAP_DALVIK_OTHER_CODE_CACHE,
    HEAP_DALVIK_OTHER_COMPILER_METADATA,
    HEAP_DALVIK_OTHER_INDIRECT_REFERENCE_TABLE,

    // Boot vdex / app dex / app vdex
    HEAP_DEX_BOOT_VDEX,
    HEAP_DEX_APP_DEX,
    HEAP_DEX_APP_VDEX,

    // App art, boot art.
    HEAP_ART_APP,
    HEAP_ART_BOOT,

    _NUM_HEAP,
    _NUM_EXCLUSIVE_HEAP = HEAP_OTHER_MEMTRACK+1,
    _NUM_CORE_HEAP = HEAP_NATIVE+1
};

struct stat_fields {
    jfieldID pss_field;
    jfieldID pssSwappable_field;
    jfieldID rss_field;
    jfieldID privateDirty_field;
    jfieldID sharedDirty_field;
    jfieldID privateClean_field;
    jfieldID sharedClean_field;
    jfieldID swappedOut_field;
    jfieldID swappedOutPss_field;
};

struct stat_field_names {
    const char* pss_name;
    const char* pssSwappable_name;
    const char* rss_name;
    const char* privateDirty_name;
    const char* sharedDirty_name;
    const char* privateClean_name;
    const char* sharedClean_name;
    const char* swappedOut_name;
    const char* swappedOutPss_name;
};

static stat_fields stat_fields[_NUM_CORE_HEAP];

static stat_field_names stat_field_names[_NUM_CORE_HEAP] = {
    { "otherPss", "otherSwappablePss", "otherRss", "otherPrivateDirty", "otherSharedDirty",
        "otherPrivateClean", "otherSharedClean", "otherSwappedOut", "otherSwappedOutPss" },
    { "dalvikPss", "dalvikSwappablePss", "dalvikRss", "dalvikPrivateDirty", "dalvikSharedDirty",
        "dalvikPrivateClean", "dalvikSharedClean", "dalvikSwappedOut", "dalvikSwappedOutPss" },
    { "nativePss", "nativeSwappablePss", "nativeRss", "nativePrivateDirty", "nativeSharedDirty",
        "nativePrivateClean", "nativeSharedClean", "nativeSwappedOut", "nativeSwappedOutPss" }
};

jfieldID otherStats_field;
jfieldID hasSwappedOutPss_field;

struct stats_t {
    int pss;
    int swappablePss;
    int rss;
    int privateDirty;
    int sharedDirty;
    int privateClean;
    int sharedClean;
    int swappedOut;
    int swappedOutPss;
};

#define BINDER_STATS "/proc/binder/stats"

static jlong android_os_Debug_getNativeHeapSize(JNIEnv *env, jobject clazz)
{
    struct mallinfo info = mallinfo();
    return (jlong) info.usmblks;
}

static jlong android_os_Debug_getNativeHeapAllocatedSize(JNIEnv *env, jobject clazz)
{
    struct mallinfo info = mallinfo();
    return (jlong) info.uordblks;
}

static jlong android_os_Debug_getNativeHeapFreeSize(JNIEnv *env, jobject clazz)
{
    struct mallinfo info = mallinfo();
    return (jlong) info.fordblks;
}

// Container used to retrieve graphics memory pss
struct graphics_memory_pss
{
    int graphics;
    int gl;
    int other;
};

/*
 * Uses libmemtrack to retrieve graphics memory that the process is using.
 * Any graphics memory reported in /proc/pid/smaps is not included here.
 */
static int read_memtrack_memory(struct memtrack_proc* p, int pid,
        struct graphics_memory_pss* graphics_mem)
{
    int err = memtrack_proc_get(p, pid);
    if (err != 0) {
        ALOGW("failed to get memory consumption info: %d", err);
        return err;
    }

    ssize_t pss = memtrack_proc_graphics_pss(p);
    if (pss < 0) {
        ALOGW("failed to get graphics pss: %zd", pss);
        return pss;
    }
    graphics_mem->graphics = pss / 1024;

    pss = memtrack_proc_gl_pss(p);
    if (pss < 0) {
        ALOGW("failed to get gl pss: %zd", pss);
        return pss;
    }
    graphics_mem->gl = pss / 1024;

    pss = memtrack_proc_other_pss(p);
    if (pss < 0) {
        ALOGW("failed to get other pss: %zd", pss);
        return pss;
    }
    graphics_mem->other = pss / 1024;

    return 0;
}

/*
 * Retrieves the graphics memory that is unaccounted for in /proc/pid/smaps.
 */
static int read_memtrack_memory(int pid, struct graphics_memory_pss* graphics_mem)
{
    struct memtrack_proc* p = memtrack_proc_new();
    if (p == NULL) {
        ALOGW("failed to create memtrack_proc");
        return -1;
    }

    int err = read_memtrack_memory(p, pid, graphics_mem);
    memtrack_proc_destroy(p);
    return err;
}

static void load_maps(int pid, stats_t* stats, bool* foundSwapPss)
{
    *foundSwapPss = false;
    uint64_t prev_end = 0;
    int prev_heap = HEAP_UNKNOWN;

    std::string smaps_path = base::StringPrintf("/proc/%d/smaps", pid);
    auto vma_scan = [&](const meminfo::Vma& vma) {
        int which_heap = HEAP_UNKNOWN;
        int sub_heap = HEAP_UNKNOWN;
        bool is_swappable = false;
        std::string name;
        if (base::EndsWith(vma.name, " (deleted)")) {
            name = vma.name.substr(0, vma.name.size() - strlen(" (deleted)"));
        } else {
            name = vma.name;
        }

        uint32_t namesz = name.size();
        if (base::StartsWith(name, "[heap]")) {
            which_heap = HEAP_NATIVE;
        } else if (base::StartsWith(name, "[anon:libc_malloc]")) {
            which_heap = HEAP_NATIVE;
        } else if (base::StartsWith(name, "[stack")) {
            which_heap = HEAP_STACK;
        } else if (base::EndsWith(name, ".so")) {
            which_heap = HEAP_SO;
            is_swappable = true;
        } else if (base::EndsWith(name, ".jar")) {
            which_heap = HEAP_JAR;
            is_swappable = true;
        } else if (base::EndsWith(name, ".apk")) {
            which_heap = HEAP_APK;
            is_swappable = true;
        } else if (base::EndsWith(name, ".ttf")) {
            which_heap = HEAP_TTF;
            is_swappable = true;
        } else if ((base::EndsWith(name, ".odex")) ||
                (namesz > 4 && strstr(name.c_str(), ".dex") != nullptr)) {
            which_heap = HEAP_DEX;
            sub_heap = HEAP_DEX_APP_DEX;
            is_swappable = true;
        } else if (base::EndsWith(name, ".vdex")) {
            which_heap = HEAP_DEX;
            // Handle system@framework@boot and system/framework/boot
            if ((strstr(name.c_str(), "@boot") != nullptr) ||
                    (strstr(name.c_str(), "/boot"))) {
                sub_heap = HEAP_DEX_BOOT_VDEX;
            } else {
                sub_heap = HEAP_DEX_APP_VDEX;
            }
            is_swappable = true;
        } else if (base::EndsWith(name, ".oat")) {
            which_heap = HEAP_OAT;
            is_swappable = true;
        } else if (base::EndsWith(name, ".art") || base::EndsWith(name, ".art]")) {
            which_heap = HEAP_ART;
            // Handle system@framework@boot* and system/framework/boot*
            if ((strstr(name.c_str(), "@boot") != nullptr) ||
                    (strstr(name.c_str(), "/boot"))) {
                sub_heap = HEAP_ART_BOOT;
            } else {
                sub_heap = HEAP_ART_APP;
            }
            is_swappable = true;
        } else if (base::StartsWith(name, "/dev/")) {
            which_heap = HEAP_UNKNOWN_DEV;
            if (base::StartsWith(name, "/dev/kgsl-3d0")) {
                which_heap = HEAP_GL_DEV;
            } else if (base::StartsWith(name, "/dev/ashmem/CursorWindow")) {
                which_heap = HEAP_CURSOR;
            } else if (base::StartsWith(name, "/dev/ashmem")) {
                which_heap = HEAP_ASHMEM;
            }
        } else if (base::StartsWith(name, "[anon:")) {
            which_heap = HEAP_UNKNOWN;
            if (base::StartsWith(name, "[anon:dalvik-")) {
                which_heap = HEAP_DALVIK_OTHER;
                if (base::StartsWith(name, "[anon:dalvik-LinearAlloc")) {
                    sub_heap = HEAP_DALVIK_OTHER_LINEARALLOC;
                } else if (base::StartsWith(name, "[anon:dalvik-alloc space") ||
                        base::StartsWith(name, "[anon:dalvik-main space")) {
                    // This is the regular Dalvik heap.
                    which_heap = HEAP_DALVIK;
                    sub_heap = HEAP_DALVIK_NORMAL;
                } else if (base::StartsWith(name,
                            "[anon:dalvik-large object space") ||
                        base::StartsWith(
                            name, "[anon:dalvik-free list large object space")) {
                    which_heap = HEAP_DALVIK;
                    sub_heap = HEAP_DALVIK_LARGE;
                } else if (base::StartsWith(name, "[anon:dalvik-non moving space")) {
                    which_heap = HEAP_DALVIK;
                    sub_heap = HEAP_DALVIK_NON_MOVING;
                } else if (base::StartsWith(name, "[anon:dalvik-zygote space")) {
                    which_heap = HEAP_DALVIK;
                    sub_heap = HEAP_DALVIK_ZYGOTE;
                } else if (base::StartsWith(name, "[anon:dalvik-indirect ref")) {
                    sub_heap = HEAP_DALVIK_OTHER_INDIRECT_REFERENCE_TABLE;
                } else if (base::StartsWith(name, "[anon:dalvik-jit-code-cache") ||
                        base::StartsWith(name, "[anon:dalvik-data-code-cache")) {
                    sub_heap = HEAP_DALVIK_OTHER_CODE_CACHE;
                } else if (base::StartsWith(name, "[anon:dalvik-CompilerMetadata")) {
                    sub_heap = HEAP_DALVIK_OTHER_COMPILER_METADATA;
                } else {
                    sub_heap = HEAP_DALVIK_OTHER_ACCOUNTING;  // Default to accounting.
                }
            }
        } else if (namesz > 0) {
            which_heap = HEAP_UNKNOWN_MAP;
        } else if (vma.start == prev_end && prev_heap == HEAP_SO) {
            // bss section of a shared library
            which_heap = HEAP_SO;
        }

        prev_end = vma.end;
        prev_heap = which_heap;

        const meminfo::MemUsage& usage = vma.usage;
        if (usage.swap_pss > 0 && *foundSwapPss != true) {
            *foundSwapPss = true;
        }

        uint64_t swapable_pss = 0;
        if (is_swappable && (usage.pss > 0)) {
            float sharing_proportion = 0.0;
            if ((usage.shared_clean > 0) || (usage.shared_dirty > 0)) {
                sharing_proportion = (usage.pss - usage.uss) / (usage.shared_clean + usage.shared_dirty);
            }
            swapable_pss = (sharing_proportion * usage.shared_clean) + usage.private_clean;
        }

        stats[which_heap].pss += usage.pss;
        stats[which_heap].swappablePss += swapable_pss;
        stats[which_heap].rss += usage.rss;
        stats[which_heap].privateDirty += usage.private_dirty;
        stats[which_heap].sharedDirty += usage.shared_dirty;
        stats[which_heap].privateClean += usage.private_clean;
        stats[which_heap].sharedClean += usage.shared_clean;
        stats[which_heap].swappedOut += usage.swap;
        stats[which_heap].swappedOutPss += usage.swap_pss;
        if (which_heap == HEAP_DALVIK || which_heap == HEAP_DALVIK_OTHER ||
                which_heap == HEAP_DEX || which_heap == HEAP_ART) {
            stats[sub_heap].pss += usage.pss;
            stats[sub_heap].swappablePss += swapable_pss;
            stats[sub_heap].rss += usage.rss;
            stats[sub_heap].privateDirty += usage.private_dirty;
            stats[sub_heap].sharedDirty += usage.shared_dirty;
            stats[sub_heap].privateClean += usage.private_clean;
            stats[sub_heap].sharedClean += usage.shared_clean;
            stats[sub_heap].swappedOut += usage.swap;
            stats[sub_heap].swappedOutPss += usage.swap_pss;
        }
    };

    meminfo::ForEachVmaFromFile(smaps_path, vma_scan);
}

static void android_os_Debug_getDirtyPagesPid(JNIEnv *env, jobject clazz,
        jint pid, jobject object)
{
    bool foundSwapPss;
    stats_t stats[_NUM_HEAP];
    memset(&stats, 0, sizeof(stats));

    load_maps(pid, stats, &foundSwapPss);

    struct graphics_memory_pss graphics_mem;
    if (read_memtrack_memory(pid, &graphics_mem) == 0) {
        stats[HEAP_GRAPHICS].pss = graphics_mem.graphics;
        stats[HEAP_GRAPHICS].privateDirty = graphics_mem.graphics;
        stats[HEAP_GRAPHICS].rss = graphics_mem.graphics;
        stats[HEAP_GL].pss = graphics_mem.gl;
        stats[HEAP_GL].privateDirty = graphics_mem.gl;
        stats[HEAP_GL].rss = graphics_mem.gl;
        stats[HEAP_OTHER_MEMTRACK].pss = graphics_mem.other;
        stats[HEAP_OTHER_MEMTRACK].privateDirty = graphics_mem.other;
        stats[HEAP_OTHER_MEMTRACK].rss = graphics_mem.other;
    }

    for (int i=_NUM_CORE_HEAP; i<_NUM_EXCLUSIVE_HEAP; i++) {
        stats[HEAP_UNKNOWN].pss += stats[i].pss;
        stats[HEAP_UNKNOWN].swappablePss += stats[i].swappablePss;
        stats[HEAP_UNKNOWN].rss += stats[i].rss;
        stats[HEAP_UNKNOWN].privateDirty += stats[i].privateDirty;
        stats[HEAP_UNKNOWN].sharedDirty += stats[i].sharedDirty;
        stats[HEAP_UNKNOWN].privateClean += stats[i].privateClean;
        stats[HEAP_UNKNOWN].sharedClean += stats[i].sharedClean;
        stats[HEAP_UNKNOWN].swappedOut += stats[i].swappedOut;
        stats[HEAP_UNKNOWN].swappedOutPss += stats[i].swappedOutPss;
    }

    for (int i=0; i<_NUM_CORE_HEAP; i++) {
        env->SetIntField(object, stat_fields[i].pss_field, stats[i].pss);
        env->SetIntField(object, stat_fields[i].pssSwappable_field, stats[i].swappablePss);
        env->SetIntField(object, stat_fields[i].rss_field, stats[i].rss);
        env->SetIntField(object, stat_fields[i].privateDirty_field, stats[i].privateDirty);
        env->SetIntField(object, stat_fields[i].sharedDirty_field, stats[i].sharedDirty);
        env->SetIntField(object, stat_fields[i].privateClean_field, stats[i].privateClean);
        env->SetIntField(object, stat_fields[i].sharedClean_field, stats[i].sharedClean);
        env->SetIntField(object, stat_fields[i].swappedOut_field, stats[i].swappedOut);
        env->SetIntField(object, stat_fields[i].swappedOutPss_field, stats[i].swappedOutPss);
    }


    env->SetBooleanField(object, hasSwappedOutPss_field, foundSwapPss);
    jintArray otherIntArray = (jintArray)env->GetObjectField(object, otherStats_field);

    jint* otherArray = (jint*)env->GetPrimitiveArrayCritical(otherIntArray, 0);
    if (otherArray == NULL) {
        return;
    }

    int j=0;
    for (int i=_NUM_CORE_HEAP; i<_NUM_HEAP; i++) {
        otherArray[j++] = stats[i].pss;
        otherArray[j++] = stats[i].swappablePss;
        otherArray[j++] = stats[i].rss;
        otherArray[j++] = stats[i].privateDirty;
        otherArray[j++] = stats[i].sharedDirty;
        otherArray[j++] = stats[i].privateClean;
        otherArray[j++] = stats[i].sharedClean;
        otherArray[j++] = stats[i].swappedOut;
        otherArray[j++] = stats[i].swappedOutPss;
    }

    env->ReleasePrimitiveArrayCritical(otherIntArray, otherArray, 0);
}

static void android_os_Debug_getDirtyPages(JNIEnv *env, jobject clazz, jobject object)
{
    android_os_Debug_getDirtyPagesPid(env, clazz, getpid(), object);
}

// Smartisan (factory PICO OS 5.13.7): a KGSL per-process memory node (bytes), 0 if it cannot be
// read.
static jlong read_kgsl_proc_mem(const char* path)
{
    char buffer[64] = {0};
    char* end = NULL;
    int fd = open(path, O_RDONLY);
    if (fd < 0) {
        return 0;
    }
    ssize_t len = read(fd, buffer, sizeof(buffer) - 1);
    close(fd);
    if (len < 0) {
        return 0;
    }
    return strtoll(buffer, &end, 10);
}

// Smartisan (factory PICO OS 5.13.7 android_os_Debug_getPssPid_core): the graphics memory added
// to the Pss/Uss/Rss comes from the KGSL nodes of the process (imported_mem = EGL,
// gpumem_unmapped = GL) instead of memtrack; outEglGl, if it has 2 entries, receives the EGL and
// GL sizes in kB.
static jlong android_os_Debug_getPssPid_core(JNIEnv *env, jobject clazz, jint pid,
        jlongArray outUssSwapPssRss, jlongArray outMemtrack, jlongArray outEglGl)
{
    char eglPath[128] = {0};
    char glPath[128] = {0};
    snprintf(eglPath, sizeof(eglPath), "/sys/class/kgsl/kgsl/proc/%d/imported_mem", pid);
    snprintf(glPath, sizeof(glPath), "/sys/class/kgsl/kgsl/proc/%d/gpumem_unmapped", pid);
    jlong egl = read_kgsl_proc_mem(eglPath);
    jlong gl = read_kgsl_proc_mem(glPath);

    jlong memtrack = (egl + gl) / 1024;
    jlong pss = memtrack;
    jlong rss = memtrack;
    jlong swapPss = 0;
    jlong uss = memtrack;

    ::android::meminfo::ProcMemInfo proc_mem(pid);
    ::android::meminfo::MemUsage stats;
    if (proc_mem.SmapsOrRollup(&stats)) {
        pss += stats.pss;
        uss += stats.uss;
        rss += stats.rss;
        swapPss = stats.swap_pss;
        pss += swapPss; // Also in swap, those pages would be accounted as Pss without SWAP
    }

    if (outUssSwapPssRss != NULL) {
        if (env->GetArrayLength(outUssSwapPssRss) >= 1) {
            jlong* outUssSwapPssRssArray = env->GetLongArrayElements(outUssSwapPssRss, 0);
            if (outUssSwapPssRssArray != NULL) {
                outUssSwapPssRssArray[0] = uss;
                if (env->GetArrayLength(outUssSwapPssRss) >= 2) {
                    outUssSwapPssRssArray[1] = swapPss;
                }
                if (env->GetArrayLength(outUssSwapPssRss) >= 3) {
                    outUssSwapPssRssArray[2] = rss;
                }
            }
            env->ReleaseLongArrayElements(outUssSwapPssRss, outUssSwapPssRssArray, 0);
        }
    }

    if (outMemtrack != NULL) {
        if (env->GetArrayLength(outMemtrack) >= 1) {
            jlong* outMemtrackArray = env->GetLongArrayElements(outMemtrack, 0);
            if (outMemtrackArray != NULL) {
                outMemtrackArray[0] = memtrack;
            }
            env->ReleaseLongArrayElements(outMemtrack, outMemtrackArray, 0);
        }
    }

    if (outEglGl != NULL) {
        if (env->GetArrayLength(outEglGl) >= 2) {
            jlong* outEglGlArray = env->GetLongArrayElements(outEglGl, 0);
            if (outEglGlArray != NULL) {
                outEglGlArray[0] = egl / 1024;
                outEglGlArray[1] = gl / 1024;
            }
            env->ReleaseLongArrayElements(outEglGl, outEglGlArray, 0);
        }
    }

    return pss;
}

static jlong android_os_Debug_getPssPid(JNIEnv *env, jobject clazz, jint pid,
        jlongArray outUssSwapPssRss, jlongArray outMemtrack)
{
    return android_os_Debug_getPssPid_core(env, clazz, pid, outUssSwapPssRss, outMemtrack, NULL);
}

static jlong android_os_Debug_getPss(JNIEnv *env, jobject clazz)
{
    return android_os_Debug_getPssPid_core(env, clazz, getpid(), NULL, NULL, NULL);
}

// The 1:1 mapping of MEMINFO_* enums here must match with the constants from
// Debug.java.
enum {
    MEMINFO_TOTAL,
    MEMINFO_FREE,
    MEMINFO_BUFFERS,
    MEMINFO_CACHED,
    MEMINFO_SHMEM,
    MEMINFO_SLAB,
    MEMINFO_SLAB_RECLAIMABLE,
    MEMINFO_SLAB_UNRECLAIMABLE,
    MEMINFO_SWAP_TOTAL,
    MEMINFO_SWAP_FREE,
    MEMINFO_ZRAM_TOTAL,
    MEMINFO_MAPPED,
    MEMINFO_VMALLOC_USED,
    MEMINFO_PAGE_TABLES,
    MEMINFO_KERNEL_STACK,
    MEMINFO_GFX_CACHED,
    MEMINFO_AVAILABLE,
    MEMINFO_COUNT
};

static void android_os_Debug_getMemInfo(JNIEnv *env, jobject clazz, jlongArray out)
{
    if (out == NULL) {
        jniThrowNullPointerException(env, "out == null");
        return;
    }

    int outLen = env->GetArrayLength(out);
    if (outLen < MEMINFO_COUNT) {
        jniThrowRuntimeException(env, "outLen < MEMINFO_COUNT");
        return;
    }

    // Read system memory info including ZRAM. The values are stored in the vector
    // in the same order as MEMINFO_* enum
    std::vector<uint64_t> mem(MEMINFO_COUNT);
    std::vector<std::string> tags(::android::meminfo::SysMemInfo::kDefaultSysMemInfoTags);
    tags.insert(tags.begin() + MEMINFO_ZRAM_TOTAL, "Zram:");
    ::android::meminfo::SysMemInfo smi;
    if (!smi.ReadMemInfo(tags, &mem)) {
        jniThrowRuntimeException(env, "SysMemInfo read failed");
        return;
    }
    // ReadMemInfo() resizes mem to the number of tags; the factory libmeminfo default tags end
    // with "GFX_cached:" and "MemAvailable:" (MEMINFO_GFX_CACHED, MEMINFO_AVAILABLE). Keep the
    // missing trailing entries at 0 when the libmeminfo in use does not have them.
    mem.resize(MEMINFO_COUNT);

    jlong* outArray = env->GetLongArrayElements(out, 0);
    if (outArray != NULL) {
        outLen = MEMINFO_COUNT;
        for (int i = 0; i < outLen; i++) {
            if (i == MEMINFO_VMALLOC_USED && mem[i] == 0) {
                outArray[i] = smi.ReadVmallocInfo() / 1024;
                continue;
            }
            outArray[i] = mem[i];
        }
    }

    env->ReleaseLongArrayElements(out, outArray, 0);
}

// Smartisan (factory PICO OS 5.13.7): logs the meminfo of all processes (libmeminfo_utils
// GetAllProcsMeminfoFast) under a "--- Dump all procs meminfo, reason: <callReason> --- " header
// and appends the lines to outLines if it is not null.
static jboolean android_os_Debug_getAllProcsMeminfoFast(JNIEnv* env, jobject clazz,
        jstring callReason, jobject outLines)
{
    std::vector<std::string> lines;
    const char* reason = env->GetStringUTFChars(callReason, NULL);
    lines.push_back("--- Dump all procs meminfo, reason: " + std::string(reason) + " --- ");
    env->ReleaseStringUTFChars(callReason, reason);

    ::android::meminfo_utils::SysMemInfo_utils smi;
    if (!smi.GetAllProcsMeminfoFast(&lines)) {
        return JNI_FALSE;
    }

    if (outLines != NULL && !lines.empty()) {
        int count = lines.size();
        jclass arrayListClass = env->FindClass("java/util/ArrayList");
        jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
        for (int i = 0; i < count; i++) {
            env->CallBooleanMethod(outLines, arrayListAdd, env->NewStringUTF(lines[i].c_str()));
        }
        env->DeleteLocalRef(arrayListClass);
    }
    return JNI_TRUE;
}

// Smartisan: parses /proc/meminfo directly (without libmeminfo), including the ION and zram
// physical usage lines of the factory kernel.
static void android_os_Debug_getMemInfoFast(JNIEnv *env, jobject clazz, jlongArray out)
{
    char buffer[2048];
    size_t numFound = 0;

    if (out == NULL) {
        jniThrowNullPointerException(env, "out == null");
        return;
    }

    int fd = open("/proc/meminfo", O_RDONLY);

    if (fd < 0) {
        ALOGW("Unable to open /proc/meminfo: %s\n", strerror(errno));
        return;
    }

    int len = read(fd, buffer, sizeof(buffer)-1);
    close(fd);

    if (len < 0) {
        ALOGW("Empty /proc/meminfo");
        return;
    }
    buffer[len] = 0;

    static const char* const tags[] = {
            "MemTotal:",
            "MemFree:",
            "Buffers:",
            "Cached:",
            "Shmem:",
            "Slab:",
            "SReclaimable:",
            "SUnreclaim:",
            "SwapTotal:",
            "SwapFree:",
            "ZRam:",
            "Mapped:",
            "VmallocUsed:",
            "PageTables:",
            "KernelStack:",
            "ION_system:",
            "ION_cached:",
            "ZRAM_phy_used:",
            NULL
    };
    static const int tagsLen[] = {
            9,
            8,
            8,
            7,
            6,
            5,
            13,
            11,
            10,
            9,
            5,
            7,
            12,
            11,
            12,
            11,
            11,
            14,
            0
    };
    long mem[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    char* p = buffer;
    while (*p && numFound < (sizeof(tagsLen) / sizeof(tagsLen[0]))) {
        int i = 0;
        while (tags[i]) {
            if (strncmp(p, tags[i], tagsLen[i]) == 0) {
                p += tagsLen[i];
                while (*p == ' ') p++;
                char* num = p;
                while (*p >= '0' && *p <= '9') p++;
                if (*p != 0) {
                    *p = 0;
                    p++;
                }
                mem[i] = atoll(num);
                numFound++;
                break;
            }
            i++;
        }
        while (*p && *p != '\n') {
            p++;
        }
        if (*p) p++;
    }

    int maxNum = env->GetArrayLength(out);
    if (maxNum > MEMINFO_COUNT) {
        maxNum = MEMINFO_COUNT;
    }
    jlong* outArray = env->GetLongArrayElements(out, 0);
    if (outArray != NULL) {
        for (int i = 0; i < maxNum; i++) {
            outArray[i] = mem[i];
        }
    }
    env->ReleaseLongArrayElements(out, outArray, 0);
}

static jint read_binder_stat(const char* stat)
{
    UniqueFile fp = MakeUniqueFile(BINDER_STATS, "re");
    if (fp == nullptr) {
        return -1;
    }

    char line[1024];

    char compare[128];
    int len = snprintf(compare, 128, "proc %d", getpid());

    // loop until we have the block that represents this process
    do {
        if (fgets(line, 1024, fp.get()) == 0) {
            return -1;
        }
    } while (strncmp(compare, line, len));

    // now that we have this process, read until we find the stat that we are looking for
    len = snprintf(compare, 128, "  %s: ", stat);

    do {
        if (fgets(line, 1024, fp.get()) == 0) {
            return -1;
        }
    } while (strncmp(compare, line, len));

    // we have the line, now increment the line ptr to the value
    char* ptr = line + len;
    jint result = atoi(ptr);
    return result;
}

static jint android_os_Debug_getBinderSentTransactions(JNIEnv *env, jobject clazz)
{
    return read_binder_stat("bcTRANSACTION");
}

static jint android_os_getBinderReceivedTransactions(JNIEnv *env, jobject clazz)
{
    return read_binder_stat("brTRANSACTION");
}

// these are implemented in android_util_Binder.cpp
jint android_os_Debug_getLocalObjectCount(JNIEnv* env, jobject clazz);
jint android_os_Debug_getProxyObjectCount(JNIEnv* env, jobject clazz);
jint android_os_Debug_getDeathObjectCount(JNIEnv* env, jobject clazz);

static bool openFile(JNIEnv* env, jobject fileDescriptor, UniqueFile& fp)
{
    if (fileDescriptor == NULL) {
        jniThrowNullPointerException(env, "fd == null");
        return false;
    }
    int origFd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    if (origFd < 0) {
        jniThrowRuntimeException(env, "Invalid file descriptor");
        return false;
    }

    /* dup() the descriptor so we don't close the original with fclose() */
    int fd = fcntl(origFd, F_DUPFD_CLOEXEC, 0);
    if (fd < 0) {
        ALOGW("dup(%d) failed: %s\n", origFd, strerror(errno));
        jniThrowRuntimeException(env, "dup() failed");
        return false;
    }

    fp.reset(fdopen(fd, "w"));
    if (fp == nullptr) {
        ALOGW("fdopen(%d) failed: %s\n", fd, strerror(errno));
        close(fd);
        jniThrowRuntimeException(env, "fdopen() failed");
        return false;
    }
    return true;
}

/*
 * Dump the native heap, writing human-readable output to the specified
 * file descriptor.
 */
static void android_os_Debug_dumpNativeHeap(JNIEnv* env, jobject,
    jobject fileDescriptor)
{
    UniqueFile fp(nullptr, safeFclose);
    if (!openFile(env, fileDescriptor, fp)) {
        return;
    }

    ALOGD("Native heap dump starting...\n");
    // Formatting of the native heap dump is handled by malloc debug itself.
    // See https://android.googlesource.com/platform/bionic/+/master/libc/malloc_debug/README.md#backtrace-heap-dump-format
    if (android_mallopt(M_WRITE_MALLOC_LEAK_INFO_TO_FILE, fp.get(), sizeof(FILE*))) {
      ALOGD("Native heap dump complete.\n");
    } else {
      PLOG(ERROR) << "Failed to write native heap dump to file";
    }
}

/*
 * Dump the native malloc info, writing xml output to the specified
 * file descriptor.
 */
static void android_os_Debug_dumpNativeMallocInfo(JNIEnv* env, jobject,
    jobject fileDescriptor)
{
    UniqueFile fp(nullptr, safeFclose);
    if (!openFile(env, fileDescriptor, fp)) {
        return;
    }

    malloc_info(0, fp.get());
}

static bool dumpTraces(JNIEnv* env, jint pid, jstring fileName, jint timeoutSecs,
                       DebuggerdDumpType dumpType) {
    const ScopedUtfChars fileNameChars(env, fileName);
    if (fileNameChars.c_str() == nullptr) {
        return false;
    }

    android::base::unique_fd fd(open(fileNameChars.c_str(),
                                     O_CREAT | O_WRONLY | O_NOFOLLOW | O_CLOEXEC | O_APPEND,
                                     0666));
    if (fd < 0) {
        PLOG(ERROR) << "Can't open " << fileNameChars.c_str();
        return false;
    }

    int res = dump_backtrace_to_file_timeout(pid, dumpType, timeoutSecs, fd);
    if (fdatasync(fd.get()) != 0) {
        PLOG(ERROR) << "Failed flushing trace.";
    }
    return res == 0;
}

static jboolean android_os_Debug_dumpJavaBacktraceToFileTimeout(JNIEnv* env, jobject clazz,
        jint pid, jstring fileName, jint timeoutSecs) {
    const bool ret = dumpTraces(env, pid, fileName, timeoutSecs, kDebuggerdJavaBacktrace);
    return ret ? JNI_TRUE : JNI_FALSE;
}

static jboolean android_os_Debug_dumpNativeBacktraceToFileTimeout(JNIEnv* env, jobject clazz,
        jint pid, jstring fileName, jint timeoutSecs) {
    const bool ret = dumpTraces(env, pid, fileName, timeoutSecs, kDebuggerdNativeBacktrace);
    return ret ? JNI_TRUE : JNI_FALSE;
}

static jstring android_os_Debug_getUnreachableMemory(JNIEnv* env, jobject clazz,
    jint limit, jboolean contents)
{
    std::string s = GetUnreachableMemoryString(contents, limit);
    return env->NewStringUTF(s.c_str());
}

static jlong android_os_Debug_getFreeZramKb(JNIEnv* env, jobject clazz) {

    jlong zramFreeKb = 0;

    std::string status_path = android::base::StringPrintf("/proc/meminfo");
    UniqueFile file = MakeUniqueFile(status_path.c_str(), "re");

    char line[256];
    while (file != nullptr && fgets(line, sizeof(line), file.get())) {
        jlong v;
        if (sscanf(line, "SwapFree: %" SCNd64 " kB", &v) == 1) {
            zramFreeKb = v;
            break;
        }
    }

    return zramFreeKb;
}

// ION figures of the factory PICO OS 5.13.7 libandroid_runtime (later Android 10): the heaps and
// pools totals are 0 when their sysfs files are not readable; the mapped size is the sum, in kB,
// of the dmabufs mapped by all processes (map references only).
static jlong android_os_Debug_getIonHeapsSizeKb(JNIEnv* env, jobject clazz) {
    jlong heapsSizeKb = 0;
    uint64_t size;

    if (meminfo::ReadIonHeapsSizeKb(&size)) {
        heapsSizeKb = size;
    }

    return heapsSizeKb;
}

static jlong android_os_Debug_getIonPoolsSizeKb(JNIEnv* env, jobject clazz) {
    jlong poolsSizeKb = 0;
    uint64_t size;

    if (meminfo::ReadIonPoolsSizeKb(&size)) {
        poolsSizeKb = size;
    }

    return poolsSizeKb;
}

static jlong android_os_Debug_getIonMappedSizeKb(JNIEnv* env, jobject clazz) {
    jlong ionPss = 0;
    std::vector<dmabufinfo::DmaBuffer> dmabufs;

    std::unique_ptr<DIR, int (*)(DIR*)> dir(opendir("/proc"), closedir);
    if (!dir) {
        LOG(ERROR) << "Failed to open /proc directory";
        return false;
    }

    struct dirent* dent;
    while ((dent = readdir(dir.get()))) {
        if (dent->d_type != DT_DIR) continue;

        int pid = atoi(dent->d_name);
        if (pid == 0) {
            continue;
        }

        if (!dmabufinfo::AppendDmaBufInfo(pid, &dmabufs, false)) {
            LOG(ERROR) << "Failed to read maps for pid " << pid;
        }
    }

    for (dmabufinfo::DmaBuffer buf : dmabufs) {
        ionPss += buf.size() / 1024;
    }

    return ionPss;
}

// Smartisan (factory PICO OS 5.13.7): QTimer clock of Debug.getTimeByQtimer(). The factory
// libandroid_runtime uses the Qualcomm sensors_timeutil helper (function-static instance): the
// ARM generic timer counter (CNTVCT) is scaled to nanoseconds with its frequency (CNTFRQ), and
// the instance also records the elapsedRealtimeNano() offset of the counter when created.
class sensors_timeutil {
public:
    static sensors_timeutil& get_instance() {
        static sensors_timeutil inst;
        return inst;
    }

    uint64_t qtimer_get_ticks() {
#if defined(__aarch64__)
        uint64_t val = 0;
        asm volatile("mrs %0, cntvct_el0" : "=r" (val));
        return val;
#else
        uint32_t lsb = 0, msb = 0;
        asm volatile("mrrc p15, 1, %[lsb], %[msb], c14" : [lsb] "=r" (lsb), [msb] "=r" (msb));
        return ((uint64_t) msb << 32) | lsb;
#endif
    }

    uint64_t qtimer_get_freq() {
#if defined(__aarch64__)
        uint64_t val = 0;
        asm volatile("mrs %0, cntfrq_el0" : "=r" (val));
        return val;
#else
        uint32_t val = 0;
        asm volatile("mrc p15, 0, %[val], c14, c0, 0" : [val] "=r" (val));
        return val;
#endif
    }

    uint64_t qtimer_ticks_to_ns(uint64_t ticks) {
        return uint64_t(double(ticks) * (double(_nsec_per_sec) / double(_qtimer_freq)));
    }

    uint64_t qtimer_get_time_ns() {
        return qtimer_ticks_to_ns(qtimer_get_ticks());
    }

private:
    sensors_timeutil() :
        _qtimer_freq(qtimer_get_freq()),
        _realtime_ns(elapsedRealtimeNano())
    {
        _offset_ns = _realtime_ns.load() - qtimer_get_time_ns();
    }

public:
    // Layout of the factory instance; only _qtimer_freq and _nsec_per_sec are read by
    // getTimeByQtimer().
    uint64_t _qtimer_freq;
    const uint64_t _nsec_per_sec = 1000000000ull;
    std::atomic<int64_t> _realtime_ns;
    const int64_t _offset_update_interval_ns = 60000000000ll;
    const int64_t _offset_max_error_ns = 10000;
    const int _offset_retries = 20;
    int64_t _offset_ns;
};

static jlong android_os_Debug_getTimeByQtimer(JNIEnv* env, jobject clazz)
{
    return (uint64_t) (sensors_timeutil::get_instance().qtimer_get_time_ns() * 0.000001);
}

/*
 * JNI registration.
 */

// Order as in the factory PICO OS 5.13.7 table, which registers getAllProcsMeminfoFast twice.
static const JNINativeMethod gMethods[] = {
    { "getNativeHeapSize",      "()J",
            (void*) android_os_Debug_getNativeHeapSize },
    { "getMemInfoFast",         "([J)V",
            (void*) android_os_Debug_getMemInfoFast },
    { "getAllProcsMeminfoFast", "(Ljava/lang/String;Ljava/util/ArrayList;)Z",
            (void*) android_os_Debug_getAllProcsMeminfoFast },
    { "getAllProcsMeminfoFast", "(Ljava/lang/String;Ljava/util/ArrayList;)Z",
            (void*) android_os_Debug_getAllProcsMeminfoFast },
    { "getNativeHeapAllocatedSize", "()J",
            (void*) android_os_Debug_getNativeHeapAllocatedSize },
    { "getNativeHeapFreeSize",  "()J",
            (void*) android_os_Debug_getNativeHeapFreeSize },
    { "getMemoryInfo",          "(Landroid/os/Debug$MemoryInfo;)V",
            (void*) android_os_Debug_getDirtyPages },
    { "getMemoryInfo",          "(ILandroid/os/Debug$MemoryInfo;)V",
            (void*) android_os_Debug_getDirtyPagesPid },
    { "getPss",                 "()J",
            (void*) android_os_Debug_getPss },
    { "getPss",                 "(I[J[J)J",
            (void*) android_os_Debug_getPssPid },
    { "getPss",                 "(I[J[J[J)J",
            (void*) android_os_Debug_getPssPid_core },
    { "getTimeByQtimer",        "()J",
            (void*) android_os_Debug_getTimeByQtimer },
    { "getMemInfo",             "([J)V",
            (void*) android_os_Debug_getMemInfo },
    { "dumpNativeHeap",         "(Ljava/io/FileDescriptor;)V",
            (void*) android_os_Debug_dumpNativeHeap },
    { "dumpNativeMallocInfo",   "(Ljava/io/FileDescriptor;)V",
            (void*) android_os_Debug_dumpNativeMallocInfo },
    { "getBinderSentTransactions", "()I",
            (void*) android_os_Debug_getBinderSentTransactions },
    { "getBinderReceivedTransactions", "()I",
            (void*) android_os_getBinderReceivedTransactions },
    { "getBinderLocalObjectCount", "()I",
            (void*)android_os_Debug_getLocalObjectCount },
    { "getBinderProxyObjectCount", "()I",
            (void*)android_os_Debug_getProxyObjectCount },
    { "getBinderDeathObjectCount", "()I",
            (void*)android_os_Debug_getDeathObjectCount },
    { "dumpJavaBacktraceToFileTimeout", "(ILjava/lang/String;I)Z",
            (void*)android_os_Debug_dumpJavaBacktraceToFileTimeout },
    { "dumpNativeBacktraceToFileTimeout", "(ILjava/lang/String;I)Z",
            (void*)android_os_Debug_dumpNativeBacktraceToFileTimeout },
    { "getUnreachableMemory", "(IZ)Ljava/lang/String;",
            (void*)android_os_Debug_getUnreachableMemory },
    { "getZramFreeKb", "()J",
            (void*)android_os_Debug_getFreeZramKb },
    { "getIonHeapsSizeKb", "()J",
            (void*)android_os_Debug_getIonHeapsSizeKb },
    { "getIonPoolsSizeKb", "()J",
            (void*)android_os_Debug_getIonPoolsSizeKb },
    { "getIonMappedSizeKb", "()J",
            (void*)android_os_Debug_getIonMappedSizeKb },
};

int register_android_os_Debug(JNIEnv *env)
{
    jclass clazz = env->FindClass("android/os/Debug$MemoryInfo");

    // Sanity check the number of other statistics expected in Java matches here.
    jfieldID numOtherStats_field = env->GetStaticFieldID(clazz, "NUM_OTHER_STATS", "I");
    jint numOtherStats = env->GetStaticIntField(clazz, numOtherStats_field);
    jfieldID numDvkStats_field = env->GetStaticFieldID(clazz, "NUM_DVK_STATS", "I");
    jint numDvkStats = env->GetStaticIntField(clazz, numDvkStats_field);
    int expectedNumOtherStats = _NUM_HEAP - _NUM_CORE_HEAP;
    if ((numOtherStats + numDvkStats) != expectedNumOtherStats) {
        jniThrowExceptionFmt(env, "java/lang/RuntimeException",
                             "android.os.Debug.Meminfo.NUM_OTHER_STATS+android.os.Debug.Meminfo.NUM_DVK_STATS=%d expected %d",
                             numOtherStats+numDvkStats, expectedNumOtherStats);
        return JNI_ERR;
    }

    otherStats_field = env->GetFieldID(clazz, "otherStats", "[I");
    hasSwappedOutPss_field = env->GetFieldID(clazz, "hasSwappedOutPss", "Z");

    for (int i=0; i<_NUM_CORE_HEAP; i++) {
        stat_fields[i].pss_field =
                env->GetFieldID(clazz, stat_field_names[i].pss_name, "I");
        stat_fields[i].pssSwappable_field =
                env->GetFieldID(clazz, stat_field_names[i].pssSwappable_name, "I");
        stat_fields[i].rss_field =
                env->GetFieldID(clazz, stat_field_names[i].rss_name, "I");
        stat_fields[i].privateDirty_field =
                env->GetFieldID(clazz, stat_field_names[i].privateDirty_name, "I");
        stat_fields[i].sharedDirty_field =
                env->GetFieldID(clazz, stat_field_names[i].sharedDirty_name, "I");
        stat_fields[i].privateClean_field =
                env->GetFieldID(clazz, stat_field_names[i].privateClean_name, "I");
        stat_fields[i].sharedClean_field =
                env->GetFieldID(clazz, stat_field_names[i].sharedClean_name, "I");
        stat_fields[i].swappedOut_field =
                env->GetFieldID(clazz, stat_field_names[i].swappedOut_name, "I");
        stat_fields[i].swappedOutPss_field =
                env->GetFieldID(clazz, stat_field_names[i].swappedOutPss_name, "I");
    }

    return jniRegisterNativeMethods(env, "android/os/Debug", gMethods, NELEM(gMethods));
}

}; // namespace android
