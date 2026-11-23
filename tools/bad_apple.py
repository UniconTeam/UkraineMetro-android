#!/usr/bin/env python3
"""Bad Apple -> Vulkan metro-map lineart asset pipeline.

Downloads the Bad Apple video (if needed), then uses ffmpeg to:
  1. extract the audio track (for the app to play at 40% volume), and
  2. convert the video to 30 fps low-res grayscale frames,

then vectorizes every frame into smoothed outline polylines ("lineart")
that the Vulkan map renderer draws as curved map lines (BranchElements,
which the native renderer smooths with Catmull-Rom).

Outputs (used by the Android app):
  app/src/main/assets/badapple/frames.bin  - lineart frames
  app/src/main/assets/badapple/meta.json   - width/height/fps/count
  app/src/main/assets/badapple/audio.m4a   - audio track

frames.bin format (little-endian), version 2:
  magic      4 bytes  b"BAPL"
  version    u8       2
  width      u16      (e.g. 256)
  height     u16      (e.g. 192)
  fps        u8       30
  frames     u32      frame count N
  offsets    u32[N]   byte offset of each frame payload from file start
  payload[i] u16 polyCount, then per poly: u16 ptCount + ptCount*(u8 x, u8 y)

Coordinate space is 0..width-1 / 0..height-1. The app scales it up
(e.g. x6) into map units before feeding TransElements to VulkanMetroView.

Usage:
  python3 tools/bad_apple.py [--limit N] [--width 160 --height 120]
"""
import argparse
import json
import struct
import subprocess
import sys
import tempfile
import urllib.request
from pathlib import Path

import numpy as np
from PIL import Image

VIDEO_URL = "https://github.com/bad-apple-lab/Bad-Apple/raw/refs/heads/main/badapple.mp4"

ROOT = Path(__file__).resolve().parent.parent
TOOLS_DIR = ROOT / "tools"
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets" / "badapple"

MAGIC = b"BAPL"
VERSION = 2


def run(cmd: list[str]) -> None:
    print("+", " ".join(cmd), flush=True)
    subprocess.run(cmd, check=True)


def download_video(dest: Path) -> None:
    if dest.exists() and dest.stat().st_size > 0:
        print(f"video exists: {dest} ({dest.stat().st_size / 1e6:.1f} MB), skip download")
        return
    print(f"downloading {VIDEO_URL} -> {dest} ...", flush=True)
    dest.parent.mkdir(parents=True, exist_ok=True)
    urllib.request.urlretrieve(VIDEO_URL, dest)
    print(f"downloaded {dest.stat().st_size / 1e6:.1f} MB")


def extract_audio(src: Path, out: Path) -> None:
    """Get the audio track with ffmpeg."""
    out.parent.mkdir(parents=True, exist_ok=True)
    run([
        "ffmpeg", "-y", "-i", str(src),
        "-vn", "-c:a", "aac", "-b:a", "128k",
        str(out),
    ])
    print(f"audio: {out} ({out.stat().st_size / 1e6:.2f} MB)")


def extract_frames(src: Path, tmp: Path, width: int, height: int, fps: int, limit: int) -> list[Path]:
    """Convert video to 30fps low-res grayscale PNG frames with ffmpeg."""
    tmp.mkdir(parents=True, exist_ok=True)
    # fps + scale + gray. -vframes limits for quick tests.
    cmd = [
        "ffmpeg", "-y", "-i", str(src),
        "-vf", f"fps={fps},scale={width}:{height},format=gray",
    ]
    if limit > 0:
        cmd += ["-vframes", str(limit)]
    cmd += [str(tmp / "f_%05d.png")]
    run(cmd)
    frames = sorted(tmp.glob("f_*.png"))
    print(f"frames: {len(frames)} x {width}x{height} @ {fps}fps")
    if not frames:
        sys.exit("ffmpeg produced no frames")
    return frames


