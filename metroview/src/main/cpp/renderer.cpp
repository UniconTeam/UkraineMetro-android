// Vulkan metro map renderer: lines as expanded triangle geometry, station
// labels as FreeType-rasterized quads from a single RGBA atlas.

#define VK_USE_PLATFORM_ANDROID_KHR 1
#include "renderer.h"

#include <android/log.h>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstring>

#define LOG_TAG "MetroVulkan"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define VK_CHECK(x)                                                     \
    do {                                                                \
        VkResult _r = (x);                                              \
        if (_r != VK_SUCCESS) {                                          \
            LOGE("Vulkan error %d at %s:%d", _r, __FILE__, __LINE__);   \
            return false;                                               \
        }                                                               \
    } while (0)

namespace metro {

namespace {

struct PushColor {
    float scrollX, scrollY;
    float scale;
    float _pad0;  // keep vec2 alignment identical to the GLSL push block
    float viewW, viewH;
    float pivotX, pivotY;
    float angle;  // map rotation, radians, clockwise-positive on screen
};

struct PushText {
    float scrollX, scrollY;
    float scale;
    float _pad0;  // keep vec2 alignment identical to the GLSL push block
    float viewW, viewH;
    float pivotX, pivotY;
    float textR, textG, textB, textA;
    float angle;  // map rotation, radians, clockwise-positive on screen
};

static_assert(sizeof(PushColor) == 36, "PushColor must match GLSL layout");
static_assert(sizeof(PushText) == 52, "PushText must match GLSL layout");

void colorIntToFloat(int32_t c, float* r, float* g, float* b, float* a) {
    *a = ((c >> 24) & 0xFF) / 255.0f;
    *r = ((c >> 16) & 0xFF) / 255.0f;
    *g = ((c >> 8) & 0xFF) / 255.0f;
    *b = (c & 0xFF) / 255.0f;
    if (*a == 0.0f) {
        *a = 1.0f;  // 0xRRGGBB colors carry no alpha: treat as opaque
    }
}

uint32_t findMemoryType(VkPhysicalDevice dev, uint32_t mask,
                        VkMemoryPropertyFlags props) {
    VkPhysicalDeviceMemoryProperties mem{};
    vkGetPhysicalDeviceMemoryProperties(dev, &mem);
    for (uint32_t i = 0; i < mem.memoryTypeCount; ++i) {
        if ((mask & (1u << i)) != 0u &&
            (mem.memoryTypes[i].propertyFlags & props) == props) {
            return i;
        }
    }
    return UINT32_MAX;
}

}  // namespace

Renderer::Renderer() = default;

Renderer::~Renderer() { destroy(); }

bool Renderer::readAsset(const char* name, std::vector<uint8_t>* out) {
    if (assets_ == nullptr || out == nullptr) {
        return false;
    }
    AAsset* asset =
        AAssetManager_open(assets_, name, AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        LOGE("missing asset %s", name);
        return false;
    }
    const size_t len = static_cast<size_t>(AAsset_getLength(asset));
    out->resize(len);
    if (len > 0) {
        memcpy(out->data(), AAsset_getBuffer(asset), len);
    }
    AAsset_close(asset);
    return true;
}

bool Renderer::init(AAssetManager* assets, const float clearRgba[4],
                    const float textRgba[4]) {
    assets_ = assets;
    clear_.color.float32[0] = clearRgba[0];
    clear_.color.float32[1] = clearRgba[1];
    clear_.color.float32[2] = clearRgba[2];
    clear_.color.float32[3] = clearRgba[3];
    memcpy(textColor_, textRgba, sizeof(textColor_));
    if (!ft_.init()) {
        LOGE("FreeType init failed");
    } else if (!ft_.loadSystemFont(kFontPx)) {
        LOGE("no system font with Cyrillic coverage found, labels disabled");
    } else {
        LOGI("font loaded for labels");
    }
    return true;
}

bool Renderer::createInstance() {
    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.apiVersion = VK_API_VERSION_1_0;
    const char* exts[] = {VK_KHR_SURFACE_EXTENSION_NAME,
                          VK_KHR_ANDROID_SURFACE_EXTENSION_NAME};
    VkInstanceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    ci.pApplicationInfo = &app;
    ci.enabledExtensionCount = 2;
    ci.ppEnabledExtensionNames = exts;
    VK_CHECK(vkCreateInstance(&ci, nullptr, &instance_));
    return true;
}

bool Renderer::createSurface() {
    auto fn = reinterpret_cast<PFN_vkCreateAndroidSurfaceKHR>(
        vkGetInstanceProcAddr(instance_, "vkCreateAndroidSurfaceKHR"));
    if (fn == nullptr || window_ == nullptr) {
        return false;
    }
    VkAndroidSurfaceCreateInfoKHR ci{};
    ci.sType = VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    ci.window = window_;
    VK_CHECK(fn(instance_, &ci, nullptr, &surface_));
    return true;
}

Renderer::SwapSupport Renderer::querySwapSupport(VkPhysicalDevice dev) {
    SwapSupport s;
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(dev, surface_, &s.caps);
    uint32_t n = 0;
    vkGetPhysicalDeviceSurfaceFormatsKHR(dev, surface_, &n, nullptr);
    s.formats.resize(n);
    vkGetPhysicalDeviceSurfaceFormatsKHR(dev, surface_, &n, s.formats.data());
    vkGetPhysicalDeviceSurfacePresentModesKHR(dev, surface_, &n, nullptr);
    s.modes.resize(n);
    vkGetPhysicalDeviceSurfacePresentModesKHR(dev, surface_, &n, s.modes.data());
    return s;
}

bool Renderer::pickPhysicalDevice() {
    uint32_t n = 0;
    vkEnumeratePhysicalDevices(instance_, &n, nullptr);
    if (n == 0) {
        LOGE("no Vulkan physical devices");
        return false;
    }
    std::vector<VkPhysicalDevice> devs(n);
    vkEnumeratePhysicalDevices(instance_, &n, devs.data());
    const char* wantExt = VK_KHR_SWAPCHAIN_EXTENSION_NAME;
    for (VkPhysicalDevice dev : devs) {
        uint32_t qn = 0;
        vkGetPhysicalDeviceQueueFamilyProperties(dev, &qn, nullptr);
        std::vector<VkQueueFamilyProperties> qs(qn);
        vkGetPhysicalDeviceQueueFamilyProperties(dev, &qn, qs.data());
        for (uint32_t i = 0; i < qn; ++i) {
            VkBool32 present = VK_FALSE;
            vkGetPhysicalDeviceSurfaceSupportKHR(dev, i, surface_, &present);
            if ((qs[i].queueFlags & VK_QUEUE_GRAPHICS_BIT) != 0u && present != 0u) {
                uint32_t en = 0;
                vkEnumerateDeviceExtensionProperties(dev, nullptr, &en, nullptr);
                std::vector<VkExtensionProperties> es(en);
                vkEnumerateDeviceExtensionProperties(dev, nullptr, &en, es.data());
                bool hasSwap = false;
                for (const auto& e : es) {
                    if (strcmp(e.extensionName, wantExt) == 0) {
                        hasSwap = true;
                    }
                }
                if (!hasSwap) {
                    continue;
                }
                SwapSupport s = querySwapSupport(dev);
                if (s.formats.empty() || s.modes.empty()) {
                    continue;
                }
                physDev_ = dev;
                gfxFamily_ = i;
                return true;
            }
        }
    }
    LOGE("no suitable Vulkan device");
    return false;
}

bool Renderer::createDevice() {
    float prio = 1.0f;
    VkDeviceQueueCreateInfo qi{};
    qi.sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
    qi.queueFamilyIndex = gfxFamily_;
    qi.queueCount = 1;
    qi.pQueuePriorities = &prio;
    const char* exts[] = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};
    VkDeviceCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
    ci.queueCreateInfoCount = 1;
    ci.pQueueCreateInfos = &qi;
    ci.enabledExtensionCount = 1;
    ci.ppEnabledExtensionNames = exts;
    VK_CHECK(vkCreateDevice(physDev_, &ci, nullptr, &device_));
    vkGetDeviceQueue(device_, gfxFamily_, 0, &gfxQueue_);
    return true;
}

bool Renderer::chooseSwapFormat() {
    SwapSupport s = querySwapSupport(physDev_);
    if (s.formats.empty()) {
        return false;
    }
    VkSurfaceFormatKHR fmt = s.formats[0];
    for (const auto& f : s.formats) {
        if (f.format == VK_FORMAT_B8G8R8A8_SRGB &&
            f.colorSpace == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
            fmt = f;
            break;
        }
    }
    swapFormat_ = fmt.format;
    swapColorSpace_ = fmt.colorSpace;
    return swapFormat_ != VK_FORMAT_UNDEFINED;
}

