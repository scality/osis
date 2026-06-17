#!/bin/bash
# Shared helpers for dev-machine scripts. Source via:
#   . "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/platform.sh"
#
# Provides:
#   detect_os                  -> echoes "mac" | "linux-debian" | "linux-rhel" | "linux"
#   detect_java17_home         -> echoes JAVA_HOME for a JDK 17 installation, or fails with a clear install hint
#   ngrok_config_path          -> echoes the per-platform default ngrok.yml path
#   package_install_hint <tool>-> echoes "<pkgmgr> install <pkg>" tailored to the dev box
#   ca_trust_commands <pem>    -> echoes the per-platform commands to system-trust a PEM cert
#
# Plain bash, no external deps beyond uname / readlink / basename.

detect_os() {
    case "$(uname -s)" in
        Darwin) echo "mac"; return ;;
        Linux)
            if [ -r /etc/os-release ]; then
                # shellcheck disable=SC1091
                . /etc/os-release
                case "${ID_LIKE:-$ID}" in
                    *debian*|*ubuntu*) echo "linux-debian"; return ;;
                    *rhel*|*fedora*|*centos*) echo "linux-rhel"; return ;;
                esac
            fi
            echo "linux"
            ;;
        *) echo "unsupported" ;;
    esac
}

detect_java17_home() {
    # Priority 1: caller already set JAVA_HOME and it's a JDK 17.
    if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        local v
        v=$("${JAVA_HOME}/bin/java" -version 2>&1 | head -1)
        if echo "$v" | grep -qE 'version "17\.'; then
            echo "$JAVA_HOME"
            return 0
        fi
    fi

    # Priority 2: macOS Homebrew.
    if [ "$(detect_os)" = "mac" ] && command -v brew >/dev/null 2>&1; then
        local prefix
        prefix=$(brew --prefix openjdk@17 2>/dev/null || true)
        if [ -n "$prefix" ] && [ -d "$prefix/libexec/openjdk.jdk/Contents/Home" ]; then
            echo "$prefix/libexec/openjdk.jdk/Contents/Home"
            return 0
        fi
    fi

    # Priority 3: standard Linux paths.
    local candidate
    for pattern in \
        '/usr/lib/jvm/java-17-openjdk'* \
        '/usr/lib/jvm/jdk-17'* \
        '/usr/lib/jvm/temurin-17'* \
        '/usr/lib/jvm/zulu-17'* \
        '/opt/java/openjdk-17'*; do
        for candidate in $pattern; do
            if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
                echo "$candidate"
                return 0
            fi
        done
    done

    echo "JDK 17 not found." >&2
    echo "Install hint: $(package_install_hint 'openjdk-17')" >&2
    return 1
}

ngrok_config_path() {
    case "$(detect_os)" in
        mac) echo "$HOME/Library/Application Support/ngrok/ngrok.yml" ;;
        *)   echo "$HOME/.config/ngrok/ngrok.yml" ;;
    esac
}

package_install_hint() {
    local tool="$1"
    case "$(detect_os)" in
        mac)
            case "$tool" in
                openjdk-17) echo "brew install openjdk@17" ;;
                tmux|terraform|rclone|jq|redis|ngrok) echo "brew install $tool" ;;
                *) echo "brew install $tool" ;;
            esac
            ;;
        linux-debian)
            case "$tool" in
                openjdk-17) echo "sudo apt install openjdk-17-jdk" ;;
                terraform) echo "follow https://developer.hashicorp.com/terraform/install#linux for apt repo, then sudo apt install terraform" ;;
                ngrok) echo "follow https://ngrok.com/download for the .deb package" ;;
                redis) echo "sudo apt install redis-server" ;;
                *) echo "sudo apt install $tool" ;;
            esac
            ;;
        linux-rhel)
            case "$tool" in
                openjdk-17) echo "sudo dnf install java-17-openjdk-devel" ;;
                terraform) echo "sudo dnf config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo && sudo dnf install terraform" ;;
                ngrok) echo "follow https://ngrok.com/download for the .rpm package" ;;
                redis) echo "sudo dnf install redis" ;;
                *) echo "sudo dnf install $tool" ;;
            esac
            ;;
        *)
            echo "install $tool via your package manager"
            ;;
    esac
}

# Print the per-platform commands to system-trust a PEM cert. Caller substitutes
# the actual path. Linux: Firefox uses its own NSS store, import manually there
# if you use Firefox.
ca_trust_commands() {
    local pem="${1:-<path-to.pem>}"
    case "$(detect_os)" in
        mac)
            echo "sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $pem"
            ;;
        linux-debian)
            echo "sudo cp $pem /usr/local/share/ca-certificates/$(basename "$pem" .pem).crt"
            echo "sudo update-ca-certificates"
            ;;
        linux-rhel)
            echo "sudo cp $pem /etc/pki/ca-trust/source/anchors/$(basename "$pem")"
            echo "sudo update-ca-trust"
            ;;
        *)
            echo "# add $pem to the system trust store using your distribution's tooling"
            ;;
    esac
}
