#include "text_atlas.h"

#include <dirent.h>

#include <ft2build.h>
#include FT_FREETYPE_H
#include FT_BITMAP_H

namespace metro {

namespace {

// Codepoints that must exist for Ukrainian station names: К и ї І.
bool hasUkrainianCyrillic(FT_Face face) {
    static const uint32_t kNeeded[] = {0x041A, 0x0438, 0x0457, 0x0406};
    for (uint32_t cp : kNeeded) {
        if (FT_Get_Char_Index(face, cp) == 0) {
            return false;
        }
    }
    return true;
}

bool hasFontSuffix(const char* name) {
    const std::string n = name != nullptr ? name : "";
    const size_t dot = n.find_last_of('.');
    if (dot == std::string::npos) {
        return false;
    }
    const std::string ext = n.substr(dot);
    return ext == ".ttf" || ext == ".otf" || ext == ".ttc";
}

}  // namespace

bool decodeUtf8(const std::string& s, std::vector<uint32_t>* out) {
    if (out == nullptr) {
        return false;
    }
    out->clear();
    size_t i = 0;
    while (i < s.size()) {
        const auto c = static_cast<unsigned char>(s[i]);
        uint32_t cp = 0;
        size_t extra = 0;
        if (c < 0x80) {
            cp = c;
            extra = 0;
        } else if ((c & 0xE0) == 0xC0) {
            cp = c & 0x1F;
            extra = 1;
        } else if ((c & 0xF0) == 0xE0) {
            cp = c & 0x0F;
            extra = 2;
        } else if ((c & 0xF8) == 0xF0) {
            cp = c & 0x07;
            extra = 3;
        } else {
            return false;
        }
        if (i + extra >= s.size()) {
            return false;
        }
        for (size_t k = 1; k <= extra; ++k) {
            const auto cc = static_cast<unsigned char>(s[i + k]);
            if ((cc & 0xC0) != 0x80) {
                return false;
            }
            cp = (cp << 6) | (cc & 0x3F);
        }
        out->push_back(cp);
        i += 1 + extra;
    }
    return true;
}

bool buildAtlas(const std::vector<LabelBitmap>& labels, int atlasW, int atlasH,
                std::vector<uint8_t>* rgba, std::vector<PlacedLabel>* placed) {
    if (rgba == nullptr || placed == nullptr || atlasW <= 0 || atlasH <= 0) {
        return false;
    }
    ShelfPacker packer(atlasW, atlasH);
    placed->clear();
    placed->reserve(labels.size());
    for (const auto& label : labels) {
        AtlasRect r;
        if (!packer.pack(label.w, label.h, &r)) {
            return false;
        }
        PlacedLabel p;
        p.rect = r;
        placed->push_back(p);
    }
    rgba->assign(static_cast<size_t>(atlasW) * atlasH * 4, 0);
    for (size_t i = 0; i < labels.size(); ++i) {
        const auto& label = labels[i];
        const auto& r = (*placed)[i].rect;
        for (int y = 0; y < label.h; ++y) {
            for (int x = 0; x < label.w; ++x) {
                const size_t dst =
                    (static_cast<size_t>(r.y + y) * atlasW + (r.x + x)) * 4;
                const uint8_t a = label.px[static_cast<size_t>(y) * label.w + x];
                (*rgba)[dst] = 255;
                (*rgba)[dst + 1] = 255;
                (*rgba)[dst + 2] = 255;
                (*rgba)[dst + 3] = a;
            }
        }
    }
    return true;
}

struct FreeTypeText::Impl {
    FT_Library library = nullptr;
    FT_Face face = nullptr;
    int pixelSize = 0;
    bool ready = false;
};

FreeTypeText::FreeTypeText() : impl_(new Impl()) {}
FreeTypeText::~FreeTypeText() {
    if (impl_->face != nullptr) {
        FT_Done_Face(impl_->face);
    }
    if (impl_->library != nullptr) {
        FT_Done_FreeType(impl_->library);
    }
    delete impl_;
}

bool FreeTypeText::init() { return FT_Init_FreeType(&impl_->library) == 0; }

bool FreeTypeText::loadFont(const char* const* paths, int pathCount, int pixelSize) {
    if (impl_->library == nullptr || paths == nullptr || pathCount <= 0) {
        return false;
    }
    for (int i = 0; i < pathCount; ++i) {
        if (paths[i] == nullptr) {
            continue;
        }
        FT_Face face = nullptr;
        if (FT_New_Face(impl_->library, paths[i], 0, &face) == 0) {
            if (FT_Set_Pixel_Sizes(face, 0, static_cast<FT_UInt>(pixelSize)) == 0) {
                impl_->face = face;
                impl_->pixelSize = pixelSize;
                impl_->ready = true;
                return true;
            }
            FT_Done_Face(face);
        }
    }
    return false;
}

bool FreeTypeText::loadSystemFont(int pixelSize) {
    if (impl_->library == nullptr) {
        return false;
    }
    static const char* kDirs[] = {"/system/fonts", "/product/fonts"};
    static const char* kPreferred[] = {
        "Roboto-Regular.ttf",
        "NotoSans-Regular.ttf",
        "GoogleSansFlex-Regular.ttf",
        "GoogleSans-Regular.ttf",
        "DroidSans.ttf",
    };
    auto tryFile = [&](const std::string& path) -> bool {
        // .ttc collections: probe face indices until one fails.
        for (int idx = 0; idx < 4; ++idx) {
            FT_Face face = nullptr;
            if (FT_New_Face(impl_->library, path.c_str(), idx, &face) != 0) {
                break;
            }
            bool ok = FT_Set_Pixel_Sizes(face, 0, static_cast<FT_UInt>(pixelSize)) == 0 &&
                      hasUkrainianCyrillic(face);
            if (ok) {
                impl_->face = face;
                impl_->pixelSize = pixelSize;
                impl_->ready = true;
                return true;
            }
            FT_Done_Face(face);
        }
        return false;
    };
    for (const char* dir : kDirs) {
        for (const char* name : kPreferred) {
            if (tryFile(std::string(dir) + "/" + name)) {
                return true;
            }
        }
        DIR* dp = opendir(dir);
        if (dp == nullptr) {
            continue;
        }
        while (dirent* e = readdir(dp)) {
            if (!hasFontSuffix(e->d_name)) {
                continue;
            }
            if (tryFile(std::string(dir) + "/" + e->d_name)) {
                closedir(dp);
                return true;
            }
        }
        closedir(dp);
    }
    return false;
}

bool FreeTypeText::hasFont() const { return impl_->ready; }

namespace {

// Loads and renders one codepoint, then faux-bolds the bitmap in place
// (no bold system font is guaranteed on device: 1.5px horizontal and
// 0.5px vertical at the raster size, in 26.6 fractional pixels).
// Emboldening failures are tolerated: the plain bitmap is still usable.
bool loadBoldGlyph(FT_Library lib, FT_Face face, uint32_t cp) {
    if (FT_Load_Char(face, cp, FT_LOAD_RENDER) != 0) {
        return false;
    }
    if (FT_GlyphSlot_Own_Bitmap(face->glyph) == 0) {
        FT_Bitmap_Embolden(lib, &face->glyph->bitmap, 96, 32);
    }
    return true;
}

}  // namespace

bool FreeTypeText::rasterizeLabel(const std::string& utf8, LabelBitmap* out) {
    if (!impl_->ready || out == nullptr) {
        return false;
    }
    std::vector<uint32_t> cps;
    if (!decodeUtf8(utf8, &cps) || cps.empty()) {
        return false;
    }
    FT_Face face = impl_->face;
    // First pass: measure.
    int penX = 0;
    int minY = 0;
    int maxY = 0;
    uint32_t prev = 0;
    for (uint32_t cp : cps) {
        if (prev != 0 && FT_HAS_KERNING(face)) {
            FT_Vector kern{};
            FT_Get_Kerning(face, FT_Get_Char_Index(face, prev),
                           FT_Get_Char_Index(face, cp), FT_KERNING_DEFAULT, &kern);
            penX += static_cast<int>(kern.x >> 6);
        }
        if (!loadBoldGlyph(impl_->library, face, cp)) {
            return false;
        }
        {
            const FT_GlyphSlot g = face->glyph;
            const int y0 = -g->bitmap_top;
            const int y1 = y0 + static_cast<int>(g->bitmap.rows);
            if (y0 < minY) {
                minY = y0;
            }
            if (y1 > maxY) {
                maxY = y1;
            }
            penX += static_cast<int>(g->advance.x >> 6);
            prev = cp;
        }
    }
    const int w = penX > 0 ? penX : 1;
    const int h = (maxY - minY) > 0 ? (maxY - minY) : 1;
    LabelBitmap bmp;
    bmp.w = w;
    bmp.h = h;
    bmp.baselineY = -minY;
    bmp.px.assign(static_cast<size_t>(w) * h, 0);
    // Second pass: blit.
    penX = 0;
    prev = 0;
    for (uint32_t cp : cps) {
        if (prev != 0 && FT_HAS_KERNING(face)) {
            FT_Vector kern{};
            FT_Get_Kerning(face, FT_Get_Char_Index(face, prev),
                           FT_Get_Char_Index(face, cp), FT_KERNING_DEFAULT, &kern);
            penX += static_cast<int>(kern.x >> 6);
        }
        if (!loadBoldGlyph(impl_->library, face, cp)) {
            return false;
        }
        const FT_GlyphSlot g = face->glyph;
        const int dstX = penX + g->bitmap_left;
        const int dstY = bmp.baselineY - g->bitmap_top;
        for (unsigned row = 0; row < g->bitmap.rows; ++row) {
            for (unsigned col = 0; col < g->bitmap.width; ++col) {
                const int x = dstX + static_cast<int>(col);
                const int y = dstY + static_cast<int>(row);
                if (x < 0 || y < 0 || x >= w || y >= h) {
                    continue;
                }
                const uint8_t v = g->bitmap.buffer[row * g->bitmap.pitch + col];
                uint8_t& dst = bmp.px[static_cast<size_t>(y) * w + x];
                if (v > dst) {
                    dst = v;
                }
            }
        }
        penX += static_cast<int>(g->advance.x >> 6);
        prev = cp;
    }
    *out = std::move(bmp);
    return true;
}

}  // namespace metro
