// Host-side unit tests for the portable metroview core (geometry expansion,
// atlas shelf packing, UTF-8 decoding, atlas assembly). No Vulkan, no JNI.
// Build: g++ -std=c++17 -I.. -I/usr/include/freetype2 test_main.cpp
//        ../geometry.cpp ../text_atlas.cpp -lfreetype -o test_main && ./test_main

#include <cmath>
#include <cstdio>
#include <string>
#include <vector>

#include "../geometry.h"
#include "../text_atlas.h"

static int failures = 0;

#define CHECK(cond)                                                            \
    do {                                                                       \
        if (!(cond)) {                                                         \
            std::printf("FAIL %s:%d: %s\n", __FILE__, __LINE__, #cond);       \
            ++failures;                                                        \
        }                                                                      \
    } while (0)

static bool approx(float a, float b) { return std::fabs(a - b) < 1e-4f; }

static void testHorizontalSegment() {
    std::vector<metro::ColorVertex> v;
    // Segment (0,0)-(10,0), width 4 -> quad x in [0,10], y in [-2,2].
    metro::appendSegment(v, 0, 0, 10, 0, 1, 0, 0, 1, 4.0f);
    // 6 quad verts + 2 discs * 10 slices * 3 verts = 66.
    CHECK(v.size() == 66);
    float minX = 1e9f, maxX = -1e9f, minY = 1e9f, maxY = -1e9f;
    for (const auto& p : v) {
        if (p.x < minX) minX = p.x;
        if (p.x > maxX) maxX = p.x;
        if (p.y < minY) minY = p.y;
        if (p.y > maxY) maxY = p.y;
    }
    CHECK(approx(minX, -2.0f) && approx(maxX, 12.0f));
    CHECK(approx(minY, -2.0f) && approx(maxY, 2.0f));
}

static void testDegenerateSegment() {
    std::vector<metro::ColorVertex> v;
    metro::appendSegment(v, 5, 5, 5, 5, 0, 1, 0, 1, 4.0f);
    // Zero-length segment degrades to a single disc.
    CHECK(v.size() == 30);
    for (const auto& p : v) {
        CHECK(std::hypot(p.x - 5.0f, p.y - 5.0f) <= 2.0f + 1e-4f);
    }
}

static void testPolylineJoints() {
    std::vector<metro::ColorVertex> v;
    const float pts[] = {0, 0, 10, 0, 10, 10};
    metro::appendPolyline(v, pts, 3, 0, 0, 1, 1, 4.0f);
    // 2 segments * 66 verts each = 132 (joint discs included per segment).
    CHECK(v.size() == 132);
    // Corner disc at (10,0) must be covered: find a vertex within radius.
    bool nearCorner = false;
    for (const auto& p : v) {
        if (std::hypot(p.x - 10.0f, p.y - 0.0f) < 0.5f) {
            nearCorner = true;
        }
    }
    CHECK(nearCorner);
}

static void testSinglePoint() {
    std::vector<metro::ColorVertex> v;
    const float pts[] = {7, 9};
    metro::appendPolyline(v, pts, 1, 1, 1, 1, 1, 6.0f);
    CHECK(v.size() == 30);
}

static void testRect() {
    std::vector<metro::ColorVertex> v;
    metro::appendRect(v, 1, 2, 5, 8, 1, 1, 1, 1);
    CHECK(v.size() == 6);
    for (const auto& p : v) {
        CHECK(p.x >= 1.0f && p.x <= 5.0f && p.y >= 2.0f && p.y <= 8.0f);
    }
}

static void testShelfPacker() {
    metro::ShelfPacker packer(64, 32);
    metro::AtlasRect a, b, c;
    CHECK(packer.pack(32, 16, &a));
    CHECK(packer.pack(32, 16, &b));
    // Third rect forces a new row.
    CHECK(packer.pack(64, 16, &c));
    CHECK(a.x == 0 && a.y == 0 && b.x == 32 && b.y == 0);
    CHECK(c.x == 0 && c.y == 16);
    // Atlas full now: anything else must fail.
    metro::AtlasRect d;
    CHECK(!packer.pack(64, 16, &d));
    CHECK(!packer.pack(65, 1, &d));
}

static void testTransforms() {
    // Zoom x2 around pivot (100,100): map point (10,20) + scroll (5,-5).
    float sx, sy;
    metro::mapToScreen(10, 20, 5, -5, 2.0f, 100, 100, &sx, &sy);
    CHECK(approx(sx, (10 + 5 - 100) * 2 + 100) && approx(sy, (20 - 5 - 100) * 2 + 100));
    float nx, ny;
    metro::screenToNdc(200, 100, 400, 200, &nx, &ny);
    CHECK(approx(nx, 0.0f) && approx(ny, 0.0f));
    metro::screenToNdc(0, 0, 400, 200, &nx, &ny);
    CHECK(approx(nx, -1.0f) && approx(ny, -1.0f));
}

static void testUtf8() {
    std::vector<uint32_t> cps;
    CHECK(metro::decodeUtf8("ABC", &cps) && cps.size() == 3 && cps[0] == 0x41);
    // "Київ": U+041A U+0438 U+0457 U+0432.
    CHECK(metro::decodeUtf8("Київ", &cps) && cps.size() == 4);
    CHECK(cps[0] == 0x41A && cps[1] == 0x438 && cps[2] == 0x457 && cps[3] == 0x432);
    CHECK(!metro::decodeUtf8("\xff\xff", &cps));
    CHECK(!metro::decodeUtf8("ab\xE2\x82", &cps));  // truncated sequence
}

static void testBuildAtlas() {
    metro::LabelBitmap l1, l2;
    l1.w = 8;
    l1.h = 4;
    l1.baselineY = 3;
    l1.px.assign(32, 255);
    l2.w = 4;
    l2.h = 4;
    l2.baselineY = 3;
    l2.px.assign(16, 128);
    std::vector<uint8_t> rgba;
    std::vector<metro::PlacedLabel> placed;
    CHECK(metro::buildAtlas({l1, l2}, 32, 16, &rgba, &placed));
    CHECK(rgba.size() == 32 * 16 * 4 && placed.size() == 2);
    // White RGB, alpha from bitmap.
    CHECK(rgba[0] == 255 && rgba[1] == 255 && rgba[2] == 255 && rgba[3] == 255);
    const size_t o = (static_cast<size_t>(placed[1].rect.y) * 32 + placed[1].rect.x) * 4;
    CHECK(rgba[o + 3] == 128);
    // Too small must fail cleanly.
    CHECK(!metro::buildAtlas({l1, l2}, 8, 4, &rgba, &placed));
}

static void testFreeTypeRaster() {
    metro::FreeTypeText ft;
    CHECK(ft.init());
    const char* fonts[] = {"/usr/share/fonts/noto/NotoSans-Regular.ttf"};
    if (!ft.loadFont(fonts, 1, 56)) {
        std::printf("SKIP freetype raster (font missing)\n");
        return;
    }
    CHECK(ft.hasFont());
    metro::LabelBitmap bmp;
    CHECK(ft.rasterizeLabel("Київ 123", &bmp));
    CHECK(bmp.w > 20 && bmp.h > 0);
    CHECK(bmp.baselineY >= 0 && bmp.baselineY <= bmp.h);
    size_t ink = 0;
    for (uint8_t v : bmp.px) {
        if (v > 0) ++ink;
    }
    CHECK(ink > 50);
    std::printf("raster w=%d h=%d baseline=%d ink=%zu\n", bmp.w, bmp.h,
                bmp.baselineY, ink);
}

int main() {
    testHorizontalSegment();
    testDegenerateSegment();
    testPolylineJoints();
    testSinglePoint();
    testRect();
    testShelfPacker();
    testTransforms();
    testUtf8();
    testBuildAtlas();
    testFreeTypeRaster();
    if (failures == 0) {
        std::printf("ALL HOST TESTS PASSED\n");
        return 0;
    }
    std::printf("%d FAILURES\n", failures);
    return 1;
}
