#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "geometry.h"

namespace metro {

// Grayscale bitmap of one rasterized label.
struct LabelBitmap {
    int w = 0;
    int h = 0;
    int baselineY = 0;  // distance in px from the top row to the text baseline
    std::vector<uint8_t> px;  // w*h grayscale (0 = transparent, 255 = opaque)
};

// One label placed into the atlas, with UVs and CPU-side pixel copy info.
struct PlacedLabel {
    AtlasRect rect;
    float anchorX = 0.0f;  // map-space anchor (station point)
    float anchorY = 0.0f;
};

// Decodes UTF-8 into code points. Returns false on invalid input.
bool decodeUtf8(const std::string& s, std::vector<uint32_t>* out);

// Assembles label bitmaps into a single white-on-transparent RGBA atlas.
// Returns false if a bitmap does not fit. Pure logic over LabelBitmap,
// no FreeType dependency, so it is unit-testable.
bool buildAtlas(const std::vector<LabelBitmap>& labels, int atlasW, int atlasH,
                std::vector<uint8_t>* rgba, std::vector<PlacedLabel>* placed);

// Native FreeType wrapper: loads a TTF/OTF font file and rasterizes
// whole labels (LTR, kerning, no complex shaping — sufficient for
// Ukrainian station names).
class FreeTypeText {
public:
    FreeTypeText();
    ~FreeTypeText();
    FreeTypeText(const FreeTypeText&) = delete;
    FreeTypeText& operator=(const FreeTypeText&) = delete;

    bool init();
    // Tries each path in order, keeps the first font that loads.
    bool loadFont(const char* const* paths, int pathCount, int pixelSize);
    // Tries preferred fonts, then scans /system/fonts and /product/fonts for
    // the first face covering Ukrainian Cyrillic. Android devices vary wildly
    // in bundled fonts, so hardcoding one path is not viable.
    bool loadSystemFont(int pixelSize);
    bool hasFont() const;
    bool rasterizeLabel(const std::string& utf8, LabelBitmap* out);

private:
    struct Impl;
    Impl* impl_;
};

}  // namespace metro