// Resolves the swapchain extent + transform for the current surface.
// Prefers an identity transform: the renderer draws in display space,
// so the compositor must not rotate our images. (Using currentTransform
// blindly once latched a ROTATE_270 mid-rotation, showing the whole
// map sideways until the next swapchain rebuild.)
void Renderer::computeSwapExtent(VkExtent2D* extent,
                                 VkSurfaceTransformFlagBitsKHR* pre) {
    SwapSupport s = querySwapSupport(physDev_);
    VkSurfaceTransformFlagBitsKHR t = s.caps.currentTransform;
    if ((s.caps.supportedTransforms & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0) {
        t = VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
    }
    LOGI("swapcaps current=%dx%d transform=0x%x (using 0x%x) min=%dx%d max=%dx%d view=%dx%d",
         s.caps.currentExtent.width, s.caps.currentExtent.height,
         s.caps.currentTransform, t, s.caps.minImageExtent.width,
         s.caps.minImageExtent.height, s.caps.maxImageExtent.width,
         s.caps.maxImageExtent.height, viewW_, viewH_);
    VkExtent2D e{
        static_cast<uint32_t>(viewW_ > 0 ? viewW_ : 1),
        static_cast<uint32_t>(viewH_ > 0 ? viewH_ : 1),
    };
    if (s.caps.currentExtent.width != UINT32_MAX) {
        e = s.caps.currentExtent;
    } else {
        if (e.width < s.caps.minImageExtent.width) {
            e.width = s.caps.minImageExtent.width;
        }
        if (e.width > s.caps.maxImageExtent.width) {
            e.width = s.caps.maxImageExtent.width;
        }
        if (e.height < s.caps.minImageExtent.height) {
            e.height = s.caps.minImageExtent.height;
        }
        if (e.height > s.caps.maxImageExtent.height) {
            e.height = s.caps.maxImageExtent.height;
        }
    }
    if (t == VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR ||
        t == VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR) {
        // Extent is expressed in pre-transform space: swap so the buffer
        // matches the display orientation.
        const uint32_t w = e.width;
        e.width = e.height;
        e.height = w;
    }
    *extent = e;
    *pre = t;
}

// Full device-level teardown + rebuild (keeps instance/surface/window).
// Used to recover from VK_ERROR_DEVICE_LOST: after a loss every device
// object is undefined, so partial rebuilds cannot work.
bool Renderer::recreateDevice() {
    if (device_ == VK_NULL_HANDLE) {
        return false;
    }
    LOGI("recreateDevice: tearing down after device loss");
    vkDeviceWaitIdle(device_);  // best effort; fails harmlessly if lost
    if (pipeColor_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeColor_, nullptr);
        pipeColor_ = VK_NULL_HANDLE;
    }
    if (pipeText_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeText_, nullptr);
        pipeText_ = VK_NULL_HANDLE;
    }
    if (pipeOverlay_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeOverlay_, nullptr);
        pipeOverlay_ = VK_NULL_HANDLE;
    }
    if (pipeLayoutColor_ != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(device_, pipeLayoutColor_, nullptr);
        pipeLayoutColor_ = VK_NULL_HANDLE;
    }
    if (pipeLayoutText_ != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(device_, pipeLayoutText_, nullptr);
        pipeLayoutText_ = VK_NULL_HANDLE;
    }
    if (descPool_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorPool(device_, descPool_, nullptr);
        descPool_ = VK_NULL_HANDLE;
    }
    overlayDescSet_ = VK_NULL_HANDLE;
    descSet_ = VK_NULL_HANDLE;
    if (dsLayout_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(device_, dsLayout_, nullptr);
        dsLayout_ = VK_NULL_HANDLE;
    }
    if (sampler_ != VK_NULL_HANDLE) {
        vkDestroySampler(device_, sampler_, nullptr);
        sampler_ = VK_NULL_HANDLE;
    }
    if (atlasView_ != VK_NULL_HANDLE) {
        vkDestroyImageView(device_, atlasView_, nullptr);
        atlasView_ = VK_NULL_HANDLE;
    }
    if (atlasImage_ != VK_NULL_HANDLE) {
        vkDestroyImage(device_, atlasImage_, nullptr);
        vkFreeMemory(device_, atlasMem_, nullptr);
        atlasImage_ = VK_NULL_HANDLE;
    }
    if (overlayView_ != VK_NULL_HANDLE) {
        vkDestroyImageView(device_, overlayView_, nullptr);
        overlayView_ = VK_NULL_HANDLE;
    }
    if (overlayImage_ != VK_NULL_HANDLE) {
        vkDestroyImage(device_, overlayImage_, nullptr);
        vkFreeMemory(device_, overlayMem_, nullptr);
        overlayImage_ = VK_NULL_HANDLE;
    }
    auto dropBuf = [&](VkBuffer& b, VkDeviceMemory& m, VkDeviceSize& c) {
        if (b != VK_NULL_HANDLE) {
            vkDestroyBuffer(device_, b, nullptr);
            vkFreeMemory(device_, m, nullptr);
            b = VK_NULL_HANDLE;
        }
        c = 0;
    };
    dropBuf(stagingBuf_, stagingMem_, stagingCap_);
    dropBuf(overlayStaging_, overlayStagingMem_, overlayStagingCap_);
    dropBuf(lineBuf_, lineMem_, lineCap_);
    dropBuf(leaderBuf_, leaderMem_, leaderCap_);
    dropBuf(textBuf_, textMem_, textCap_);
    dropBuf(haloBuf_, haloMem_, haloCap_);
    dropBuf(overlayBuf_, overlayBufMem_, overlayBufCap_);
    if (imgAvail_ != VK_NULL_HANDLE) {
        vkDestroySemaphore(device_, imgAvail_, nullptr);
        imgAvail_ = VK_NULL_HANDLE;
    }
    if (renderDone_ != VK_NULL_HANDLE) {
        vkDestroySemaphore(device_, renderDone_, nullptr);
        renderDone_ = VK_NULL_HANDLE;
    }
    if (renderFence_ != VK_NULL_HANDLE) {
        vkDestroyFence(device_, renderFence_, nullptr);
        renderFence_ = VK_NULL_HANDLE;
    }
    if (cmdPool_ != VK_NULL_HANDLE) {
        vkDestroyCommandPool(device_, cmdPool_, nullptr);
        cmdPool_ = VK_NULL_HANDLE;
    }
    if (renderPass_ != VK_NULL_HANDLE) {
        vkDestroyRenderPass(device_, renderPass_, nullptr);
        renderPass_ = VK_NULL_HANDLE;
    }
    destroySwapchain();
    vkDestroyDevice(device_, nullptr);
    device_ = VK_NULL_HANDLE;
    atlasLayout_ = VK_IMAGE_LAYOUT_UNDEFINED;
    overlayLayout_ = VK_IMAGE_LAYOUT_UNDEFINED;
    ready_ = false;
    if (!pickPhysicalDevice() || !createDevice() || !chooseSwapFormat() ||
        !createRenderPass() || !createSwapchain() ||
        !createDescriptorResources() || !createPipelines() ||
        !createOverlayPipeline() || !createSyncObjects()) {
        LOGE("recreateDevice failed");
        return false;
    }
    atlasDirty_ = !atlasRgba_.empty();
    textLayoutDirty_ = true;
    labelsDirty_ = false;
    overlayTexDirty_ = true;
    uploadOverlay();
    dataDirty_ = true;
    ready_ = true;
    // No render() call here: the caller returns true so Kotlin schedules
    // the next frame, which draws on the fresh device (avoids recursion
    // if the device is still lost).
    return true;
}

bool Renderer::createSwapchain() {    if (swapFormat_ == VK_FORMAT_UNDEFINED && !chooseSwapFormat()) {
        LOGE("no swapchain format");
        return false;
    }
    VkExtent2D extent;
    VkSurfaceTransformFlagBitsKHR pre;
    computeSwapExtent(&extent, &pre);
    swapExtent_ = extent;
    swapTransform_ = pre;
    // Image count limits come from a fresh query (cheap, runs rarely).
    SwapSupport s = querySwapSupport(physDev_);
    uint32_t count = s.caps.minImageCount + 1;
    if (s.caps.maxImageCount > 0 && count > s.caps.maxImageCount) {
        count = s.caps.maxImageCount;
    }
    VkSwapchainCreateInfoKHR ci{};
    ci.sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
    ci.surface = surface_;
    ci.minImageCount = count;
    ci.imageFormat = swapFormat_;
    ci.imageColorSpace = swapColorSpace_;
    ci.imageExtent = extent;
    ci.imageArrayLayers = 1;
    ci.imageUsage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
    ci.imageSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    ci.preTransform = pre;
    ci.compositeAlpha = VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
    ci.presentMode = VK_PRESENT_MODE_FIFO_KHR;
    ci.clipped = VK_TRUE;
    VK_CHECK(vkCreateSwapchainKHR(device_, &ci, nullptr, &swapchain_));
    uint32_t n = 0;
    vkGetSwapchainImagesKHR(device_, swapchain_, &n, nullptr);
    swapImages_.resize(n);
    vkGetSwapchainImagesKHR(device_, swapchain_, &n, swapImages_.data());
    swapViews_.resize(n);
    for (uint32_t i = 0; i < n; ++i) {
        VkImageViewCreateInfo vi{};
        vi.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
        vi.image = swapImages_[i];
        vi.viewType = VK_IMAGE_VIEW_TYPE_2D;
        vi.format = swapFormat_;
        vi.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        vi.subresourceRange.levelCount = 1;
        vi.subresourceRange.layerCount = 1;
        VK_CHECK(vkCreateImageView(device_, &vi, nullptr, &swapViews_[i]));
    }
    framebuffers_.resize(n);
    for (uint32_t i = 0; i < n; ++i) {
        VkFramebufferCreateInfo fi{};
        fi.sType = VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
        fi.renderPass = renderPass_;
        fi.attachmentCount = 1;
        fi.pAttachments = &swapViews_[i];
        fi.width = extent.width;
        fi.height = extent.height;
        fi.layers = 1;
        VK_CHECK(vkCreateFramebuffer(device_, &fi, nullptr, &framebuffers_[i]));
    }
    return true;
}

void Renderer::destroySwapchain() {
    if (device_ == VK_NULL_HANDLE) {
        return;
    }
    // A present using these images may still be in flight (presents are
    // async and untracked by our fence). Draining first avoids faulting
    // the driver by tearing down in-use swapchain resources, which
    // surfaces as VK_ERROR_DEVICE_LOST on a later submit.
    vkDeviceWaitIdle(device_);
    for (VkFramebuffer fb : framebuffers_) {
        vkDestroyFramebuffer(device_, fb, nullptr);
    }
    framebuffers_.clear();
    for (VkImageView v : swapViews_) {
        vkDestroyImageView(device_, v, nullptr);
    }
    swapViews_.clear();
    swapImages_.clear();
    if (swapchain_ != VK_NULL_HANDLE) {
        vkDestroySwapchainKHR(device_, swapchain_, nullptr);
        swapchain_ = VK_NULL_HANDLE;
    }
}

bool Renderer::createRenderPass() {
    VkAttachmentDescription att{};
    att.format = swapFormat_;
    att.samples = VK_SAMPLE_COUNT_1_BIT;
    att.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
    att.storeOp = VK_ATTACHMENT_STORE_OP_STORE;
    att.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    att.finalLayout = VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
    VkAttachmentReference ref{};
    ref.attachment = 0;
    ref.layout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
    VkSubpassDescription sub{};
    sub.pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS;
    sub.colorAttachmentCount = 1;
    sub.pColorAttachments = &ref;
    VkSubpassDependency dep{};
    dep.srcSubpass = VK_SUBPASS_EXTERNAL;
    dep.dstSubpass = 0;
    dep.srcStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dep.dstStageMask = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    dep.dstAccessMask = VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
    VkRenderPassCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
    ci.attachmentCount = 1;
    ci.pAttachments = &att;
    ci.subpassCount = 1;
    ci.pSubpasses = &sub;
    ci.dependencyCount = 1;
    ci.pDependencies = &dep;
    VK_CHECK(vkCreateRenderPass(device_, &ci, nullptr, &renderPass_));
    return true;
}

bool Renderer::createDescriptorResources() {
    VkDescriptorSetLayoutBinding b{};
    b.binding = 0;
    b.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    b.descriptorCount = 1;
    b.stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
    VkDescriptorSetLayoutCreateInfo li{};
    li.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
    li.bindingCount = 1;
    li.pBindings = &b;
    VK_CHECK(vkCreateDescriptorSetLayout(device_, &li, nullptr, &dsLayout_));

    VkSamplerCreateInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
    si.magFilter = VK_FILTER_LINEAR;
    si.minFilter = VK_FILTER_LINEAR;
    si.addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    si.addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    si.addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
    si.maxAnisotropy = 1.0f;
    si.maxLod = 1.0f;
    VK_CHECK(vkCreateSampler(device_, &si, nullptr, &sampler_));

    if (!createImage(kAtlasSize, kAtlasSize, VK_FORMAT_R8G8B8A8_UNORM,
                     VK_IMAGE_TILING_OPTIMAL,
                     VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                     VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &atlasImage_, &atlasMem_)) {
        return false;
    }
    VkImageViewCreateInfo vi{};
    vi.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    vi.image = atlasImage_;
    vi.viewType = VK_IMAGE_VIEW_TYPE_2D;
    vi.format = VK_FORMAT_R8G8B8A8_UNORM;
    vi.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    vi.subresourceRange.levelCount = 1;
    vi.subresourceRange.layerCount = 1;
    VK_CHECK(vkCreateImageView(device_, &vi, nullptr, &atlasView_));

    VkDescriptorPoolSize poolSize{};
    poolSize.type = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    poolSize.descriptorCount = 2;
    VkDescriptorPoolCreateInfo pi{};
    pi.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
    pi.maxSets = 2;
    pi.poolSizeCount = 1;
    pi.pPoolSizes = &poolSize;
    VK_CHECK(vkCreateDescriptorPool(device_, &pi, nullptr, &descPool_));
    VkDescriptorSetAllocateInfo ai{};
    ai.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    ai.descriptorPool = descPool_;
    ai.descriptorSetCount = 1;
    ai.pSetLayouts = &dsLayout_;
    VK_CHECK(vkAllocateDescriptorSets(device_, &ai, &descSet_));
    VkDescriptorImageInfo img{};
    img.sampler = sampler_;
    img.imageView = atlasView_;
    img.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    VkWriteDescriptorSet w{};
    w.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    w.dstSet = descSet_;
    w.descriptorCount = 1;
    w.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    w.pImageInfo = &img;
    vkUpdateDescriptorSets(device_, 1, &w, 0, nullptr);
    if (!createOverlayResources()) {
        return false;
    }
    return true;
}

