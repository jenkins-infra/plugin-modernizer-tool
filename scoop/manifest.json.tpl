{
    "version": "{{projectVersion}}",
    "description": "{{projectDescription}}",
    "homepage": "{{projectLinkHomepage}}",
    "license": "{{projectLicense}}",
    "url": "{{distributionUrl}}#/{{distributionExecutableWindows}}",
    "hash": "sha256:{{distributionChecksumSha256}}",
    "bin": "{{distributionExecutableWindows}}",
    "checkver": {
        "url": "https://api.github.com/repos/jenkins-infra/plugin-modernizer-tool/releases/latest",
        "jsonpath": "$.tag_name"
    },
    "autoupdate": {
        "url": "{{scoopAutoupdateUrl}}#/{{distributionExecutableWindows}}",
        "hash": {
            "url": "$url.sha256"
        }
    }
}