def trace_contours(edge: "np.ndarray") -> list[list[tuple[int, int]]]:
    """Chain 1px edge pixels into ordered polylines via greedy 8-neighbor
    walks (preferring the straightest continuation)."""
    h, w = edge.shape
    remaining = set(zip(*np.nonzero(edge)))  # (y, x) pairs
    polys: list[list[tuple[int, int]]] = []
    while remaining:
        seed = remaining.pop()
        fwd: list[tuple[int, int]] = [seed]
        bwd: list[tuple[int, int]] = []
        for chain, first_step in ((fwd, True), (bwd, False)):
            y, x = seed
            # Incoming direction for the straightness preference.
            dy, dx = 0, 0
            while True:
                best = None
                best_cost = 3
                for ny in (y - 1, y, y + 1):
                    if ny < 0 or ny >= h:
                        continue
                    for nx in (x - 1, x, x + 1):
                        if (nx == x and ny == y) or nx < 0 or nx >= w:
                            continue
                        if (ny, nx) not in remaining:
                            continue
                        # Cost = direction change (0 straight .. 2 U-turn).
                        step_y, step_x = ny - y, nx - x
                        cost = abs(step_y - dy) + abs(step_x - dx)
                        if cost < best_cost:
                            best_cost = cost
                            best = (ny, nx)
                            if cost == 0:
                                break
                    if best_cost == 0:
                        break
                if best is None:
                    break
                dy, dx = best[0] - y, best[1] - x
                y, x = best
                remaining.discard(best)
                chain.append(best)
        poly = [(x, y) for (y, x) in reversed(bwd)] + [(x, y) for (y, x) in fwd]
        if len(poly) >= 2:
            polys.append(poly)
        elif poly:
            polys.append(poly)  # single dot
    return polys


def douglas_peucker(pts: list[tuple[int, int]], eps: float) -> list[tuple[int, int]]:
    """Simplify a polyline, keeping endpoints. Iterative, no recursion."""
    if len(pts) <= 2:
        return pts
    keep = [True] + [False] * (len(pts) - 2) + [True]
    stack = [(0, len(pts) - 1)]
    while stack:
        a, b = stack.pop()
        ax, ay = pts[a]
        bx, by = pts[b]
        dx, dy = bx - ax, by - ay
        denom = (dx * dx + dy * dy) ** 0.5
        far, far_d = -1, eps
        for i in range(a + 1, b):
            px, py = pts[i]
            if denom == 0:
                d = ((px - ax) ** 2 + (py - ay) ** 2) ** 0.5
            else:
                d = abs(dy * px - dx * py + bx * ay - by * ax) / denom
            if d > far_d:
                far_d, far = d, i
        if far >= 0:
            keep[far] = True
            stack.append((a, far))
            stack.append((far, b))
    return [p for p, k in zip(pts, keep) if k]


