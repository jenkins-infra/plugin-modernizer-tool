{
  description = "Plugin Modernizer Tool";

  inputs.nixpkgs.url = "https://channels.nixos.org/nixos-26.05/nixexprs.tar.zst";

  outputs = { nixpkgs, ... }:
    let
      systems = [
        "aarch64-darwin"
        "aarch64-linux"
        "x86_64-darwin"
        "x86_64-linux"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      version = "3255.v1b_e36cc2c10d";
      package = system:
        let
          pkgs = import nixpkgs { inherit system; };
          src = pkgs.fetchurl {
            url = "https://github.com/jenkins-infra/plugin-modernizer-tool/releases/download/${version}/jenkins-plugin-modernizer-${version}.jar";
            hash = builtins.convertHash {
              hash = "e69ac90ffc49499cc3e084ee5d66a353db0802671357e4e6718c0c9e6fa4a79c";
              hashAlgo = "sha256";
              toHashFormat = "sri";
            };
          };
        in
        pkgs.stdenvNoCC.mkDerivation {
          pname = "plugin-modernizer";
          inherit version src;
          dontUnpack = true;
          nativeBuildInputs = [ pkgs.makeWrapper ];
          installPhase = ''
            install -Dm444 "$src" "$out/share/java/plugin-modernizer.jar"
            makeWrapper "${pkgs.openjdk25_headless}/bin/java" "$out/bin/plugin-modernizer" \
              --add-flags "-jar $out/share/java/plugin-modernizer.jar"
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