bool Renderer::createOverlayResources() {
    if (!createImage(kOverlayW, kOverlayH, VK_FORMAT_R8G8B8A8_UNORM,
                     VK_IMAGE_TILING_OPTIMAL,
                     VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                     VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, &overlayImage_,
                     &overlayMem_)) {
        return false;
    }
    VkImageViewCreateInfo vi{};
    vi.sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
    vi.image = overlayImage_;
    vi.viewType = VK_IMAGE_VIEW_TYPE_2D;
    vi.format = VK_FORMAT_R8G8B8A8_UNORM;
    vi.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
    vi.subresourceRange.levelCount = 1;
    vi.subresourceRange.layerCount = 1;
    VK_CHECK(vkCreateImageView(device_, &vi, nullptr, &overlayView_));
    VkDescriptorSetAllocateInfo ai{};
    ai.sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
    ai.descriptorPool = descPool_;
    ai.descriptorSetCount = 1;
    ai.pSetLayouts = &dsLayout_;
    VK_CHECK(vkAllocateDescriptorSets(device_, &ai, &overlayDescSet_));
    VkDescriptorImageInfo img{};
    img.sampler = sampler_;
    img.imageView = overlayView_;
    img.imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
    VkWriteDescriptorSet w{};
    w.sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
    w.dstSet = overlayDescSet_;
    w.descriptorCount = 1;
    w.descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
    w.pImageInfo = &img;
    vkUpdateDescriptorSets(device_, 1, &w, 0, nullptr);
    return true;
}

bool Renderer::createShaderModule(const std::vector<uint8_t>& code,
                                  VkShaderModule* out) {
    VkShaderModuleCreateInfo ci{};
    ci.sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
    ci.codeSize = code.size();
    ci.pCode = reinterpret_cast<const uint32_t*>(code.data());
    VK_CHECK(vkCreateShaderModule(device_, &ci, nullptr, out));
    return true;
}

bool Renderer::createPipelines() {
    std::vector<uint8_t> colorVert, colorFrag, textVert, textFrag;
    if (!readAsset("shaders/color_vert.spv", &colorVert) ||
        !readAsset("shaders/color_frag.spv", &colorFrag) ||
        !readAsset("shaders/text_vert.spv", &textVert) ||
        !readAsset("shaders/text_frag.spv", &textFrag)) {
        return false;
    }
    VkShaderModule colorVertM, colorFragM, textVertM, textFragM;
    if (!createShaderModule(colorVert, &colorVertM) ||
        !createShaderModule(colorFrag, &colorFragM) ||
        !createShaderModule(textVert, &textVertM) ||
        !createShaderModule(textFrag, &textFragM)) {
        return false;
    }

    VkPushConstantRange colorRange{};
    colorRange.stageFlags = VK_SHADER_STAGE_VERTEX_BIT;
    colorRange.size = sizeof(PushColor);
    VkPipelineLayoutCreateInfo pli{};
    pli.sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
    pli.pushConstantRangeCount = 1;
    pli.pPushConstantRanges = &colorRange;
    VK_CHECK(vkCreatePipelineLayout(device_, &pli, nullptr, &pipeLayoutColor_));

    VkPushConstantRange textRange{};
    textRange.stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
    textRange.size = sizeof(PushText);
    pli.pPushConstantRanges = &textRange;
    pli.setLayoutCount = 1;
    pli.pSetLayouts = &dsLayout_;
    VK_CHECK(vkCreatePipelineLayout(device_, &pli, nullptr, &pipeLayoutText_));

    VkVertexInputBindingDescription colorBind{};
    colorBind.stride = sizeof(ColorVertex);
    colorBind.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
    VkVertexInputAttributeDescription colorAttr[2]{};
    colorAttr[0].location = 0;
    colorAttr[0].format = VK_FORMAT_R32G32_SFLOAT;
    colorAttr[0].offset = 0;
    colorAttr[1].location = 1;
    colorAttr[1].format = VK_FORMAT_R32G32B32A32_SFLOAT;
    colorAttr[1].offset = 8;

    VkVertexInputBindingDescription textBind{};
    textBind.stride = sizeof(TextVertex);
    textBind.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
    VkVertexInputAttributeDescription textAttr[2]{};
    textAttr[0].location = 0;
    textAttr[0].format = VK_FORMAT_R32G32_SFLOAT;
    textAttr[0].offset = 0;
    textAttr[1].location = 1;
    textAttr[1].format = VK_FORMAT_R32G32_SFLOAT;
    textAttr[1].offset = 8;

    VkPipelineVertexInputStateCreateInfo vinColor{};
    vinColor.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    vinColor.vertexBindingDescriptionCount = 1;
    vinColor.pVertexBindingDescriptions = &colorBind;
    vinColor.vertexAttributeDescriptionCount = 2;
    vinColor.pVertexAttributeDescriptions = colorAttr;

    VkPipelineVertexInputStateCreateInfo vinText = vinColor;
    vinText.pVertexBindingDescriptions = &textBind;
    vinText.pVertexAttributeDescriptions = textAttr;

    VkPipelineInputAssemblyStateCreateInfo inputAsm{};
    inputAsm.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAsm.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;

    VkViewport vp{};
    vp.width = 1.0f;
    vp.height = 1.0f;
    vp.maxDepth = 1.0f;
    VkRect2D sc{};
    sc.extent = {1, 1};
    VkPipelineViewportStateCreateInfo vpState{};
    vpState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    vpState.viewportCount = 1;
    vpState.pViewports = &vp;
    vpState.scissorCount = 1;
    vpState.pScissors = &sc;

    VkPipelineRasterizationStateCreateInfo rast{};
    rast.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rast.polygonMode = VK_POLYGON_MODE_FILL;
    rast.cullMode = VK_CULL_MODE_NONE;
    rast.lineWidth = 1.0f;

    VkPipelineMultisampleStateCreateInfo ms{};
    ms.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    ms.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineColorBlendAttachmentState blend{};
    blend.blendEnable = VK_TRUE;
    blend.srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
    blend.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    blend.colorBlendOp = VK_BLEND_OP_ADD;
    blend.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
    blend.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    blend.alphaBlendOp = VK_BLEND_OP_ADD;
    blend.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                           VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    VkPipelineColorBlendStateCreateInfo blendState{};
    blendState.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    blendState.attachmentCount = 1;
    blendState.pAttachments = &blend;

    VkDynamicState dynStates[] = {VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR};
    VkPipelineDynamicStateCreateInfo dyn{};
    dyn.sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
    dyn.dynamicStateCount = 2;
    dyn.pDynamicStates = dynStates;

    VkPipelineShaderStageCreateInfo colorStages[2]{};
    colorStages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    colorStages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    colorStages[0].module = colorVertM;
    colorStages[0].pName = "main";
    colorStages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    colorStages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    colorStages[1].module = colorFragM;
    colorStages[1].pName = "main";

    VkGraphicsPipelineCreateInfo pi{};
    pi.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pi.stageCount = 2;
    pi.pStages = colorStages;
    pi.pVertexInputState = &vinColor;
    pi.pInputAssemblyState = &inputAsm;
    pi.pViewportState = &vpState;
    pi.pRasterizationState = &rast;
    pi.pMultisampleState = &ms;
    pi.pColorBlendState = &blendState;
    pi.pDynamicState = &dyn;
    pi.layout = pipeLayoutColor_;
    pi.renderPass = renderPass_;
    VK_CHECK(vkCreateGraphicsPipelines(device_, VK_NULL_HANDLE, 1, &pi, nullptr,
                                       &pipeColor_));

    VkPipelineShaderStageCreateInfo textStages[2]{};
    textStages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    textStages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    textStages[0].module = textVertM;
    textStages[0].pName = "main";
    textStages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    textStages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    textStages[1].module = textFragM;
    textStages[1].pName = "main";
    pi.pStages = textStages;
    pi.pVertexInputState = &vinText;
    pi.layout = pipeLayoutText_;
    VK_CHECK(vkCreateGraphicsPipelines(device_, VK_NULL_HANDLE, 1, &pi, nullptr,
                                       &pipeText_));

    vkDestroyShaderModule(device_, colorVertM, nullptr);
    vkDestroyShaderModule(device_, colorFragM, nullptr);
    vkDestroyShaderModule(device_, textVertM, nullptr);
    vkDestroyShaderModule(device_, textFragM, nullptr);
    return true;
}

