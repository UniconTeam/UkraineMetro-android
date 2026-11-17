# Metro map Vulkan renderer (Android NDK) + vendored FreeType for labels.
cmake_minimum_required(VERSION 3.22.1)
project(metroview LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --- FreeType (native font rasterization for station labels) ---
set(BUILD_SHARED_LIBS OFF)
set(FT_DISABLE_ZLIB ON CACHE BOOL "" FORCE)
set(FT_DISABLE_BZIP2 ON CACHE BOOL "" FORCE)
set(FT_DISABLE_PNG ON CACHE BOOL "" FORCE)
set(FT_DISABLE_HARFBUZZ ON CACHE BOOL "" FORCE)
set(FT_DISABLE_BROTLI ON CACHE BOOL "" FORCE)
set(SKIP_INSTALL_ALL ON CACHE BOOL "" FORCE)

include(FetchContent)
FetchContent_Declare(
    freetype
    URL https://download.savannah.gnu.org/releases/freetype/freetype-2.15.0.tar.gz
    URL_HASH SHA256=9186627467a616d9a9e3e574748ac0c45fa77398e6364edef3041a8ba8d92f64
)
FetchContent_MakeAvailable(freetype)

add_library(metroview SHARED
    metro_jni.cpp
    renderer.cpp
    geometry.cpp
    text_atlas.cpp
)

target_include_directories(metroview PRIVATE
    ${freetype_SOURCE_DIR}/include
)

find_library(vulkan-lib vulkan)
find_library(android-lib android)
find_library(log-lib log)

target_link_libraries(metroview
    freetype
    ${vulkan-lib}
    ${android-lib}
    ${log-lib}
)

target_compile_options(metroview PRIVATE -Wall -Wextra -Wno-unused-parameter)
