# Single CNY currency assets

These assets were extracted from the user's original 1320 × 2868 screenshots. Each file contains one complete banknote or coin on a transparent background. Banknotes are straightened for reuse; apply rotation in the consuming UI when needed.

| Asset | Denomination | Native artwork size |
| --- | --- | --- |
| `yuan-100` | 100 yuan | 620 × 308 px |
| `yuan-50` | 50 yuan | 493 × 234 px |
| `yuan-20` | 20 yuan | 485 × 230 px |
| `yuan-10` | 10 yuan | 479 × 227 px |
| `yuan-5` | 5 yuan | 471 × 223 px |
| `yuan-1` | 1 yuan | 456 × 216 px |
| `jiao-5` | 0.50 yuan | 86 × 87 px |
| `jiao-1` | 0.10 yuan | 85 × 87 px |
| `fen-1` | 0.01 yuan | 96 × 98 px |

- `.png`: native-resolution transparent master, with 8 px padding on each side.
- `.webp`: lossless equivalent of the PNG master.
- `@2x.png`: interpolated 2× export with restrained sharpening and 16 px padding. It does not contain additional source detail.
- `manifest.json`: denominations in integer fen, dimensions, source screenshot names, crop bounds and rotation corrections.

Use the native master or lossless WebP for normal UI sizes. Keep image aspect ratios and use the source detail size when choosing a display size. Coin detail is limited by the approximately 90 px originals; larger exports should not be presented as new high-resolution artwork.

Public URL prefix: `/mobile/finance/currency/single/`.

The older `../yuan-100.png` is a separate stack extracted from the earlier reference. It is not the single-note asset in this folder. No AI generation or redraw was used for these exports.