bool Renderer::createOverlayPipeline() {
    std::vector<uint8_t> vert, frag;
    if (!readAsset("shaders/overlay_vert.spv", &vert) ||
        !readAsset("shaders/overlay_frag.spv", &frag)) {
        return false;
    }
    VkShaderModule vertM, fragM;
    if (!createShaderModule(vert, &vertM) ||
        !createShaderModule(frag, &fragM)) {
        return false;
    }

    VkVertexInputBindingDescription bind{};
    bind.stride = sizeof(TextVertex);
    bind.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
    VkVertexInputAttributeDescription attr[2]{};
    attr[0].location = 0;
    attr[0].format = VK_FORMAT_R32G32_SFLOAT;
    attr[0].offset = 0;
    attr[1].location = 1;
    attr[1].format = VK_FORMAT_R32G32_SFLOAT;
    attr[1].offset = 8;
    VkPipelineVertexInputStateCreateInfo vin{};
    vin.sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
    vin.vertexBindingDescriptionCount = 1;
    vin.pVertexBindingDescriptions = &bind;
    vin.vertexAttributeDescriptionCount = 2;
    vin.pVertexAttributeDescriptions = attr;

    VkPipelineInputAssemblyStateCreateInfo inputAsm{};
    inputAsm.sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
    inputAsm.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;

    VkViewport vp{};
    vp.width = 1.0f;
    vp.height = 1.0f;
    vp.maxDepth = 1.0f;
    VkRect2D sc{};
    sc.extent = {1, 1};
    VkPipelineViewportStateCreateInfo vpState{};
    vpState.sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
    vpState.viewportCount = 1;
    vpState.pViewports = &vp;
    vpState.scissorCount = 1;
    vpState.pScissors = &sc;

    VkPipelineRasterizationStateCreateInfo rast{};
    rast.sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
    rast.polygonMode = VK_POLYGON_MODE_FILL;
    rast.cullMode = VK_CULL_MODE_NONE;
    rast.lineWidth = 1.0f;

    VkPipelineMultisampleStateCreateInfo ms{};
    ms.sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
    ms.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;

    VkPipelineColorBlendAttachmentState blend{};
    blend.blendEnable = VK_TRUE;
    blend.srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
    blend.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    blend.colorBlendOp = VK_BLEND_OP_ADD;
    blend.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
    blend.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
    blend.alphaBlendOp = VK_BLEND_OP_ADD;
    blend.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT |
                           VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
    VkPipelineColorBlendStateCreateInfo blendState{};
    blendState.sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
    blendState.attachmentCount = 1;
    blendState.pAttachments = &blend;

    VkDynamicState dynStates[] = {VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR};
    VkPipelineDynamicStateCreateInfo dyn{};
    dyn.sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
    dyn.dynamicStateCount = 2;
    dyn.pDynamicStates = dynStates;

    VkPipelineShaderStageCreateInfo stages[2]{};
    stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
    stages[0].module = vertM;
    stages[0].pName = "main";
    stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
    stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
    stages[1].module = fragM;
    stages[1].pName = "main";

    VkGraphicsPipelineCreateInfo pi{};
    pi.sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
    pi.stageCount = 2;
    pi.pStages = stages;
    pi.pVertexInputState = &vin;
    pi.pInputAssemblyState = &inputAsm;
    pi.pViewportState = &vpState;
    pi.pRasterizationState = &rast;
    pi.pMultisampleState = &ms;
    pi.pColorBlendState = &blendState;
    pi.pDynamicState = &dyn;
    pi.layout = pipeLayoutText_;
    pi.renderPass = renderPass_;
    VK_CHECK(vkCreateGraphicsPipelines(device_, VK_NULL_HANDLE, 1, &pi, nullptr,
                                       &pipeOverlay_));

    vkDestroyShaderModule(device_, vertM, nullptr);
    vkDestroyShaderModule(device_, fragM, nullptr);
    return true;
}

bool Renderer::createSyncObjects() {
    VkCommandPoolCreateInfo pi{};
    pi.sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
    pi.queueFamilyIndex = gfxFamily_;
    pi.flags = VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
    VK_CHECK(vkCreateCommandPool(device_, &pi, nullptr, &cmdPool_));
    VkCommandBufferAllocateInfo ai{};
    ai.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
    ai.commandPool = cmdPool_;
    ai.level = VK_COMMAND_BUFFER_LEVEL_PRIMARY;
    ai.commandBufferCount = 1;
    VK_CHECK(vkAllocateCommandBuffers(device_, &ai, &cmdBuf_));
    VkSemaphoreCreateInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
    VK_CHECK(vkCreateSemaphore(device_, &si, nullptr, &imgAvail_));
    VK_CHECK(vkCreateSemaphore(device_, &si, nullptr, &renderDone_));
    VkFenceCreateInfo fi{};
    fi.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
    fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
    VK_CHECK(vkCreateFence(device_, &fi, nullptr, &renderFence_));
    return true;
}

bool Renderer::createBuffer(VkDeviceSize size, VkBufferUsageFlags usage,
                            VkMemoryPropertyFlags props, VkBuffer* buf,
                            VkDeviceMemory* mem) {
    VkBufferCreateInfo bi{};
    bi.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
    bi.size = size;
    bi.usage = usage;
    bi.sharingMode = VK_SHARING_MODE_EXCLUSIVE;
    if (vkCreateBuffer(device_, &bi, nullptr, buf) != VK_SUCCESS) {
        return false;
    }
    VkMemoryRequirements req{};
    vkGetBufferMemoryRequirements(device_, *buf, &req);
    const uint32_t idx = findMemoryType(physDev_, req.memoryTypeBits, props);
    if (idx == UINT32_MAX) {
        vkDestroyBuffer(device_, *buf, nullptr);
        return false;
    }
    VkMemoryAllocateInfo ai{};
    ai.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    ai.allocationSize = req.size;
    ai.memoryTypeIndex = idx;
    if (vkAllocateMemory(device_, &ai, nullptr, mem) != VK_SUCCESS) {
        vkDestroyBuffer(device_, *buf, nullptr);
        return false;
    }
    vkBindBufferMemory(device_, *buf, *mem, 0);
    return true;
}

bool Renderer::createImage(uint32_t w, uint32_t h, VkFormat fmt, VkImageTiling tiling,
                           VkImageUsageFlags usage, VkMemoryPropertyFlags props,
                           VkImage* img, VkDeviceMemory* mem) {
    VkImageCreateInfo ii{};
    ii.sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
    ii.imageType = VK_IMAGE_TYPE_2D;
    ii.format = fmt;
    ii.extent = {w, h, 1};
    ii.mipLevels = 1;
    ii.arrayLayers = 1;
    ii.samples = VK_SAMPLE_COUNT_1_BIT;
    ii.tiling = tiling;
    ii.usage = usage;
    ii.initialLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    if (vkCreateImage(device_, &ii, nullptr, img) != VK_SUCCESS) {
        return false;
    }
    VkMemoryRequirements req{};
    vkGetImageMemoryRequirements(device_, *img, &req);
    const uint32_t idx = findMemoryType(physDev_, req.memoryTypeBits, props);
    if (idx == UINT32_MAX) {
        vkDestroyImage(device_, *img, nullptr);
        return false;
    }
    VkMemoryAllocateInfo ai{};
    ai.sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
    ai.allocationSize = req.size;
    ai.memoryTypeIndex = idx;
    if (vkAllocateMemory(device_, &ai, nullptr, mem) != VK_SUCCESS) {
        vkDestroyImage(device_, *img, nullptr);
        return false;
    }
    vkBindImageMemory(device_, *img, *mem, 0);
    return true;
}

void Renderer::setSurface(ANativeWindow* window) {
    if (window_ == window) {
        return;
    }
    if (device_ != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device_);
    }
    destroySwapchain();
    if (surface_ != VK_NULL_HANDLE && instance_ != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance_, surface_, nullptr);
        surface_ = VK_NULL_HANDLE;
    }
    if (window_ != nullptr) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
    ready_ = false;
    if (window == nullptr) {
        return;
    }
    window_ = window;
    if (instance_ == VK_NULL_HANDLE && !createInstance()) {
        return;
    }
    if (!createSurface()) {
        return;
    }
    if (device_ == VK_NULL_HANDLE) {
        if (!pickPhysicalDevice() || !createDevice() || !chooseSwapFormat() ||
            !createRenderPass() || !createSwapchain() ||
            !createDescriptorResources() || !createPipelines() ||
            !createOverlayPipeline() || !createSyncObjects()) {
            return;
        }
    } else {
        // New surface on an existing device: drop the old swapchain
        // (created against the previous surface) before creating the
        // new one, instead of leaking it.
        destroySwapchain();
        if (!createSwapchain()) {
            return;
        }
    }
    dataDirty_ = true;
    labelsDirty_ = true;
    atlasDirty_ = !atlasRgba_.empty();
    ready_ = true;
    render();
}

void Renderer::resize(int w, int h) {
    if (w <= 0 || h <= 0 || device_ == VK_NULL_HANDLE) {
        LOGE("resize ignored w=%d h=%d", w, h);
        return;
    }
    LOGI("resize %dx%d", w, h);
    viewW_ = w;
    viewH_ = h;
    textLayoutDirty_ = true;
    // Coalesce resize storms (rotation produces several in a row):
    // only rebuild when the resolved extent actually changed.
    VkExtent2D extent;
    VkSurfaceTransformFlagBitsKHR pre;
    computeSwapExtent(&extent, &pre);
    if (swapchain_ != VK_NULL_HANDLE && extent.width == swapExtent_.width &&
        extent.height == swapExtent_.height && pre == swapTransform_) {
        render();
        return;
    }
    vkDeviceWaitIdle(device_);
    destroySwapchain();
    if (!createSwapchain()) {
        LOGE("resize: swapchain recreate failed");
    }
    render();
}

void Renderer::setTransferColor(uint32_t argb) {
    if (argb == transferColor_) {
        return;
    }
    transferColor_ = argb;
    if (!branchPts_.empty() || !transPts_.empty()) {
        rebuildMapGeometry();
    }
}

void Renderer::setBigLabels(bool enabled) {
    if (enabled == bigLabels_) {
        return;
    }
    bigLabels_ = enabled;
    // Quads scale up; the atlas itself is reused as-is.
    textLayoutDirty_ = true;
}

void Renderer::rebuildMapGeometry() {
    lineVerts_.clear();
    const float* p = branchPts_.data();
    for (size_t i = 0; i < branchCounts_.size(); ++i) {
        float r, g, b, a;
        colorIntToFloat(branchColors_[i], &r, &g, &b, &a);
        appendSmoothPolyline(lineVerts_, p, branchCounts_[i], r, g, b, a,
                             kLineWidthMap, kSmoothSubdiv);
        p += static_cast<size_t>(branchCounts_[i]) * 2;
    }
    float tr, tg, tb, ta;
    colorIntToFloat(static_cast<int32_t>(transferColor_), &tr, &tg, &tb, &ta);
    const size_t segs = transPts_.size() / 4;
    for (size_t i = 0; i < segs; ++i) {
        appendSegment(lineVerts_, transPts_[4 * i], transPts_[4 * i + 1],
                      transPts_[4 * i + 2], transPts_[4 * i + 3], tr, tg, tb,
                      ta, kLineWidthMap);
    }
    // Station markers (white ring + branch-color center), drawn on top
    // of the lines as part of the same vertex buffer.
    const size_t labels = labelColorsCache_.size();
    for (size_t i = 0; i < labels; ++i) {
        const float x = labelPosCache_[2 * i];
        const float y = labelPosCache_[2 * i + 1];
        appendDisc(lineVerts_, x, y, kMarkerOuterR, 1.0f, 1.0f, 1.0f, 1.0f);
        float r, g, b, a;
        colorIntToFloat(labelColorsCache_[i], &r, &g, &b, &a);
        appendDisc(lineVerts_, x, y, kMarkerInnerR, r, g, b, 1.0f);
    }
    dataDirty_ = true;
}

