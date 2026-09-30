// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
//
// Exercises the PICO SurfaceTexture/ImageManagerExt extension of libhwui through the factory
// public library libpxrguiex.so, like a VR app does:
//   initImageManagerExtThread, setOnFrameAvailableCallbackAndTextureIdExt, setTexName,
//   acquireTexture, createFence, releaseTexture, getTimestamp/getFrameNumber and
//   globalUpdateTexImageExt on a SurfaceTexture fed by a CPU producer through a BufferQueue.
// Prints one line per check and "RESULT: PASS" / "RESULT: FAIL (n)"; exit status 0 on success.

#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <android/native_window.h>
#include <dlfcn.h>
#include <gui/BufferQueue.h>
#include <gui/Surface.h>
#include <stdio.h>
#include <string.h>
#include <system/window.h>
#include <unistd.h>

#include <atomic>

#include "surfacetexture/SurfaceTexture.h"

using namespace android;

namespace {

int gFailures = 0;

void check(bool ok, const char* what) {
    printf("%s: %s\n", ok ? "OK  " : "FAIL", what);
    if (!ok) gFailures++;
}

// libpxrguiex entry points (the long handle is a raw android::SurfaceTexture*).
struct Pxr {
    int (*initImageManagerExtThread)();
    int (*setOnFrameAvailableCallbackAndTextureIdExt)(long, long, int);
    void (*setTexName)(long, int*, int);
    int (*acquireTexture)(long, int*);
    void (*releaseTexture)(long, int, int);
    int (*createFence)();
    long (*getTimestamp)(long);
    long (*getFrameNumber)(long);
    int (*globalUpdateTexImageExt)(long);
};

template <typename T>
bool sym(void* lib, const char* name, T* out) {
    *out = reinterpret_cast<T>(dlsym(lib, name));
    if (*out == nullptr) printf("FAIL: dlsym %s: %s\n", name, dlerror());
    return *out != nullptr;
}

std::atomic<int> gCallbackArg(-1);
std::atomic<int> gCallbackCount(0);
void onFrame(int arg) {
    gCallbackArg = arg;
    gCallbackCount++;
}

const char* kVs =
        "attribute vec2 p; varying vec2 t;\n"
        "void main() { t = p * 0.5 + 0.5; gl_Position = vec4(p, 0.0, 1.0); }\n";
const char* kFs =
        "#extension GL_OES_EGL_image_external : require\n"
        "precision mediump float; varying vec2 t; uniform samplerExternalOES s;\n"
        "void main() { gl_FragColor = texture2D(s, t); }\n";

GLuint compile(GLenum type, const char* src) {
    GLuint s = glCreateShader(type);
    glShaderSource(s, 1, &src, nullptr);
    glCompileShader(s);
    return s;
}

// Draws texture tex (GL_TEXTURE_EXTERNAL_OES) to the 16x16 pbuffer and returns the RGBA center.
uint32_t drawAndRead(GLuint program, GLuint tex) {
    static const GLfloat quad[] = {-1, -1, 1, -1, -1, 1, 1, 1};
    glViewport(0, 0, 16, 16);
    glClearColor(0, 0, 0, 1);
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(program);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glUniform1i(glGetUniformLocation(program, "s"), 0);
    GLint p = glGetAttribLocation(program, "p");
    glEnableVertexAttribArray(p);
    glVertexAttribPointer(p, 2, GL_FLOAT, GL_FALSE, 0, quad);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    uint32_t pixel = 0;
    glReadPixels(8, 8, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, &pixel);
    return pixel;
}

// Queues one 64x64 frame of a single RGBA color from the CPU.
bool queueFrame(ANativeWindow* window, uint32_t rgba) {
    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(window, &buffer, nullptr) != 0) return false;
    for (int y = 0; y < buffer.height; y++) {
        uint32_t* row = static_cast<uint32_t*>(buffer.bits) + y * buffer.stride;
        for (int x = 0; x < buffer.width; x++) row[x] = rgba;
    }
    return ANativeWindow_unlockAndPost(window) == 0;
}

int acquireWithRetry(const Pxr& pxr, long handle, int* tex) {
    int rc = 6;
    for (int i = 0; i < 200 && rc == 6; i++) {
        rc = pxr.acquireTexture(handle, tex);
        if (rc == 6) usleep(5000);
    }
    return rc;
}

}  // namespace

