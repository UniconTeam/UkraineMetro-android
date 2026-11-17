#pragma once

#include <android/asset_manager.h>
#include <android/native_window.h>
#include <vulkan/vulkan.h>

#include <cstdint>
#include <string>
#include <vector>

#include "geometry.h"
#include "text_atlas.h"

namespace metro {

// Vulkan renderer for the metro map. Single-threaded by design: all public
// methods must be called from the same thread (the Android UI thread).
// Rendering is strictly on demand (updateData / resize / pan / zoom).
class Renderer {
public:
    Renderer();
    ~Renderer();
    Renderer(const Renderer&) = delete;
    Renderer& operator=(const Renderer&) = delete;

    // Loads SPIR-V shaders from assets, inits FreeType + system font.
    // Must be called once before any surface work.
    bool init(AAssetManager* assets, const float clearRgba[4], const float bubbleRgba[4],
              const float textRgba[4]);

    // (Re)attaches the output window. Passing nullptr detaches (releases it).
    void setSurface(ANativeWindow* window);
    void resize(int w, int h);

    // Full map data. Copies everything to CPU-side storage; GPU upload
    // happens lazily on the next render().
    // branchColors: 0xAARRGGBB per branch; branchPts: concatenated x,y pairs;
    // branchCounts: point count per branch. transPts: x0,y0,x1,y1 per transfer.
    // labelTexts: UTF-8 station names; labelPos: x,y map anchor per label.
    void setData(const int32_t* branchColors, const float* branchPts,
                 const int32_t* branchCounts, int branchCount, const float* transPts,
                 int transSegCount, const char* const* labelTexts, const float* labelPos,
                 int labelCount);

    void setView(float scrollX, float scrollY, float scale, float pivotX, float pivotY,
                 float textR, float textG, float textB, float textA);
    void render();
    void destroy();

private:
    static constexpr int kAtlasSize = 1024;
    static constexpr int kFontPx = 56;
    // Original Canvas metrics, in map units: text height, bubble padding,
    // line width. Label quads are sized in map units so they zoom with the map
    // exactly like the old View did.
    static constexpr float kLabelTextMapH = 5.0f;  // was textSize = 5f
    static constexpr float kBubblePadMap = 3.0f;   // was Const.BUBLE_SCALE
    static constexpr float kLineWidthMap = 4.0f;   // was Const.LINE_WIDTH

    struct SwapSupport {
        VkSurfaceCapabilitiesKHR caps{};
        std::vector<VkSurfaceFormatKHR> formats;
        std::vector<VkPresentModeKHR> modes;
    };

    bool createInstance();
    bool createSurface();
    bool pickPhysicalDevice();
    bool createDevice();
    bool createSwapchain();
    void destroySwapchain();
    bool createRenderPass();
    bool chooseSwapFormat();
    bool createDescriptorResources();
    bool createPipelines();
    bool createSyncObjects();
    bool createShaderModule(const std::vector<uint8_t>& code, VkShaderModule* out);
    bool readAsset(const char* name, std::vector<uint8_t>* out);
    SwapSupport querySwapSupport(VkPhysicalDevice dev);
    bool createBuffer(VkDeviceSize size, VkBufferUsageFlags usage,
                      VkMemoryPropertyFlags props, VkBuffer* buf, VkDeviceMemory* mem);
    bool createImage(uint32_t w, uint32_t h, VkFormat fmt, VkImageTiling tiling,
                     VkImageUsageFlags usage, VkMemoryPropertyFlags props, VkImage* img,
                     VkDeviceMemory* mem);
    void uploadAtlas(const std::vector<uint8_t>& rgba);
    void uploadVertices();
    void rebuildLines();
    void rebuildLabelQuads();
    void recordCommandBuffer(VkCommandBuffer cmd, uint32_t imageIndex);