void Renderer::setData(const int32_t* branchColors, const float* branchPts,
                       const int32_t* branchCounts, int branchCount,
                       const float* transPts, int transSegCount,
                       const char* const* labelTexts, const float* labelPos,
                       const int32_t* labelColors, int labelCount) {
    // Cache geometry inputs so the map can be rebuilt (transfer color
    // change) without re-pushing everything from Kotlin.
    branchColors_.assign(branchColors, branchColors + branchCount);
    size_t ptsTotal = 0;
    for (int i = 0; i < branchCount; ++i) {
        ptsTotal += static_cast<size_t>(branchCounts[i]) * 2;
    }
    branchPts_.assign(branchPts, branchPts + ptsTotal);
    branchCounts_.assign(branchCounts, branchCounts + branchCount);
    transPts_.assign(transPts, transPts + static_cast<size_t>(transSegCount) * 4);
    labelPosCache_.assign(labelPos, labelPos + static_cast<size_t>(labelCount) * 2);
    if (labelColors != nullptr) {
        labelColorsCache_.assign(labelColors, labelColors + labelCount);
    } else {
        labelColorsCache_.assign(static_cast<size_t>(labelCount), 0);
    }
    rebuildMapGeometry();

    labelBitmaps_.clear();
    labelAnchors_.clear();
    placedLabels_.clear();
    atlasRgba_.clear();
    if (labelCount > 0 && ft_.hasFont()) {
        int nonEmpty = 0;
        labelBitmaps_.reserve(static_cast<size_t>(labelCount));
        for (int i = 0; i < labelCount; ++i) {
            LabelBitmap bmp;
            const char* text = labelTexts[i] != nullptr ? labelTexts[i] : "";
            if (text[0] == '\0') {
                continue;  // marker-only entry: dot without text
            }
            ++nonEmpty;
            if (!ft_.rasterizeLabel(text, &bmp)) {
                continue;  // one bad label must not kill the whole atlas
            }
            labelBitmaps_.push_back(std::move(bmp));
            labelAnchors_.emplace_back(labelPos[2 * i], labelPos[2 * i + 1]);
        }
        if (!labelBitmaps_.empty()) {
            std::vector<uint8_t> rgba;
            std::vector<PlacedLabel> placed;
            if (buildAtlas(labelBitmaps_, kAtlasSize, kAtlasSize, &rgba, &placed)) {
                atlasRgba_ = std::move(rgba);
                placedLabels_ = std::move(placed);
                for (size_t i = 0; i < placedLabels_.size(); ++i) {
                    placedLabels_[i].anchorX = labelAnchors_[i].first;
                    placedLabels_[i].anchorY = labelAnchors_[i].second;
                }
                atlasDirty_ = true;
            } else {
                LOGE("labels do not fit the %dx%d atlas", kAtlasSize, kAtlasSize);
            }
        }
        if (labelBitmaps_.empty() && nonEmpty > 0) {
            LOGE("no labels rasterized out of %d", labelCount);
        }
    }
    dataDirty_ = true;
    labelsDirty_ = true;
    textLayoutDirty_ = true;
    anims_.clear();
    snapLayout_ = true;
}

void Renderer::setView(float scrollX, float scrollY, float scale, float pivotX,
                       float pivotY, float angle, float textR, float textG,
                       float textB, float textA, float haloR, float haloG,
                       float haloB, float haloA) {
    scrollX_ = scrollX;
    scrollY_ = scrollY;
    scale_ = scale;
    pivotX_ = pivotX;
    pivotY_ = pivotY;
    angleRad_ = angle;
    textColor_[0] = textR;
    textColor_[1] = textG;
    textColor_[2] = textB;
    textColor_[3] = textA;
    haloColor_[0] = haloR;
    haloColor_[1] = haloG;
    haloColor_[2] = haloB;
    haloColor_[3] = haloA;
    // Label quads are laid out in screen space: any camera change moves them.
    textLayoutDirty_ = true;
}

void Renderer::setDebugOverlay(bool enabled, int topMarginPx) {
    debugOverlay_ = enabled;
    overlayTopMargin_ = topMarginPx < 0 ? 0 : topMarginPx;
    frameDts_.clear();
    haveLastFrame_ = false;
    overlayTexDirty_ = true;
    if (enabled) {
        overlayRgba_.assign(static_cast<size_t>(kOverlayW) * kOverlayH * 4, 0);
    }
}

bool Renderer::rebuildLabelQuads() {
    textVerts_.clear();
    haloVerts_.clear();
    leaderVerts_.clear();
    if (placedLabels_.empty() || scale_ <= 0.0f || viewW_ <= 0 || viewH_ <= 0) {
        return false;
    }
    if (anims_.size() != placedLabels_.size()) {
        anims_.assign(placedLabels_.size(), LabelAnim{});
        snapLayout_ = true;
    }
    const float cx = static_cast<float>(viewW_) * 0.5f;
    const float cy = static_cast<float>(viewH_) * 0.5f;
    const float cosA = std::cos(angleRad_);
    const float sinA = std::sin(angleRad_);
    // Map <-> screen helpers for this frame's camera.
    // screen = R(a) * ((map + scroll - c) * k) + c
    auto toScreen = [&](float mx, float my, float* sx, float* sy) {
        const float px = (mx + scrollX_ - cx) * scale_;
        const float py = (my + scrollY_ - cy) * scale_;
        *sx = cosA * px - sinA * py + cx;
        *sy = sinA * px + cosA * py + cy;
    };
    auto toMap = [&](float sx, float sy, float* mx, float* my) {
        const float px = (sx - cx) / scale_;
        const float py = (sy - cy) / scale_;
        *mx = cosA * px + sinA * py + cx - scrollX_;
        *my = -sinA * px + cosA * py + cy - scrollY_;
    };
    // Fixed on-screen text size (Google Maps style), independent of zoom.
    const float ds = (bigLabels_ ? kLabelTextPxBig : kLabelTextPx) /
                     static_cast<float>(kFontPx);
    const float dotR = kMarkerOuterR * scale_;
    struct Rect {
        float x0, y0, x1, y1;
    };
    std::vector<Rect> kept;
    kept.reserve(placedLabels_.size());
    bool animating = false;
    for (size_t i = 0; i < placedLabels_.size(); ++i) {
        const auto& pl = placedLabels_[i];
        const auto& bmp = labelBitmaps_[i];
        float ax, ay;
        toScreen(pl.anchorX, pl.anchorY, &ax, &ay);
        const float wPx = bmp.w * ds;
        const float hPx = bmp.h * ds;
        const float hw = wPx * 0.5f;
        const float hh = hPx * 0.5f;
        // 8 candidate slots around the station dot, best first: N, S, E, W,
        // then diagonals. Centers are pushed out along each direction just
        // far enough (support function) that the rect clears the dot by at
        // least kLabelGapPx at any zoom.
        const float inv = 0.70710678f;
        const float dirs[8][2] = {{0.0f, -1.0f},
                                  {0.0f, 1.0f},
                                  {1.0f, 0.0f},
                                  {-1.0f, 0.0f},
                                  {inv, -inv},
                                  {-inv, -inv},
                                  {inv, inv},
                                  {-inv, inv}};
        int chosen = -1;
        float x0 = 0.0f, y0 = 0.0f, x1 = 0.0f, y1 = 0.0f;
        for (int s = 0; s < 8; ++s) {
            const float dx = dirs[s][0];
            const float dy = dirs[s][1];
            const float ext = std::fabs(dx) * hw + std::fabs(dy) * hh;
            const float dist = dotR + kLabelGapPx + ext;
            const float cxr = ax + dx * dist;
            const float cyr = ay + dy * dist;
            const float rx0 = cxr - hw;
            const float ry0 = cyr - hh;
            const float rx1 = cxr + hw;
            const float ry1 = cyr + hh;
            bool overlaps = false;
            for (const auto& r : kept) {
                if (rx0 < r.x1 && rx1 > r.x0 && ry0 < r.y1 && ry1 > r.y0) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) {
                chosen = s;
                x0 = rx0;
                y0 = ry0;
                x1 = rx1;
                y1 = ry1;
                break;
            }
        }
        if (chosen < 0) {
            continue;  // no free slot: skip the label entirely.
        }
        kept.push_back({x0, y0, x1, y1});
        // Ease the per-label offset toward the chosen slot (screen px).
        // The anchor tracks the camera exactly; only the offset glides,
        // so labels don't lag behind fingers while panning.
        const float tox = (x0 + x1) * 0.5f - ax;
        const float toy = (y0 + y1) * 0.5f - ay;
        LabelAnim& an = anims_[i];
        float eox, eoy;
        if (snapLayout_ || !an.live) {
            eox = tox;
            eoy = toy;
            an.ox = tox;
            an.oy = toy;
            an.live = true;
        } else {
            eox = an.ox + (tox - an.ox) * kEaseRate;
            eoy = an.oy + (toy - an.oy) * kEaseRate;
            if (std::fabs(tox - eox) < kEaseSnapPx &&
                std::fabs(toy - eoy) < kEaseSnapPx) {
                eox = tox;
                eoy = toy;
            }
            an.ox = eox;
            an.oy = eoy;
            if (eox != tox || eoy != toy) {
                animating = true;
            }
        }
        // Eased rect actually drawn this frame (declutter used targets).
        const float ecx = ax + eox;
        const float ecy = ay + eoy;
        const float ex0 = ecx - hw;
        const float ey0 = ecy - hh;
        const float ex1 = ecx + hw;
        const float ey1 = ecy + hh;
        // Leader arrow: thick shaft from the dot edge to the nearest
        // point of the *eased* label rect, with an arrowhead pointing at
        // the station, so it stays attached mid-glide. Skipped only when
        // the eased rect already sits on the dot (default above-dot slot
        // at rest).
        {
            float nx = ax < ex0 ? ex0 : (ax > ex1 ? ex1 : ax);
            float ny = ay < ey0 ? ey0 : (ay > ey1 ? ey1 : ay);
            const float ddx = nx - ax;
            const float ddy = ny - ay;
            const float ddl = std::sqrt(ddx * ddx + ddy * ddy);
            if (ddl > dotR + 0.5f) {
                float ux = ddx;
                float uy = ddy;
                if (ddl < 1e-6f) {
                    ux = 0.0f;
                    uy = -1.0f;
                } else {
                    ux /= ddl;
                    uy /= ddl;
                }
                const float sx = ax + ux * (dotR + 1.0f);
                const float sy = ay + uy * (dotR + 1.0f);
                float p0x, p0y, p1x, p1y;
                toMap(sx, sy, &p0x, &p0y);
                toMap(nx, ny, &p1x, &p1y);
                const float lw = kLeaderWidthPx / scale_;
                appendSegment(leaderVerts_, p0x, p0y, p1x, p1y,
                              kLeaderColor[0], kLeaderColor[1],
                              kLeaderColor[2], kLeaderColor[3], lw);
                // Arrowhead: tip at the dot edge, base pushed toward the
                // label, wings perpendicular to the shaft.
                const float bx = sx + ux * kLeaderArrowLenPx;
                const float by = sy + uy * kLeaderArrowLenPx;
                const float wx = -uy * kLeaderArrowHalfPx;
                const float wy = ux * kLeaderArrowHalfPx;
                float tx, ty, b1x, b1y, b2x, b2y;
                toMap(sx, sy, &tx, &ty);
                toMap(bx + wx, by + wy, &b1x, &b1y);
                toMap(bx - wx, by - wy, &b2x, &b2y);
                leaderVerts_.push_back(
                    {tx, ty, kLeaderColor[0], kLeaderColor[1],
                     kLeaderColor[2], kLeaderColor[3]});
                leaderVerts_.push_back(
                    {b1x, b1y, kLeaderColor[0], kLeaderColor[1],
                     kLeaderColor[2], kLeaderColor[3]});
                leaderVerts_.push_back(
                    {b2x, b2y, kLeaderColor[0], kLeaderColor[1],
                     kLeaderColor[2], kLeaderColor[3]});
            }
        }
        // Eased corners back to map space (stays axis-aligned on screen).
        float qx[4], qy[4];
        toMap(ex0, ey0, &qx[0], &qy[0]);
        toMap(ex0, ey1, &qx[1], &qy[1]);
        toMap(ex1, ey0, &qx[2], &qy[2]);
        toMap(ex1, ey1, &qx[3], &qy[3]);
        // White halo: same glyph, slightly expanded, drawn first so the
        // dark text sits on a readable outline (Google Maps style).
        float hx[4], hy[4];
        toMap(ex0 - kHaloPx, ey0 - kHaloPx, &hx[0], &hy[0]);
        toMap(ex0 - kHaloPx, ey1 + kHaloPx, &hx[1], &hy[1]);
        toMap(ex1 + kHaloPx, ey0 - kHaloPx, &hx[2], &hy[2]);
        toMap(ex1 + kHaloPx, ey1 + kHaloPx, &hx[3], &hy[3]);
        const float u0 = pl.rect.x / static_cast<float>(kAtlasSize);
        const float v0 = pl.rect.y / static_cast<float>(kAtlasSize);
        const float u1 = (pl.rect.x + pl.rect.w) / static_cast<float>(kAtlasSize);
        const float v1 = (pl.rect.y + pl.rect.h) / static_cast<float>(kAtlasSize);
        textVerts_.push_back({qx[0], qy[0], u0, v0});
        textVerts_.push_back({qx[1], qy[1], u0, v1});
        textVerts_.push_back({qx[2], qy[2], u1, v0});
        textVerts_.push_back({qx[1], qy[1], u0, v1});
        textVerts_.push_back({qx[3], qy[3], u1, v1});
        textVerts_.push_back({qx[2], qy[2], u1, v0});
        haloVerts_.push_back({hx[0], hy[0], u0, v0});
        haloVerts_.push_back({hx[1], hy[1], u0, v1});
        haloVerts_.push_back({hx[2], hy[2], u1, v0});
        haloVerts_.push_back({hx[1], hy[1], u0, v1});
        haloVerts_.push_back({hx[3], hy[3], u1, v1});
        haloVerts_.push_back({hx[2], hy[2], u1, v0});
    }
    snapLayout_ = false;
    return animating;
}

