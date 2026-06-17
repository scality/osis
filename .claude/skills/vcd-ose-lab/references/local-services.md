# Running Vault, Cloudserver, and OSIS locally

The VCD-OSE lab's OSE instance on EC2 needs to reach an OSIS, an S3, and a Vault somewhere. For development, the typical Scality engineer runs all three locally on their dev machine (Mac or Linux), then exposes them via ngrok so the EC2 VM can reach them.

This doc walks the user through standing those three up in separate tmux sessions so each service's logs stay visible and each can be restarted independently. Where commands differ across operating systems, both forms are shown side by side; otherwise everything is portable.

> Do **not** commit your local copy of this file with real credentials filled in. The version in the skill is the template; your working copy lives under `/tmp/osis-local/` (or wherever you choose).

## Prerequisites

- The three Scality repos cloned locally. Set `VAULT_REPO`, `CLOUDSERVER_REPO`, `OSIS_REPO` to where you cloned them; the snippets below use those variables. Example layout:
  - `$VAULT_REPO` → `~/scality/vault`
  - `$CLOUDSERVER_REPO` → `~/scality/cloudserver`
  - `$OSIS_REPO` → `~/scality/osis`
- Each repo's `yarn install` (or `./gradlew bootJar` for osis) has been run successfully at least once.
- `tmux`, `redis`, JDK 17, `ngrok`, `terraform`, `jq` installed.
  - **Mac:** `brew install tmux redis openjdk@17 ngrok terraform jq`
  - **Debian/Ubuntu:** `sudo apt install tmux redis-server openjdk-17-jdk jq`; follow https://developer.hashicorp.com/terraform/install#linux for the HashiCorp apt repo, then `sudo apt install terraform`; follow https://ngrok.com/download for the `.deb` package.
  - **Fedora/Rocky/RHEL:** `sudo dnf install tmux redis java-17-openjdk-devel jq`; `sudo dnf config-manager --add-repo https://rpm.releases.hashicorp.com/RHEL/hashicorp.repo && sudo dnf install terraform`; ngrok via https://ngrok.com/download `.rpm`.
- Redis available on `localhost:6379`, plus three Redis Sentinels on `26379/26380/26381` monitoring `mymaster` at `127.0.0.1:6379` (see `Sentinel setup` below). `scripts/local/setup-redis-sentinels.sh` handles this on any platform that has `redis-sentinel`.

## Start the three sessions

Run once at the start of each work session. The commands kill any stale tmux sessions of the same name and start fresh ones in detached mode, so they survive your terminal closing.

`JAVA_HOME` is auto-discovered via `scripts/local/lib/platform.sh::detect_java17_home`, which prefers `$JAVA_HOME` if you have one set, then falls back to Homebrew on Mac and `/usr/lib/jvm/java-17-*` on Linux.

```bash
. dev/vcd-ose-lab/scripts/local/lib/platform.sh

# Clean slate
tmux kill-session -t vault       2>/dev/null
tmux kill-session -t cloudserver 2>/dev/null
tmux kill-session -t osis        2>/dev/null
tmux kill-session -t ngrok       2>/dev/null

# Vault
tmux new-session -d -s vault       -x 200 -y 50
tmux send-keys -t vault \
  "cd $VAULT_REPO && VAULT_DB_BACKEND=LEVELDB yarn start" Enter

# Cloudserver (S3)
tmux new-session -d -s cloudserver -x 200 -y 50
tmux send-keys -t cloudserver \
  "cd $CLOUDSERVER_REPO && S3VAULT=scality REMOTE_MANAGEMENT_DISABLE=1 yarn start" Enter

# OSIS — resolve the built jar from the repo so the version (osisVersion in
# build.gradle, currently appended with -SNAPSHOT) never has to be hard-coded.
# Newest jar wins, so a rebuild after a version bump is picked up automatically.
OSIS_JAR="$(ls -t "$OSIS_REPO"/build/libs/osis-scality-*.jar | head -1)"
tmux new-session -d -s osis        -x 200 -y 50
tmux send-keys -t osis \
  ". $OSIS_REPO/dev/vcd-ose-lab/scripts/local/lib/platform.sh && \
   export JAVA_HOME=\$(detect_java17_home) && \
   export PATH=\$JAVA_HOME/bin:\$PATH && \
   cd /tmp/osis-local && \
   java -jar -Dspring.config.location=file:/tmp/osis-local/application.properties \
        $OSIS_JAR" Enter
```

(If `$OSIS_JAR` comes up empty, the jar hasn't been built yet — run `scripts/local/build-osis-jar.sh` first.)

## Attach / detach / list / kill

```bash
tmux ls                    # list sessions
tmux attach -t vault       # attach (Ctrl-b d to detach)
tmux attach -t cloudserver
tmux attach -t osis
tmux kill-session -t osis  # stop one
```

`Ctrl-b d` detaches without killing. The process keeps running. Re-attach any time.

## Verify each service is up

```bash
# Vault admin
curl -s http://localhost:8600/_/healthcheck

# Cloudserver (returns 405 for GET / which is expected)
curl -sI http://localhost:8000/ | head -1

# OSIS
curl -s http://localhost:9333/_/healthcheck | python3 -m json.tool
curl -s http://localhost:9333/api/info     | python3 -m json.tool
```

A healthy OSIS healthcheck shows all components `UP`:

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping":      { "status": "UP" },
    "redis":     { "status": "UP" },
    "s3":        { "status": "UP" },
    "vault":     { "status": "UP" }
  }
}
```

## Sentinel setup (one-time)

OSIS hardcodes Redis Sentinel mode. Run `scripts/local/setup-redis-sentinels.sh` once and the sentinels survive across runs (until a reboot of your dev machine). The script writes per-sentinel configs under `/tmp/osis-local/sentinels/` and daemonises `redis-sentinel` on 26379/26380/26381.

If you'd rather do it by hand:

```bash
mkdir -p /tmp/osis-local/sentinels
for p in 26379 26380 26381; do
  cat > /tmp/osis-local/sentinels/sentinel-$p.conf <<EOF
