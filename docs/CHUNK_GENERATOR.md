# Registered bounded DEM chunk generator

Earthward now registers `earthward:pilot`, a serializable NeoForge/Minecraft chunk generator codec. The generator stores only the external package identifier in level data and refuses to decode unless the matching elevation package is present and passes path, size, schema and SHA-256 verification.

Terrain density is derived directly from the verified Copernicus DSM at one vertical block per source metre plus the constant Minecraft sea-level translation. Aquifers and vanilla structures are disabled. Outside installed coverage the density is void rather than invented geography. The pilot currently uses plains as an explicit placeholder because land-cover data has not yet been sourced.

Registration and compilation do not yet mean the generator has been launched in a real Minecraft client. The map's create button remains guarded until elevation is staged with geometry, a safe spawn is proved, save ownership metadata is implemented, and client loading succeeds.
