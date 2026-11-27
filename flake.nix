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
      version = "3383.v81b_0d8a_b_9450";
      package = system:
        let
          pkgs = import nixpkgs { inherit system; };
          platform = if pkgs.stdenv.hostPlatform.isDarwin then "osx-aarch_64" else if pkgs.stdenv.hostPlatform.isAarch64 then "linux-aarch_64" else "linux-x86_64";
          src = pkgs.fetchurl {
            url = "https://github.com/jenkins-infra/plugin-modernizer-tool/releases/download/${version}/jenkins-plugin-modernizer-${version}-${platform}";
            hash = if pkgs.stdenv.hostPlatform.isDarwin
              then "sha256:79cf1bb8816b34cf36b65cf221098bc75f55861909ea8e9881b2aa956eebb2e7"
              else if pkgs.stdenv.hostPlatform.isAarch64
              then "sha256:f585e30c63a815b965c8a03a41eac18c888481b26e05a04541bc7808cde8dab6"
              else "sha256:09bd0decaea952c2611f87ea2d91e8b566e20a8001aa780e0619df4d12030092";
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