namespace {

void fmtCount(size_t n, char* out, size_t cap) {
    if (n < 1000) {
        snprintf(out, cap, "%zu", n);
    } else if (n < 1000000) {
        snprintf(out, cap, "%.1fk", static_cast<double>(n) / 1000.0);
    } else {
        snprintf(out, cap, "%.2fM", static_cast<double>(n) / 1000000.0);
    }
}

}  // namespace

void Renderer::rebuildOverlay() {
    overlayVerts_.clear();
    if (!debugOverlay_ || scale_ <= 0.0f || viewW_ <= 0 || viewH_ <= 0) {
        return;
    }
    float mean = 0.0f;
    for (float d : frameDts_) {
        mean += d;
    }
    float fps = 0.0f;
    float ms = 0.0f;
    if (!frameDts_.empty()) {
        mean /= static_cast<float>(frameDts_.size());
        if (mean > 1e-6f) {
            fps = 1.0f / mean;
            ms = mean * 1000.0f;
        }
    }
    const size_t tris = (lineVerts_.size() + leaderVerts_.size() +
                         textVerts_.size() + haloVerts_.size()) /
                        3;
    const size_t verts = lineVerts_.size() + leaderVerts_.size() +
                         textVerts_.size() + haloVerts_.size();
    const auto now = std::chrono::steady_clock::now();
    const bool due =
        std::chrono::duration_cast<std::chrono::milliseconds>(now -
                                                              lastOverlayCompose_)
            .count() > 120;
    if (overlayTexDirty_ || due) {
        composeOverlay(fps, ms, tris, verts, placedLabels_.size());
        lastOverlayCompose_ = now;
        overlayTexDirty_ = false;
        uploadOverlay();
    }
    // Screen-space panel rect back to map space (same trick as labels).
    const float cx = static_cast<float>(viewW_) * 0.5f;
    const float cy = static_cast<float>(viewH_) * 0.5f;
    const float cosA = std::cos(angleRad_);
    const float sinA = std::sin(angleRad_);
    auto toMap = [&](float sx, float sy, float* mx, float* my) {
        const float px = (sx - cx) / scale_;
        const float py = (sy - cy) / scale_;
        *mx = cosA * px + sinA * py + cx - scrollX_;
        *my = -sinA * px + cosA * py + cy - scrollY_;
    };
    const float x0 = static_cast<float>(kOverlayMargin);
    const float y0 =
        static_cast<float>(overlayTopMargin_ + kOverlayMargin);
    const float x1 = x0 + static_cast<float>(kOverlayDisplayW);
    const float y1 = y0 + static_cast<float>(kOverlayDisplayH);
    float qx[4], qy[4];
    toMap(x0, y0, &qx[0], &qy[0]);
    toMap(x0, y1, &qx[1], &qy[1]);
    toMap(x1, y0, &qx[2], &qy[2]);
    toMap(x1, y1, &qx[3], &qy[3]);
    overlayVerts_.push_back({qx[0], qy[0], 0.0f, 0.0f});
    overlayVerts_.push_back({qx[1], qy[1], 0.0f, 1.0f});
    overlayVerts_.push_back({qx[2], qy[2], 1.0f, 0.0f});
    overlayVerts_.push_back({qx[1], qy[1], 0.0f, 1.0f});
    overlayVerts_.push_back({qx[3], qy[3], 1.0f, 1.0f});
    overlayVerts_.push_back({qx[2], qy[2], 1.0f, 0.0f});
}

void Renderer::composeOverlay(float fps, float ms, size_t tris, size_t verts,
                              size_t labelCount) {
    const int W = kOverlayW;
    const int H = kOverlayH;
    if (overlayRgba_.size() != static_cast<size_t>(W) * H * 4) {
        overlayRgba_.assign(static_cast<size_t>(W) * H * 4, 0);
    }
    auto dot = [&](int x, int y, uint8_t r, uint8_t g, uint8_t b,
                   uint8_t a) {
        if (x < 0 || y < 0 || x >= W || y >= H) {
            return;
        }
        uint8_t* p = &overlayRgba_[(static_cast<size_t>(y) * W + x) * 4];
        p[0] = r;
        p[1] = g;
        p[2] = b;
        p[3] = a;
    };
    auto bar = [&](int x0, int y0, int x1, int y1, uint8_t r, uint8_t g,
                   uint8_t b, uint8_t a) {
        if (x0 > x1) {
            const int t = x0;
            x0 = x1;
            x1 = t;
        }
        if (y0 > y1) {
            const int t = y0;
            y0 = y1;
            y1 = t;
        }
        for (int y = y0; y <= y1; ++y) {
            for (int x = x0; x <= x1; ++x) {
                dot(x, y, r, g, b, a);
            }
        }
    };
    // Panel background + hairline border.
    bar(0, 0, W - 1, H - 1, 9, 11, 17, 216);
    bar(0, 0, W - 1, 1, 255, 255, 255, 36);
    bar(0, H - 2, W - 1, H - 1, 255, 255, 255, 36);
    bar(0, 0, 1, H - 1, 255, 255, 255, 36);
    bar(W - 2, 0, W - 1, H - 1, 255, 255, 255, 36);
    // Text lines (FreeType, same font as station labels).
    auto blit = [&](const std::string& s, int ox, int oy) {
        if (!ft_.hasFont() || s.empty()) {
            return;
        }
        LabelBitmap bmp;
        if (!ft_.rasterizeLabel(s, &bmp)) {
            return;
        }
        for (int y = 0; y < bmp.h; ++y) {
            for (int x = 0; x < bmp.w; ++x) {
                const uint8_t g = bmp.px[static_cast<size_t>(y) * bmp.w + x];
                if (g == 0) {
                    continue;
                }
                const int dx = ox + x;
                const int dy = oy + y;
                if (dx < 0 || dy < 0 || dx >= W || dy >= H) {
                    continue;
                }
                uint8_t* p =
                    &overlayRgba_[(static_cast<size_t>(dy) * W + dx) * 4];
                p[0] = 255;
                p[1] = 255;
                p[2] = 255;
                if (g > p[3]) {
                    p[3] = g;
                }
            }
        }
    };
    char line1[96];
    char line2[96];
    char cTris[16];
    char cVerts[16];
    if (fps < 0.5f) {
        snprintf(line1, sizeof(line1), "-- FPS  -- ms");
    } else {
        snprintf(line1, sizeof(line1), "%.0f FPS  %.1f ms", fps, ms);
    }
    fmtCount(tris, cTris, sizeof(cTris));
    fmtCount(verts, cVerts, sizeof(cVerts));
    snprintf(line2, sizeof(line2), "%s tris | %s verts | %zu lbl", cTris,
             cVerts, labelCount);
    blit(line1, 24, 18);
    blit(line2, 24, 92);
    // Frame-time graph: bars, green <= 16.7ms, yellow <= 33.3ms, red above.
    const int gx0 = 24;
    const int gx1 = W - 24;
    const int gy0 = 182;
    const int gy1 = H - 18;
    const float y167 =
        gy1 - (16.7f / kOverlayGraphMaxMs) * (gy1 - gy0);
    for (int x = gx0; x <= gx1; ++x) {
        dot(x, static_cast<int>(y167), 255, 255, 255, 56);
    }
    const size_t n = frameDts_.size();
    for (size_t i = 0; i < n; ++i) {
        const float sampleMs = frameDts_[i] * 1000.0f;
        const int x = gx0 + static_cast<int>(i * (gx1 - gx0) /
                                             (kOverlayHistory - 1));
        float frac = sampleMs / kOverlayGraphMaxMs;
        if (frac < 0.0f) {
            frac = 0.0f;
        }
        if (frac > 1.0f) {
            frac = 1.0f;
        }
        const int y = gy1 - static_cast<int>(frac * (gy1 - gy0));
        uint8_t r = 74;
        uint8_t g = 222;
        uint8_t b = 128;
        if (sampleMs > 33.3f) {
            r = 248;
            g = 113;
            b = 113;
        } else if (sampleMs > 16.7f) {
            r = 250;
            g = 204;
            b = 21;
        }
        bar(x, y, x + 2, gy1, r, g, b, 255);
    }
}

void Renderer::uploadOverlay() {
    const VkDeviceSize need = overlayRgba_.size();
    if (need == 0 || device_ == VK_NULL_HANDLE) {
        return;
    }
    if (need > overlayStagingCap_ || overlayStaging_ == VK_NULL_HANDLE) {
        if (overlayStaging_ != VK_NULL_HANDLE) {
            vkDestroyBuffer(device_, overlayStaging_, nullptr);
            vkFreeMemory(device_, overlayStagingMem_, nullptr);
        }
        overlayStagingCap_ = need;
        if (!createBuffer(overlayStagingCap_, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                          VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                              VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                          &overlayStaging_, &overlayStagingMem_)) {
            return;
        }
    }
    void* dst = nullptr;
    vkMapMemory(device_, overlayStagingMem_, 0, need, 0, &dst);
    memcpy(dst, overlayRgba_.data(), need);
    vkUnmapMemory(device_, overlayStagingMem_);
    overlayCopyPending_ = true;
}

