# Pilot elevation package

Earthward now acquires a real bounded Copernicus DEM GLO-30 Public 2021 Cloud Optimized GeoTIFF from the public AWS Open Data bucket without credentials. The pilot rectangle is cropped into an immutable JSON DSM grid with source hash, output hash, acquisition time, licence URL, CRS, EGM2008 vertical reference, dimensions and exact metre samples.

The Java runtime has a separate fail-closed elevation loader. It verifies path safety, file size, SHA-256, schema, CRS, vertical reference and finite sample limits before exposing bilinear geographic sampling. No vertical scaling or compression is applied.

Important limitation: GLO-30 is an approximately 30 m digital surface model, not one-metre measured bare-earth terrain. It may include structures and vegetation. The requested 1 block = 1 metre rule refers to world scale; it does not increase source resolution. Generation and safe spawn remain disabled until the following stages connect this verified grid to a bounded chunk generator and validate resulting surfaces.
