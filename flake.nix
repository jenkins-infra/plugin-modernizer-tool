{
  description = "Plugin Modernizer Tool";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-26.05";

  outputs = { nixpkgs, ... }:
    let
      systems = [
        "aarch64-darwin"
        "aarch64-linux"
        "x86_64-linux"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      version = "3377.v9ff1a_7e58158";
      package = system:
        let
          pkgs = import nixpkgs { inherit system; };
          platform = if pkgs.stdenv.hostPlatform.isDarwin then "osx-aarch_64" else if pkgs.stdenv.hostPlatform.isAarch64 then "linux-aarch_64" else "linux-x86_64";
          src = pkgs.fetchurl {
            url = "https://github.com/jenkins-infra/plugin-modernizer-tool/releases/download/${version}/jenkins-plugin-modernizer-${version}-${platform}";
            hash = if pkgs.stdenv.hostPlatform.isDarwin
              then "sha256-ec8buIFrNM82tlzyIQmLx19VhhkJ6o6YgbKqlW7rsuc="
              else if pkgs.stdenv.hostPlatform.isAarch64
              then "sha256-9YXjDGOoFbllyKA6QerBjIiEgbJuBaBFQbx4CM3o2rY="
              else "sha256-ztPP59DBP8P2lMJtcRrhZCbCdV2qlKRUIhzkbBtYRQc=";
          };
        in
        pkgs.stdenvNoCC.mkDerivation {
          pname = "plugin-modernizer";
          inherit version src;
          dontUnpack = true;
          installPhase = ''
            install -Dm755 "$src" "$out/bin/plugin-modernizer"
          '';
          meta = {
            description = "CLI tool for modernizing Jenkins plugins";
            homepage = "https://github.com/jenkins-infra/plugin-modernizer-tool";
            license = pkgs.lib.licenses.mit;
            mainProgram = "plugin-modernizer";
            platforms = systems;
          };
        };
    in
    {
      packages = forAllSystems (system: {
        default = package system;
        plugin-modernizer = package system;
      });
    };
}