    // --- config / CPU state ---
    AAssetManager* assets_ = nullptr;
    ANativeWindow* window_ = nullptr;
    VkClearValue clear_{};
    int viewW_ = 0;
    int viewH_ = 0;
    float scrollX_ = 0.0f;
    float scrollY_ = 0.0f;
    float scale_ = 1.0f;
    float pivotX_ = 0.0f;
    float pivotY_ = 0.0f;
    float textColor_[4] = {0.41f, 0.34f, 0.38f, 1.0f};
    float bubbleColor_[4] = {0.79f, 0.79f, 0.79f, 1.0f};

    std::vector<ColorVertex> lineVerts_;
    std::vector<ColorVertex> bubbleVerts_;
    std::vector<TextVertex> textVerts_;
    std::vector<LabelBitmap> labelBitmaps_;
    std::vector<PlacedLabel> placedLabels_;
    std::vector<std::pair<float, float>> labelAnchors_;
    std::vector<uint8_t> atlasRgba_;
    bool dataDirty_ = false;
    bool labelsDirty_ = false;
    bool atlasDirty_ = false;
    VkImageLayout atlasLayout_ = VK_IMAGE_LAYOUT_UNDEFINED;
    FreeTypeText ft_;

    // --- Vulkan state ---
    VkInstance instance_ = VK_NULL_HANDLE;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
    VkPhysicalDevice physDev_ = VK_NULL_HANDLE;
    VkDevice device_ = VK_NULL_HANDLE;
    VkQueue gfxQueue_ = VK_NULL_HANDLE;
    uint32_t gfxFamily_ = 0;
    VkSwapchainKHR swapchain_ = VK_NULL_HANDLE;
    VkFormat swapFormat_ = VK_FORMAT_UNDEFINED;
    VkColorSpaceKHR swapColorSpace_ = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
    VkExtent2D swapExtent_{0, 0};
    std::vector<VkImage> swapImages_;
    std::vector<VkImageView> swapViews_;
    std::vector<VkFramebuffer> framebuffers_;
    VkRenderPass renderPass_ = VK_NULL_HANDLE;
    VkDescriptorSetLayout dsLayout_ = VK_NULL_HANDLE;
    VkPipelineLayout pipeLayoutColor_ = VK_NULL_HANDLE;
    VkPipelineLayout pipeLayoutText_ = VK_NULL_HANDLE;
    VkPipeline pipeColor_ = VK_NULL_HANDLE;
    VkPipeline pipeText_ = VK_NULL_HANDLE;
    VkDescriptorPool descPool_ = VK_NULL_HANDLE;
    VkDescriptorSet descSet_ = VK_NULL_HANDLE;
    VkSampler sampler_ = VK_NULL_HANDLE;
    VkImage atlasImage_ = VK_NULL_HANDLE;
    VkDeviceMemory atlasMem_ = VK_NULL_HANDLE;
    VkImageView atlasView_ = VK_NULL_HANDLE;
    VkBuffer stagingBuf_ = VK_NULL_HANDLE;
    VkDeviceMemory stagingMem_ = VK_NULL_HANDLE;
    VkDeviceSize stagingCap_ = 0;
    VkBuffer lineBuf_ = VK_NULL_HANDLE;
    VkDeviceMemory lineMem_ = VK_NULL_HANDLE;
    VkDeviceSize lineCap_ = 0;
    VkBuffer bubbleBuf_ = VK_NULL_HANDLE;
    VkDeviceMemory bubbleMem_ = VK_NULL_HANDLE;
    VkDeviceSize bubbleCap_ = 0;
    VkBuffer textBuf_ = VK_NULL_HANDLE;
    VkDeviceMemory textMem_ = VK_NULL_HANDLE;
    VkDeviceSize textCap_ = 0;
    VkCommandPool cmdPool_ = VK_NULL_HANDLE;
    VkCommandBuffer cmdBuf_ = VK_NULL_HANDLE;
    VkSemaphore imgAvail_ = VK_NULL_HANDLE;
    VkSemaphore renderDone_ = VK_NULL_HANDLE;
    VkFence renderFence_ = VK_NULL_HANDLE;
    bool ready_ = false;
};

}  // namespace metro
