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
      version = "3305.v25f9a_966fc07";
      package = system:
        let
          pkgs = import nixpkgs { inherit system; };
          src = pkgs.fetchurl {
            url = "https://github.com/jenkins-infra/plugin-modernizer-tool/releases/download/${version}/jenkins-plugin-modernizer-${version}.jar";
            hash = builtins.convertHash {
              hash = "d0c8d6099b824e650d209dc8b6ed5fbb7f474518545f9456e9513a95a998af60";
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
