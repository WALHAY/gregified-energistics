{ pkgs ? import <nixpkgs> { } }:

let
  minecraftLibraries = with pkgs; [
    libGL
    libx11
    libxext
    libxcursor
    libxrandr
    libxrender
    libxxf86vm
    libxtst
    libxi
    alsa-lib
    libpulseaudio
    openal
    fontconfig
    freetype
    zlib
    stdenv.cc.cc.lib
  ];
in
pkgs.mkShell {
  # Gradle runs on Java 25; RetroFuturaGradle provisions Java 16 for Jabel
  # compilation and Java 8 for Minecraft 1.12.2 through Foojay.
  packages = [
    pkgs.zulu25
    # LWJGL 2 runs `xrandr -q` to enumerate display modes.
    pkgs.xrandr
  ];
  JAVA_HOME = "${pkgs.zulu25}";

  shellHook = ''
    # Downloaded JDKs and LWJGL 2 need these libraries at runtime on NixOS.
    # Include the host's graphics drivers for hardware-accelerated OpenGL.
    export LD_LIBRARY_PATH="/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath minecraftLibraries}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
  '';
}