int main(int argc, char** argv) {
    // --link-only: stop after linking, for an isolated run whose EGL loader would pick up the
    // installed /system EGL wrappers (the loader dlopens them by absolute path).
    const bool linkOnly = argc > 1 && strcmp(argv[1], "--link-only") == 0;
    void* lib = dlopen("libpxrguiex.so", RTLD_NOW);
    if (lib == nullptr) {
        printf("FAIL: dlopen libpxrguiex.so: %s\nRESULT: FAIL (1)\n", dlerror());
        return 1;
    }
    Pxr pxr;
    bool ok = sym(lib, "_Z25initImageManagerExtThreadv", &pxr.initImageManagerExtThread) &
              sym(lib, "_Z42setOnFrameAvailableCallbackAndTextureIdExtlli",
                  &pxr.setOnFrameAvailableCallbackAndTextureIdExt) &
              sym(lib, "_Z10setTexNamelPii", &pxr.setTexName) &
              sym(lib, "_Z14acquireTexturelPi", &pxr.acquireTexture) &
              sym(lib, "_Z14releaseTexturelii", &pxr.releaseTexture) &
              sym(lib, "_Z11createFencev", &pxr.createFence) &
              sym(lib, "_Z12getTimestampl", &pxr.getTimestamp) &
              sym(lib, "_Z14getFrameNumberl", &pxr.getFrameNumber) &
              sym(lib, "_Z23globalUpdateTexImageExtl", &pxr.globalUpdateTexImageExt);
    check(ok, "dlopen libpxrguiex.so and resolve its entry points");
    if (!ok) {
        printf("RESULT: FAIL (%d)\n", gFailures);
        return 1;
    }
    if (linkOnly) {
        Dl_info info;
        const bool hwui = dladdr(dlsym(RTLD_DEFAULT, "_ZN7android15ImageManagerExt10initThreadEv"), &info) != 0;
        printf("%s: %s\n", hwui ? "OK  " : "FAIL", "ImageManagerExt::initThread bound in the loaded libhwui");
        if (hwui) printf("libhwui: %s\n", info.dli_fname);
        printf(hwui ? "RESULT: PASS\n" : "RESULT: FAIL (1)\n");
        return hwui ? 0 : 1;
    }

    // EGL/GLES2 context on a 16x16 pbuffer.
    EGLDisplay dpy = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglInitialize(dpy, nullptr, nullptr);
    const EGLint configAttribs[] = {EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, EGL_RENDERABLE_TYPE,
                                    EGL_OPENGL_ES2_BIT, EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8,
                                    EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE};
    EGLConfig config;
    EGLint numConfigs = 0;
    eglChooseConfig(dpy, configAttribs, &config, 1, &numConfigs);
    const EGLint pbufferAttribs[] = {EGL_WIDTH, 16, EGL_HEIGHT, 16, EGL_NONE};
    EGLSurface surface = eglCreatePbufferSurface(dpy, config, pbufferAttribs);
    const EGLint contextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    EGLContext context = eglCreateContext(dpy, config, EGL_NO_CONTEXT, contextAttribs);
    check(numConfigs > 0 && eglMakeCurrent(dpy, surface, surface, context) == EGL_TRUE,
          "EGL pbuffer context current");

    GLuint program = glCreateProgram();
    glAttachShader(program, compile(GL_VERTEX_SHADER, kVs));
    glAttachShader(program, compile(GL_FRAGMENT_SHADER, kFs));
    glLinkProgram(program);

    int tid = pxr.initImageManagerExtThread();
    printf("      initImageManagerExtThread() = %d\n", tid);
    check(tid > 0, "initImageManagerExtThread returns the ImageManagerExt thread id");

    // VR compositor SurfaceTexture fed by a CPU producer.
    GLuint names[3];
    glGenTextures(3, names);
    sp<IGraphicBufferProducer> producer;
    sp<IGraphicBufferConsumer> consumer;
    BufferQueue::createBufferQueue(&producer, &consumer);
    sp<SurfaceTexture> st =
            new SurfaceTexture(consumer, names[0], GL_TEXTURE_EXTERNAL_OES, true, false);
    st->setDefaultBufferSize(64, 64);
    long handle = reinterpret_cast<long>(st.get());

    check(pxr.setOnFrameAvailableCallbackAndTextureIdExt(handle, reinterpret_cast<long>(onFrame),
                                                         42) == 0,
          "setOnFrameAvailableCallbackAndTextureIdExt");
    int texNames[3] = {static_cast<int>(names[0]), static_cast<int>(names[1]),
                       static_cast<int>(names[2])};
    pxr.setTexName(handle, texNames, 3);

    sp<Surface> window = new Surface(producer, false);
    ANativeWindow* anw = window.get();
    native_window_api_connect(anw, NATIVE_WINDOW_API_CPU);
    native_window_set_buffers_format(anw, HAL_PIXEL_FORMAT_RGBA_8888);
    native_window_set_buffers_dimensions(anw, 64, 64);

    // Frame 1: red.
    check(queueFrame(anw, 0xff0000ff), "queue frame 1 (red)");
    for (int i = 0; i < 100 && gCallbackCount == 0; i++) usleep(10000);
    printf("      frame callback count %d arg %d\n", gCallbackCount.load(), gCallbackArg.load());
    check(gCallbackCount == 1 && gCallbackArg == 42, "frame callback called with texture id 42");

    int tex = -1;
    int rc = acquireWithRetry(pxr, handle, &tex);
    printf("      acquireTexture = %d, texName %d (names %u %u %u)\n", rc, tex, names[0],
           names[1], names[2]);
    check(rc == 0 && tex == static_cast<int>(names[0]), "acquireTexture binds frame 1");
    uint32_t pixel = drawAndRead(program, tex);
    printf("      frame 1 pixel 0x%08x\n", pixel);
    check(pixel == 0xff0000ff, "frame 1 content is red");
    printf("      getTimestamp %ld getFrameNumber %ld\n", pxr.getTimestamp(handle),
           pxr.getFrameNumber(handle));
    check(pxr.getFrameNumber(handle) == 1, "getFrameNumber == 1");

    int fence = pxr.createFence();
    printf("      createFence = %d\n", fence);
    check(fence >= 0, "createFence returns a native fence fd");
    pxr.releaseTexture(handle, tex, fence);

    // Frame 2: green, bound to the first free texture name again.
    check(queueFrame(anw, 0xff00ff00), "queue frame 2 (green)");
    tex = -1;
    rc = acquireWithRetry(pxr, handle, &tex);
    printf("      acquireTexture = %d, texName %d\n", rc, tex);
    check(rc == 0 && tex == static_cast<int>(names[0]), "acquireTexture binds frame 2");
    pixel = drawAndRead(program, tex);
    printf("      frame 2 pixel 0x%08x\n", pixel);
    check(pixel == 0xff00ff00, "frame 2 content is green");
    pxr.releaseTexture(handle, tex, -1);

    // Nothing queued: acquireTexture reports no texture.
    tex = 123;
    rc = pxr.acquireTexture(handle, &tex);
    printf("      acquireTexture (empty queue) = %d, texName %d\n", rc, tex);
    check(rc == 0 && tex == -1, "acquireTexture with an empty queue");

    native_window_api_disconnect(anw, NATIVE_WINDOW_API_CPU);
    window.clear();

    // Regular (non VR) SurfaceTexture through globalUpdateTexImageExt: reads the flags at the
    // factory offsets (4872/4873 arm64, 2776/2777 arm32).
    sp<IGraphicBufferProducer> producer2;
    sp<IGraphicBufferConsumer> consumer2;
    BufferQueue::createBufferQueue(&producer2, &consumer2);
    GLuint tex2;
    glGenTextures(1, &tex2);
    sp<SurfaceTexture> st2 = new SurfaceTexture(consumer2, tex2, GL_TEXTURE_EXTERNAL_OES, true,
                                                false);
    st2->setDefaultBufferSize(64, 64);
    sp<Surface> window2 = new Surface(producer2, false);
    ANativeWindow* anw2 = window2.get();
    native_window_api_connect(anw2, NATIVE_WINDOW_API_CPU);
    native_window_set_buffers_format(anw2, HAL_PIXEL_FORMAT_RGBA_8888);
    native_window_set_buffers_dimensions(anw2, 64, 64);
    check(queueFrame(anw2, 0xffff0000), "queue frame (blue) to a regular SurfaceTexture");
    rc = pxr.globalUpdateTexImageExt(reinterpret_cast<long>(st2.get()));
    printf("      globalUpdateTexImageExt = %d\n", rc);
    check(rc == 0, "globalUpdateTexImageExt (not protected content)");
    pixel = drawAndRead(program, tex2);
    printf("      regular frame pixel 0x%08x\n", pixel);
    check(pixel == 0xffff0000, "regular SurfaceTexture content is blue");
    native_window_api_disconnect(anw2, NATIVE_WINDOW_API_CPU);
    window2.clear();

    st2.clear();
    st.clear();
    eglMakeCurrent(dpy, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(dpy, context);
    eglDestroySurface(dpy, surface);

    if (gFailures == 0) {
        printf("RESULT: PASS\n");
    } else {
        printf("RESULT: FAIL (%d)\n", gFailures);
    }
    return gFailures == 0 ? 0 : 1;
}