void Renderer::uploadVertices() {
    auto ensure = [&](VkBuffer& buf, VkDeviceMemory& mem, VkDeviceSize& cap,
                      VkDeviceSize need, VkBufferUsageFlags usage) {
        if (need == 0) {
            need = 1;
        }
        if (need <= cap && buf != VK_NULL_HANDLE) {
            return true;
        }
        if (buf != VK_NULL_HANDLE) {
            vkDestroyBuffer(device_, buf, nullptr);
            vkFreeMemory(device_, mem, nullptr);
        }
        cap = need;
        return createBuffer(cap, usage, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                                            VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                            &buf, &mem);
    };
    ensure(lineBuf_, lineMem_, lineCap_,
           lineVerts_.size() * sizeof(ColorVertex), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    ensure(leaderBuf_, leaderMem_, leaderCap_,
           leaderVerts_.size() * sizeof(ColorVertex), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    ensure(textBuf_, textMem_, textCap_, textVerts_.size() * sizeof(TextVertex),
           VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    ensure(haloBuf_, haloMem_, haloCap_, haloVerts_.size() * sizeof(TextVertex),
           VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    ensure(overlayBuf_, overlayBufMem_, overlayBufCap_,
           overlayVerts_.size() * sizeof(TextVertex),
           VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
    auto copyTo = [&](VkBuffer buf, VkDeviceMemory mem, const void* data,
                      size_t bytes) {
        if (bytes == 0 || buf == VK_NULL_HANDLE) {
            return;
        }
        void* dst = nullptr;
        vkMapMemory(device_, mem, 0, bytes, 0, &dst);
        memcpy(dst, data, bytes);
        vkUnmapMemory(device_, mem);
    };
    copyTo(lineBuf_, lineMem_, lineVerts_.data(),
           lineVerts_.size() * sizeof(ColorVertex));
    copyTo(leaderBuf_, leaderMem_, leaderVerts_.data(),
           leaderVerts_.size() * sizeof(ColorVertex));
    copyTo(textBuf_, textMem_, textVerts_.data(),
           textVerts_.size() * sizeof(TextVertex));
    copyTo(haloBuf_, haloMem_, haloVerts_.data(),
           haloVerts_.size() * sizeof(TextVertex));
    copyTo(overlayBuf_, overlayBufMem_, overlayVerts_.data(),
           overlayVerts_.size() * sizeof(TextVertex));
}

void Renderer::uploadAtlas(const std::vector<uint8_t>& rgba) {
    const VkDeviceSize need = rgba.size();
    if (need > stagingCap_ || stagingBuf_ == VK_NULL_HANDLE) {
        if (stagingBuf_ != VK_NULL_HANDLE) {
            vkDestroyBuffer(device_, stagingBuf_, nullptr);
            vkFreeMemory(device_, stagingMem_, nullptr);
        }
        stagingCap_ = need;
        if (!createBuffer(stagingCap_, VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                          VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                              VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                          &stagingBuf_, &stagingMem_)) {
            return;
        }
    }
    void* dst = nullptr;
    vkMapMemory(device_, stagingMem_, 0, need, 0, &dst);
    memcpy(dst, rgba.data(), need);
    vkUnmapMemory(device_, stagingMem_);
}

void Renderer::recordCommandBuffer(VkCommandBuffer cmd, uint32_t imageIndex) {
    // NOTE: no explicit vkResetCommandBuffer here. The buffer is recorded
    // fresh every frame and vkBeginCommandBuffer implicitly resets it; an
    // explicit reset of this one-time-submit buffer wedges this Adreno
    // driver (every later submit fails with VK_ERROR_DEVICE_LOST).
    VkCommandBufferBeginInfo bi{};
    bi.sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
    bi.flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
    vkBeginCommandBuffer(cmd, &bi);

    if (atlasDirty_ && !atlasRgba_.empty()) {
        VkImageMemoryBarrier toDst{};
        toDst.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        toDst.oldLayout = atlasLayout_;
        toDst.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        toDst.srcAccessMask = atlasLayout_ == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                                  ? VK_ACCESS_SHADER_READ_BIT
                                  : VkAccessFlags(0);
        toDst.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        toDst.image = atlasImage_;
        toDst.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        toDst.subresourceRange.levelCount = 1;
        toDst.subresourceRange.layerCount = 1;
        vkCmdPipelineBarrier(cmd,
                             atlasLayout_ == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                                 ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                                 : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                             VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0,
                             nullptr, 1, &toDst);
        VkBufferImageCopy region{};
        region.imageExtent = {kAtlasSize, kAtlasSize, 1};
        region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        region.imageSubresource.layerCount = 1;
        vkCmdCopyBufferToImage(cmd, stagingBuf_, atlasImage_,
                               VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
        VkImageMemoryBarrier toRead = toDst;
        toRead.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        toRead.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        toRead.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        toRead.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT,
                             VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &toRead);
        atlasLayout_ = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        atlasDirty_ = false;
    }

    if (overlayCopyPending_ && !overlayRgba_.empty() &&
        overlayImage_ != VK_NULL_HANDLE) {
        VkImageMemoryBarrier toDst{};
        toDst.sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
        toDst.oldLayout = overlayLayout_;
        toDst.newLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        toDst.srcAccessMask = overlayLayout_ == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                                  ? VK_ACCESS_SHADER_READ_BIT
                                  : VkAccessFlags(0);
        toDst.dstAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        toDst.image = overlayImage_;
        toDst.subresourceRange.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        toDst.subresourceRange.levelCount = 1;
        toDst.subresourceRange.layerCount = 1;
        vkCmdPipelineBarrier(cmd,
                             overlayLayout_ == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
                                 ? VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                                 : VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                             VK_PIPELINE_STAGE_TRANSFER_BIT, 0, 0, nullptr, 0,
                             nullptr, 1, &toDst);
        VkBufferImageCopy region{};
        region.imageExtent = {static_cast<uint32_t>(kOverlayW),
                              static_cast<uint32_t>(kOverlayH), 1};
        region.imageSubresource.aspectMask = VK_IMAGE_ASPECT_COLOR_BIT;
        region.imageSubresource.layerCount = 1;
        vkCmdCopyBufferToImage(cmd, overlayStaging_, overlayImage_,
                               VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
        VkImageMemoryBarrier toRead = toDst;
        toRead.oldLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
        toRead.newLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        toRead.srcAccessMask = VK_ACCESS_TRANSFER_WRITE_BIT;
        toRead.dstAccessMask = VK_ACCESS_SHADER_READ_BIT;
        vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT,
                             VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, 0, nullptr,
                             0, nullptr, 1, &toRead);
        overlayLayout_ = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        overlayCopyPending_ = false;
    }

    VkRenderPassBeginInfo rp{};
    rp.sType = VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
    rp.renderPass = renderPass_;
    rp.framebuffer = framebuffers_[imageIndex];
    rp.renderArea.extent = swapExtent_;
    rp.clearValueCount = 1;
    rp.pClearValues = &clear_;
    vkCmdBeginRenderPass(cmd, &rp, VK_SUBPASS_CONTENTS_INLINE);

    VkViewport vp{};
    vp.width = static_cast<float>(swapExtent_.width);
    vp.height = static_cast<float>(swapExtent_.height);
    vp.maxDepth = 1.0f;
    vkCmdSetViewport(cmd, 0, 1, &vp);
    VkRect2D sc{};
    sc.extent = swapExtent_;
    vkCmdSetScissor(cmd, 0, 1, &sc);

    PushColor pc{scrollX_, scrollY_, scale_, 0.0f /*_pad0*/,
                 static_cast<float>(swapExtent_.width),
                 static_cast<float>(swapExtent_.height), pivotX_, pivotY_,
                 angleRad_};
    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeColor_);
    vkCmdPushConstants(cmd, pipeLayoutColor_, VK_SHADER_STAGE_VERTEX_BIT, 0,
                       sizeof(pc), &pc);
    if (!lineVerts_.empty()) {
        VkDeviceSize off = 0;
        vkCmdBindVertexBuffers(cmd, 0, 1, &lineBuf_, &off);
        vkCmdDraw(cmd, static_cast<uint32_t>(lineVerts_.size()), 1, 0, 0);
    }
    if (!leaderVerts_.empty()) {
        VkDeviceSize off = 0;
        vkCmdBindVertexBuffers(cmd, 0, 1, &leaderBuf_, &off);
        vkCmdDraw(cmd, static_cast<uint32_t>(leaderVerts_.size()), 1, 0, 0);
    }
    if (!textVerts_.empty()) {
        PushText pt{scrollX_, scrollY_, scale_, 0.0f /*_pad0*/,
                    static_cast<float>(swapExtent_.width),
                    static_cast<float>(swapExtent_.height), pivotX_, pivotY_,
                    textColor_[0], textColor_[1], textColor_[2], textColor_[3],
                    angleRad_};
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeText_);
        vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeLayoutText_, 0, 1, &descSet_, 0, nullptr);
        // Halo pass first: same glyphs, slightly expanded, so the
        // text reads over lines of any color (Google Maps style).
        if (!haloVerts_.empty()) {
            PushText ph = pt;
            ph.textR = haloColor_[0];
            ph.textG = haloColor_[1];
            ph.textB = haloColor_[2];
            ph.textA = haloColor_[3];
            vkCmdPushConstants(cmd, pipeLayoutText_,
                               VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                               0, sizeof(ph), &ph);
            VkDeviceSize off = 0;
            vkCmdBindVertexBuffers(cmd, 0, 1, &haloBuf_, &off);
            vkCmdDraw(cmd, static_cast<uint32_t>(haloVerts_.size()), 1, 0, 0);
        }
        vkCmdPushConstants(cmd, pipeLayoutText_,
                           VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                           0, sizeof(pt), &pt);
        VkDeviceSize off = 0;
        vkCmdBindVertexBuffers(cmd, 0, 1, &textBuf_, &off);
        vkCmdDraw(cmd, static_cast<uint32_t>(textVerts_.size()), 1, 0, 0);
    }
    // Debug overlay last: raw RGBA panel from its own texture.
    if (debugOverlay_ && !overlayVerts_.empty() &&
        overlayImage_ != VK_NULL_HANDLE) {
        PushText pt{scrollX_, scrollY_, scale_, 0.0f /*_pad0*/,
                    static_cast<float>(swapExtent_.width),
                    static_cast<float>(swapExtent_.height), pivotX_, pivotY_,
                    1.0f, 1.0f, 1.0f, 1.0f, angleRad_};
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeOverlay_);
        vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeLayoutText_, 0, 1, &overlayDescSet_, 0,
                                nullptr);
        vkCmdPushConstants(cmd, pipeLayoutText_,
                           VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT,
                           0, sizeof(pt), &pt);
        VkDeviceSize off = 0;
        vkCmdBindVertexBuffers(cmd, 0, 1, &overlayBuf_, &off);
        vkCmdDraw(cmd, static_cast<uint32_t>(overlayVerts_.size()), 1, 0, 0);
    }
    vkCmdEndRenderPass(cmd);
    vkEndCommandBuffer(cmd);
}