port $p
dir /tmp/osis-local/sentinels
pidfile /tmp/osis-local/sentinels/sentinel-$p.pid
logfile /tmp/osis-local/sentinels/sentinel-$p.log
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
EOF
  redis-sentinel /tmp/osis-local/sentinels/sentinel-$p.conf --daemonize yes
done
for p in 26379 26380 26381; do echo -n "sentinel $p: "; redis-cli -p $p ping; done
```

## OSIS local config

The OSIS jar reads its config from `/tmp/osis-local/application.properties`. A working dev copy with these overrides:

- `osis.scality.vault.decrypt-admin-credentials=false` (use the plain access/secret keys, not the encrypted-on-disk variant).
- `osis.scality.vault.access-key=<your-vault-dev-access-key>` and `secret-key=<your-vault-dev-secret-key>` — the values that match whatever the Vault you're running recognises as super-admin. For a fresh `VAULT_DB_BACKEND=LEVELDB` Vault these are documented in the Vault repo's dev README; do not paste them into this file.
- `server.port=9333`, `server.ssl.enabled=false`.
- `spring.redis.sentinel.master=mymaster`, `spring.redis.sentinel.nodes=localhost:26379,localhost:26380,localhost:26381`.
- `osis.scality.s3.capabilities-file-path=file:$OSIS_REPO/src/main/resources/s3capabilities.json`.
- `osis.api.version=1.0.0` and `osis.scality.redis.credentials.hashKey=osis:s3credentials` (these are required even if you don't think you need them — Spring fails fast on missing placeholders).

`crypto.yml` is hardcoded to `file:/conf/crypto.yml` (see `CryptoEnv.java`). Place the file once on your dev box:

```bash
sudo mkdir -p /conf
sudo cp /tmp/osis-local/crypto.yml /conf/crypto.yml
sudo chmod 644 /conf/crypto.yml
```

Build the jar with `scripts/local/build-osis-jar.sh` (auto-discovers Java 17 on any platform). If you'd rather invoke gradle yourself:

```bash
. $OSIS_REPO/dev/vcd-ose-lab/scripts/local/lib/platform.sh
export JAVA_HOME="$(detect_java17_home)"
cd "$OSIS_REPO"
./gradlew bootJar -x test -PsonatypeUsername=x -PsonatypePassword=x
```

## Expose to the EC2 lab via ngrok

Once all three healthchecks are green, expose OSIS and S3 publicly so the OSE service on the EC2 VM can reach them. Vault stays local — only OSIS calls Vault.

The ngrok config path differs per OS. `scripts/local/lib/platform.sh::ngrok_config_path` returns the right one:

- **Mac:** `~/Library/Application Support/ngrok/ngrok.yml`
- **Linux:** `~/.config/ngrok/ngrok.yml`

```yaml
# in the path above
version: "3"
agent:
  authtoken: <your-token>
tunnels:
  osis:
    proto: http
    addr: 9333
  s3:
    proto: http
    addr: 8000
```

Run it in a tmux session named `ngrok` — the spin-up/status/tear-down workflows all expect that session to exist alongside `vault`, `cloudserver`, and `osis`:

```bash
tmux kill-session -t ngrok 2>/dev/null
tmux new-session -d -s ngrok -x 200 -y 50
tmux send-keys -t ngrok "ngrok start --all" Enter
```

Copy the two public URLs that ngrok prints into `dev/vcd-ose-lab/configs/lab.yaml`:

```yaml
endpoints:
  osis_url: https://<random>.ngrok-free.app
  s3_url:   https://<random>.ngrok-free.app
```

Then run the `refresh-endpoints` workflow to re-wire OSE's OSIS adapter against the current URLs.

## Common gotchas

- `lsof -iTCP:8000,8500,8600,9333 -sTCP:LISTEN` shows nothing → one of the three tmux services died. Attach and read the logs.
- `osis-scality-*.jar` not found → run `scripts/local/build-osis-jar.sh`; output lands in `build/libs/`.
- OSIS log `Cannot invoke "...RedisProperties$Sentinel.getMaster()" because ... null` → you launched OSIS before the sentinels were running. Start sentinels, retry.
- OSIS log `Could not resolve placeholder 'osis.scality.utapi.endpoint'` → missing line in `application.properties`; copy from this doc.
- Cloudserver fails with `EADDRINUSE` on 8000 → kill the stale node process (`lsof -iTCP:8000 -sTCP:LISTEN`, then `kill <pid>`). The Docker proxy on 8000 from a `kind` cluster does NOT conflict; it maps to a container.
