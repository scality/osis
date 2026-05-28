# Running Vault, Cloudserver, and OSIS locally on your Mac

The VCD-OSE lab's OSE instance on EC2 needs to reach an OSIS, an S3, and a Vault somewhere. For development, the typical Scality engineer runs all three locally on their Mac, then exposes them via ngrok so the EC2 VM can reach them.

This doc walks the user through standing those three up in separate tmux sessions so each service's logs stay visible and each can be restarted independently.

> Do **not** commit your local copy of this file with real credentials filled in. The version in the skill is the template; your working copy lives under `/tmp/osis-local/` (or wherever you choose).

## Prerequisites

- `tmux` installed (`brew install tmux`).
- The three Scality repos cloned locally:
  - `~/anurag-builds-things/scality/vault`
  - `~/anurag-builds-things/scality/cloudserver`
  - `~/anurag-builds-things/scality/osis`
- Each repo's `yarn install` (or `./gradlew bootJar` for osis) has been run successfully at least once.
- Java 17 available (e.g., `brew install openjdk@17`).
- Redis available on `localhost:6379`, plus three Redis Sentinels on `26379/26380/26381` monitoring `mymaster` at `127.0.0.1:6379` (see `Sentinel setup` below).

## Start the three sessions

Run once at the start of each work session. The commands kill any stale tmux sessions of the same name and start fresh ones in detached mode, so they survive your terminal closing.

```bash
# Clean slate
tmux kill-session -t vault       2>/dev/null
tmux kill-session -t cloudserver 2>/dev/null
tmux kill-session -t osis        2>/dev/null

# Vault
tmux new-session -d -s vault       -x 200 -y 50
tmux send-keys -t vault \
  'cd ~/anurag-builds-things/scality/vault && VAULT_DB_BACKEND=LEVELDB yarn start' Enter

# Cloudserver (S3)
tmux new-session -d -s cloudserver -x 200 -y 50
tmux send-keys -t cloudserver \
  'cd ~/anurag-builds-things/scality/cloudserver && S3VAULT=scality REMOTE_MANAGEMENT_DISABLE=1 yarn start' Enter

# OSIS
tmux new-session -d -s osis        -x 200 -y 50
tmux send-keys -t osis \
  'export JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home && \
   export PATH=$JAVA_HOME/bin:$PATH && \
   cd /tmp/osis-local && \
   java -jar -Dspring.config.location=file:/tmp/osis-local/application.properties \
        ~/anurag-builds-things/scality/osis/build/libs/osis-scality-2.2.5-SNAPSHOT.jar' Enter
```

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

OSIS hardcodes Redis Sentinel mode. Start three sentinels once and they survive across runs (until your Mac reboots):

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
- `osis.scality.vault.access-key=D4IT2AWSB588GO5J9T00` and `secret-key=UEEu8tYlsOGGrgf4DAiSZD6apVNPUWqRiPG0nTB6` (the sample super-admin credentials baked into the Vault dev DB).
- `server.port=9333`, `server.ssl.enabled=false`.
- `spring.redis.sentinel.master=mymaster`, `spring.redis.sentinel.nodes=localhost:26379,localhost:26380,localhost:26381`.
- `osis.scality.s3.capabilities-file-path=file:<absolute path to osis>/src/main/resources/s3capabilities.json`.
- `osis.api.version=1.0.0` and `osis.scality.redis.credentials.hashKey=osis:s3credentials` (these are required even if you don't think you need them — Spring fails fast on missing placeholders).

`crypto.yml` is **hardcoded** to `/conf/crypto.yml` (see `CryptoEnv.java:30`). The `osis.security.config.path` property is not honored. Without this file present, every S3-credential operation throws `Cannot invoke "java.util.List.stream()" because the return value of "CryptoEnv.getKeys()" is null`. One-time setup on the Mac:

```bash
sudo mkdir -p /conf
sudo cp /tmp/osis-local/crypto.yml /conf/crypto.yml
sudo chmod 644 /conf/crypto.yml
```

Build the jar once with Java 17:

```bash
export JAVA_HOME=$(/opt/homebrew/bin/brew --prefix openjdk@17)/libexec/openjdk.jdk/Contents/Home
cd ~/anurag-builds-things/scality/osis
./gradlew bootJar -x test -PsonatypeUsername=x -PsonatypePassword=x
```

## Expose to the EC2 lab via ngrok

Once all three healthchecks are green, expose OSIS and S3 publicly so the OSE service on the EC2 VM can reach them. Vault stays local — only OSIS calls Vault.

```yaml
# ~/.config/ngrok/ngrok.yml
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

```bash
ngrok start --all   # in its own terminal; leave running
```

Copy the two public URLs that ngrok prints into `dev/vcd-ose-lab/configs/lab.yaml`:

```yaml
endpoints:
  osis_url: https://<random>.ngrok-free.app
  s3_url:   https://<random>.ngrok-free.app
```

Then run `mage refresh` against the lab to re-wire OSE's OSIS adapter against the current URLs.

## Common gotchas

- `lsof -iTCP:8000,8500,8600,9333 -sTCP:LISTEN` shows nothing → one of the three tmux services died. Attach and read the logs.
- `osis-scality-*.jar` not found → run the gradle build above; output lands in `build/libs/`.
- OSIS log `Cannot invoke "...RedisProperties$Sentinel.getMaster()" because ... null` → you launched OSIS before the sentinels were running. Start sentinels, retry.
- OSIS log `Could not resolve placeholder 'osis.scality.utapi.endpoint'` → missing line in `application.properties`; copy from this doc.
- Cloudserver fails with `EADDRINUSE` on 8000 → kill the stale node process (`lsof -iTCP:8000 -sTCP:LISTEN`, then `kill <pid>`). The Docker proxy on 8000 from a `kind` cluster does NOT conflict; it maps to a container.