def frame_to_polys(
    png: Path,
    bbox: tuple[int, int, int, int],
    width: int,
    height: int,
    threshold: int = 128,
    dp_eps: float = 1.1,
) -> list[list[tuple[int, int]]]:
    """Crop to content, aspect-fit centered on a white canvas, then
    threshold -> 1px silhouette outline -> ordered contour polylines,
    simplified with Douglas-Peucker.

    The aspect-preserving fit + symmetric padding centers the content by
    construction, so centering the frame bounds centers the video.
    The app renders each polyline as a smoothed map line, so curves stay
    curves instead of pixel staircases.
    """
    img = Image.open(png).convert("L")
    x0, y0, x1, y1 = bbox
    img = img.crop((x0, y0, x1 + 1, y1 + 1))
    cw, ch = img.size
    s = min(width / cw, height / ch)
    fw, fh = max(1, round(cw * s)), max(1, round(ch * s))
    img = img.resize((fw, fh), Image.BILINEAR)
    canvas = Image.new("L", (width, height), 255)
    canvas.paste(img, ((width - fw) // 2, (height - fh) // 2))
    a = np.array(canvas)
    black = a < threshold  # True = silhouette
    h, w = black.shape
    # Edge = black pixel with at least one white/void 4-neighbor.
    up = np.ones_like(black)
    up[1:, :] = black[:-1, :]
    down = np.ones_like(black)
    down[:-1, :] = black[1:, :]
    left = np.ones_like(black)
    left[:, 1:] = black[:, :-1]
    right = np.ones_like(black)
    right[:, :-1] = black[:, 1:]
    edge = black & (~(up & down & left & right))
    polys = trace_contours(edge)
    return [douglas_peucker(p, dp_eps) for p in polys if len(p) >= 1]


def compute_content_bbox(pngs: list[Path], threshold: int = 128, margin: int = 4) -> tuple[int, int, int, int]:
    """Union bounding box of dark pixels over sampled frames."""
    probe = Image.open(pngs[0]).convert("L")
    pw, ph = probe.size
    x0, y0, x1, y1 = pw, ph, -1, -1
    for png in pngs[::5]:
        a = np.array(Image.open(png).convert("L"))
        ys, xs = np.nonzero(a < threshold)
        if xs.size == 0:
            continue
        x0 = min(x0, int(xs.min()))
        x1 = max(x1, int(xs.max()))
        y0 = min(y0, int(ys.min()))
        y1 = max(y1, int(ys.max()))
    if x1 < x0:  # no dark pixels at all: keep the full frame
        return (0, 0, pw - 1, ph - 1)
    return (
        max(0, x0 - margin),
        max(0, y0 - margin),
        min(pw - 1, x1 + margin),
        min(ph - 1, y1 + margin),
    )


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0, help="only process first N frames (0 = all)")
    ap.add_argument("--width", type=int, default=256)
    ap.add_argument("--height", type=int, default=192)
    ap.add_argument("--extract-scale", type=int, default=2,
                    help="extract at width*scale for quality, then fit down")
    ap.add_argument("--dp-eps", type=float, default=1.1,
                    help="Douglas-Peucker simplification epsilon, output px")
    ap.add_argument("--fps", type=int, default=30)
    ap.add_argument("--video", type=Path, default=TOOLS_DIR / "badapple.mp4")
    args = ap.parse_args()

    download_video(args.video)
    extract_audio(args.video, ASSET_DIR / "audio.m4a")

    ex_w, ex_h = args.width * args.extract_scale, args.height * args.extract_scale
    with tempfile.TemporaryDirectory(prefix="badapple_") as td:
        pngs = extract_frames(args.video, Path(td), ex_w, ex_h, args.fps, args.limit)
        print("computing content bbox ...", flush=True)
        bbox = compute_content_bbox(pngs)
        print(f"  content bbox (at {ex_w}x{ex_h}): {bbox}")
        all_polys: list[list[list[tuple[int, int]]]] = []
        total_pts = 0
        total_polys = 0
        for i, png in enumerate(pngs):
            polys = frame_to_polys(png, bbox, args.width, args.height, dp_eps=args.dp_eps)
            all_polys.append(polys)
            total_polys += len(polys)
            total_pts += sum(len(p) for p in polys)
            if (i + 1) % 500 == 0:
                print(f"  vectorized {i + 1}/{len(pngs)} ...", flush=True)
        print(f"vectorized {len(pngs)} frames, {total_polys} polys, {total_pts} pts "
              f"(avg {total_pts / max(len(pngs), 1):.1f} pts/frame)")

        # Encode frames.bin (v2 header: magic + u8 ver + u16 w,h + u8 fps + u32 n)
        ASSET_DIR.mkdir(parents=True, exist_ok=True)
        n = len(all_polys)
        header_size = 4 + 1 + 2 + 2 + 1 + 4 + 4 * n
        offsets: list[int] = []
        payloads: list[bytes] = []
        off = header_size
        for polys in all_polys:
            if len(polys) > 0xFFFF:
                polys = polys[:0xFFFF]
            blob = struct.pack("<H", len(polys))
            for pts in polys:
                if len(pts) > 0xFFFF:
                    pts = pts[:0xFFFF]
                blob += struct.pack("<H", len(pts))
                for (x, y) in pts:
                    blob += struct.pack("BB", x, y)
            payloads.append(blob)
            offsets.append(off)
            off += len(blob)
        out = ASSET_DIR / "frames.bin"
        with open(out, "wb") as f:
            f.write(MAGIC)
            f.write(struct.pack("<BHHB", VERSION, args.width, args.height, args.fps))
            f.write(struct.pack("<I", n))
            for o in offsets:
                f.write(struct.pack("<I", o))
            for blob in payloads:
                f.write(blob)
        meta = {
            "width": args.width, "height": args.height, "fps": args.fps,
            "frames": n, "polys": total_polys, "points": total_pts,
            "format": "BAPL v2: u16 polyCount; per poly u16 ptCount + u8 x,y points",
        }
        with open(ASSET_DIR / "meta.json", "w") as f:
            json.dump(meta, f, indent=2)
        print(f"wrote {out} ({out.stat().st_size / 1e6:.2f} MB) + meta.json")


if __name__ == "__main__":
    main()
