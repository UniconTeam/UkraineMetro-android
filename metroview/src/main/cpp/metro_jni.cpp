// JNI bridge for VulkanMetroView. All calls are expected on the Android UI
// thread; the native Renderer is single-threaded by design.

#include <android/asset_manager_jni.h>
#include <android/native_window_jni.h>
#include <jni.h>

#include <string>
#include <vector>

#include "renderer.h"

namespace {

metro::Renderer* fromHandle(jlong h) {
    return reinterpret_cast<metro::Renderer*>(h);
}

std::vector<std::string> toStrings(JNIEnv* env, jobjectArray arr) {
    std::vector<std::string> out;
    if (arr == nullptr) {
        return out;
    }
    const jsize n = env->GetArrayLength(arr);
    out.reserve(n);
    for (jsize i = 0; i < n; ++i) {
        auto js = static_cast<jstring>(env->GetObjectArrayElement(arr, i));
        if (js == nullptr) {
            out.emplace_back();
            continue;
        }
        const char* utf = env->GetStringUTFChars(js, nullptr);
        out.emplace_back(utf != nullptr ? utf : "");
        if (utf != nullptr) {
            env->ReleaseStringUTFChars(js, utf);
        }
        env->DeleteLocalRef(js);
    }
    return out;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeInit(
    JNIEnv* env, jobject /*thiz*/, jobject assetManager, jfloatArray colors) {
    auto* renderer = new metro::Renderer();
    float clearRgba[4] = {1.0f, 1.0f, 1.0f, 1.0f};
    float bubbleRgba[4] = {0.79f, 0.79f, 0.79f, 1.0f};
    float textRgba[4] = {0.41f, 0.34f, 0.38f, 1.0f};
    if (colors != nullptr && env->GetArrayLength(colors) >= 12) {
        jfloat buf[12];
        env->GetFloatArrayRegion(colors, 0, 12, buf);
        for (int i = 0; i < 4; ++i) {
            clearRgba[i] = buf[i];
            bubbleRgba[i] = buf[4 + i];
            textRgba[i] = buf[8 + i];
        }
    }
    AAssetManager* assets = AAssetManager_fromJava(env, assetManager);
    renderer->init(assets, clearRgba, bubbleRgba, textRgba);
    return reinterpret_cast<jlong>(renderer);
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeDestroy(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    delete fromHandle(handle);  // delete nullptr is a no-op
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeSurfaceCreated(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jobject surface) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    ANativeWindow* window =
        surface != nullptr ? ANativeWindow_fromSurface(env, surface) : nullptr;
    renderer->setSurface(window);
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeSurfaceDestroyed(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    renderer->setSurface(nullptr);
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeResize(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jint w, jint h) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    renderer->resize(w, h);
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeSetData(
    JNIEnv* env, jobject /*thiz*/, jlong handle, jintArray branchColors,
    jfloatArray branchPts, jintArray branchCounts, jfloatArray transPts,
    jobjectArray labelTexts, jfloatArray labelPos) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    jsize branchCount = branchCounts != nullptr ? env->GetArrayLength(branchCounts) : 0;
    std::vector<int32_t> colors(static_cast<size_t>(branchCount));
    std::vector<int32_t> counts(static_cast<size_t>(branchCount));
    if (branchCount > 0) {
        env->GetIntArrayRegion(branchColors, 0, branchCount,
                               reinterpret_cast<jint*>(colors.data()));
        env->GetIntArrayRegion(branchCounts, 0, branchCount,
                               reinterpret_cast<jint*>(counts.data()));
    }
    jsize ptsLen = branchPts != nullptr ? env->GetArrayLength(branchPts) : 0;
    std::vector<float> pts(static_cast<size_t>(ptsLen));
    if (ptsLen > 0) {
        env->GetFloatArrayRegion(branchPts, 0, ptsLen,
                                 reinterpret_cast<jfloat*>(pts.data()));
    }
    jsize transLen = transPts != nullptr ? env->GetArrayLength(transPts) : 0;
    std::vector<float> trans(static_cast<size_t>(transLen));
    if (transLen > 0) {
        env->GetFloatArrayRegion(transPts, 0, transLen,
                                 reinterpret_cast<jfloat*>(trans.data()));
    }
    std::vector<std::string> texts = toStrings(env, labelTexts);
    jsize posLen = labelPos != nullptr ? env->GetArrayLength(labelPos) : 0;
    std::vector<float> pos(static_cast<size_t>(posLen));
    if (posLen > 0) {
        env->GetFloatArrayRegion(labelPos, 0, posLen,
                                 reinterpret_cast<jfloat*>(pos.data()));
    }
    std::vector<const char*> cstrs;
    cstrs.reserve(texts.size());
    for (const auto& t : texts) {
        cstrs.push_back(t.c_str());
    }
    renderer->setData(colors.data(), pts.data(), counts.data(),
                      static_cast<int>(branchCount), trans.data(),
                      static_cast<int>(transLen / 4), cstrs.data(), pos.data(),
                      static_cast<int>(texts.size()));
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeSetView(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle, jfloat scrollX,
    jfloat scrollY, jfloat scale, jfloat angle, jfloat pivotX, jfloat pivotY,
    jfloat textR, jfloat textG, jfloat textB, jfloat textA) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    renderer->setView(scrollX, scrollY, scale, pivotX, pivotY, angle, textR,
                      textG, textB, textA);
}

JNIEXPORT void JNICALL
Java_uniconteam_ukrainemetro_mapview_view_VulkanMetroView_nativeRender(
    JNIEnv* /*env*/, jobject /*thiz*/, jlong handle) {
    metro::Renderer* renderer = fromHandle(handle);
    if (renderer == nullptr) {
        return;
    }
    renderer->render();
}

}  // extern "C"