bool Renderer::render() {
    if (!ready_ || window_ == nullptr || viewW_ <= 0 || viewH_ <= 0) {
        return false;
    }
    if (renderFence_ == VK_NULL_HANDLE) {
        VkFenceCreateInfo fi{};
        fi.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
        fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        if (vkCreateFence(device_, &fi, nullptr, &renderFence_) != VK_SUCCESS) {
            return false;
        }
    }
    if (swapchain_ == VK_NULL_HANDLE) {
        destroySwapchain();
        if (!createSwapchain()) {
            LOGE("swapchain missing, recreate failed");
            return false;
        }
    }
    const auto frameNow = std::chrono::steady_clock::now();
    if (debugOverlay_) {
        if (haveLastFrame_) {
            const float dt =
                std::chrono::duration<float>(frameNow - lastFrame_).count();
            if (dt > 0.25f) {
                frameDts_.clear();  // app was idle/backgrounded: fresh window
            } else {
                frameDts_.push_back(dt);
                if (frameDts_.size() > kOverlayHistory) {
                    frameDts_.erase(frameDts_.begin());
                }
            }
        }
        lastFrame_ = frameNow;
        haveLastFrame_ = true;
    } else {
        haveLastFrame_ = false;
    }
    vkWaitForFences(device_, 1, &renderFence_, VK_TRUE, UINT64_MAX);
    if (dataDirty_) {
        dataDirty_ = false;
    }
    bool animating = false;
    if (labelsDirty_ || textLayoutDirty_) {
        animating = rebuildLabelQuads();
        labelsDirty_ = false;
        // Keep rebuilding while the easing is still settling so the
        // Kotlin side can schedule the next frame.
        textLayoutDirty_ = animating;
    }
    if (debugOverlay_) {
        rebuildOverlay();
    }
    uploadVertices();
    if (atlasDirty_) {
        uploadAtlas(atlasRgba_);
    }
    uint32_t imageIndex = 0;
    VkResult acq = vkAcquireNextImageKHR(device_, swapchain_, UINT64_MAX, imgAvail_,
                                        VK_NULL_HANDLE, &imageIndex);
    if (acq == VK_ERROR_OUT_OF_DATE_KHR) {
        LOGI("acquire out-of-date, recreating");
        destroySwapchain();
        if (!createSwapchain()) {
            LOGE("swapchain recreate failed");
        }
        return animating || debugOverlay_;
    }
    if (acq == VK_ERROR_DEVICE_LOST) {
        LOGE("acquire: device lost, rebuilding");
        return recreateDevice();
    }
    if (acq != VK_SUCCESS && acq != VK_SUBOPTIMAL_KHR) {
        LOGE("acquire failed %d", acq);
        return animating || debugOverlay_;
    }
    vkResetFences(device_, 1, &renderFence_);
    recordCommandBuffer(cmdBuf_, imageIndex);
    VkPipelineStageFlags waitStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
    VkSubmitInfo si{};
    si.sType = VK_STRUCTURE_TYPE_SUBMIT_INFO;
    si.waitSemaphoreCount = 1;
    si.pWaitSemaphores = &imgAvail_;
    si.pWaitDstStageMask = &waitStage;
    si.commandBufferCount = 1;
    si.pCommandBuffers = &cmdBuf_;
    si.signalSemaphoreCount = 1;
    si.pSignalSemaphores = &renderDone_;
    VkResult sub = vkQueueSubmit(gfxQueue_, 1, &si, renderFence_);
    if (sub == VK_ERROR_DEVICE_LOST) {
        LOGE("submit: device lost, rebuilding");
        return recreateDevice();
    }
    if (sub != VK_SUCCESS) {
        LOGE("queue submit failed %d", sub);
        // The fence was just reset but nothing will ever signal it: rebuild
        // it in the signaled state so the next frame cannot wait forever,
        // and drop the swapchain so the next frame recreates everything.
        vkDeviceWaitIdle(device_);
        vkDestroyFence(device_, renderFence_, nullptr);
        renderFence_ = VK_NULL_HANDLE;
        VkFenceCreateInfo fi{};
        fi.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
        fi.flags = VK_FENCE_CREATE_SIGNALED_BIT;
        if (vkCreateFence(device_, &fi, nullptr, &renderFence_) != VK_SUCCESS) {
            LOGE("fence recreate failed");
            renderFence_ = VK_NULL_HANDLE;
            return false;
        }
        destroySwapchain();
        return false;
    }
    VkPresentInfoKHR pi{};
    pi.sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
    pi.waitSemaphoreCount = 1;
    pi.pWaitSemaphores = &renderDone_;
    pi.swapchainCount = 1;
    pi.pSwapchains = &swapchain_;
    pi.pImageIndices = &imageIndex;
    VkResult pres = vkQueuePresentKHR(gfxQueue_, &pi);
    if (pres == VK_ERROR_DEVICE_LOST) {
        LOGE("present: device lost, rebuilding");
        return recreateDevice();
    }
    if (pres != VK_SUCCESS && pres != VK_SUBOPTIMAL_KHR) {
        LOGE("present failed %d", pres);
    }
    if (pres == VK_ERROR_OUT_OF_DATE_KHR || pres == VK_SUBOPTIMAL_KHR) {
        destroySwapchain();
        createSwapchain();
    }
    return animating || debugOverlay_;
}

void Renderer::destroy() {
    if (device_ != VK_NULL_HANDLE) {
        vkDeviceWaitIdle(device_);
    }
    destroySwapchain();
    if (lineBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, lineBuf_, nullptr);
        vkFreeMemory(device_, lineMem_, nullptr);
        lineBuf_ = VK_NULL_HANDLE;
    }
    if (leaderBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, leaderBuf_, nullptr);
        vkFreeMemory(device_, leaderMem_, nullptr);
        leaderBuf_ = VK_NULL_HANDLE;
    }
    if (textBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, textBuf_, nullptr);
        vkFreeMemory(device_, textMem_, nullptr);
        textBuf_ = VK_NULL_HANDLE;
    }
    if (haloBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, haloBuf_, nullptr);
        vkFreeMemory(device_, haloMem_, nullptr);
        haloBuf_ = VK_NULL_HANDLE;
    }
    if (stagingBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, stagingBuf_, nullptr);
        vkFreeMemory(device_, stagingMem_, nullptr);
        stagingBuf_ = VK_NULL_HANDLE;
    }
    if (overlayStaging_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, overlayStaging_, nullptr);
        vkFreeMemory(device_, overlayStagingMem_, nullptr);
        overlayStaging_ = VK_NULL_HANDLE;
    }
    if (overlayBuf_ != VK_NULL_HANDLE) {
        vkDestroyBuffer(device_, overlayBuf_, nullptr);
        vkFreeMemory(device_, overlayBufMem_, nullptr);
        overlayBuf_ = VK_NULL_HANDLE;
    }
    if (overlayView_ != VK_NULL_HANDLE) {
        vkDestroyImageView(device_, overlayView_, nullptr);
        overlayView_ = VK_NULL_HANDLE;
    }
    if (overlayImage_ != VK_NULL_HANDLE) {
        vkDestroyImage(device_, overlayImage_, nullptr);
        vkFreeMemory(device_, overlayMem_, nullptr);
        overlayImage_ = VK_NULL_HANDLE;
    }
    overlayDescSet_ = VK_NULL_HANDLE;
    if (atlasView_ != VK_NULL_HANDLE) {
        vkDestroyImageView(device_, atlasView_, nullptr);
        atlasView_ = VK_NULL_HANDLE;
    }
    if (atlasImage_ != VK_NULL_HANDLE) {
        vkDestroyImage(device_, atlasImage_, nullptr);
        vkFreeMemory(device_, atlasMem_, nullptr);
        atlasImage_ = VK_NULL_HANDLE;
    }
    if (sampler_ != VK_NULL_HANDLE) {
        vkDestroySampler(device_, sampler_, nullptr);
        sampler_ = VK_NULL_HANDLE;
    }
    if (descPool_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorPool(device_, descPool_, nullptr);
        descPool_ = VK_NULL_HANDLE;
    }
    if (dsLayout_ != VK_NULL_HANDLE) {
        vkDestroyDescriptorSetLayout(device_, dsLayout_, nullptr);
        dsLayout_ = VK_NULL_HANDLE;
    }
    if (pipeColor_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeColor_, nullptr);
        pipeColor_ = VK_NULL_HANDLE;
    }
    if (pipeText_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeText_, nullptr);
        pipeText_ = VK_NULL_HANDLE;
    }
    if (pipeOverlay_ != VK_NULL_HANDLE) {
        vkDestroyPipeline(device_, pipeOverlay_, nullptr);
        pipeOverlay_ = VK_NULL_HANDLE;
    }
    if (pipeLayoutColor_ != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(device_, pipeLayoutColor_, nullptr);
        pipeLayoutColor_ = VK_NULL_HANDLE;
    }
    if (pipeLayoutText_ != VK_NULL_HANDLE) {
        vkDestroyPipelineLayout(device_, pipeLayoutText_, nullptr);
        pipeLayoutText_ = VK_NULL_HANDLE;
    }
    if (renderPass_ != VK_NULL_HANDLE) {
        vkDestroyRenderPass(device_, renderPass_, nullptr);
        renderPass_ = VK_NULL_HANDLE;
    }
    if (imgAvail_ != VK_NULL_HANDLE) {
        vkDestroySemaphore(device_, imgAvail_, nullptr);
        imgAvail_ = VK_NULL_HANDLE;
    }
    if (renderDone_ != VK_NULL_HANDLE) {
        vkDestroySemaphore(device_, renderDone_, nullptr);
        renderDone_ = VK_NULL_HANDLE;
    }
    if (renderFence_ != VK_NULL_HANDLE) {
        vkDestroyFence(device_, renderFence_, nullptr);
        renderFence_ = VK_NULL_HANDLE;
    }
    if (cmdPool_ != VK_NULL_HANDLE) {
        vkDestroyCommandPool(device_, cmdPool_, nullptr);
        cmdPool_ = VK_NULL_HANDLE;
    }
    if (device_ != VK_NULL_HANDLE) {
        vkDestroyDevice(device_, nullptr);
        device_ = VK_NULL_HANDLE;
    }
    if (surface_ != VK_NULL_HANDLE && instance_ != VK_NULL_HANDLE) {
        vkDestroySurfaceKHR(instance_, surface_, nullptr);
        surface_ = VK_NULL_HANDLE;
    }
    if (instance_ != VK_NULL_HANDLE) {
        vkDestroyInstance(instance_, nullptr);
        instance_ = VK_NULL_HANDLE;
    }
    if (window_ != nullptr) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
    atlasLayout_ = VK_IMAGE_LAYOUT_UNDEFINED;
    ready_ = false;
}

}  // namespace metro
