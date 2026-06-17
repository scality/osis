<required_reading>
- `references/local-services.md` — to confirm local OSIS + S3 are alive at the new ngrok URLs.
- `references/troubleshooting.md` — only if `ose config validate` fails.
</required_reading>

<when>
ngrok restarted and the OSIS / S3 public URLs changed. The lab's VCD and OSE are unchanged, but OSE's OSIS-adapter config now points at stale tunnels.
</when>

<process>
## Step 1 — Capture the new URLs

```bash
curl -s http://127.0.0.1:4040/api/tunnels | python3 -c \
  'import sys,json; d=json.load(sys.stdin); [print(t["name"], t["public_url"]) for t in d["tunnels"]]'
```

Or ask the user, if they prefer.

## Step 2 — Add the S3 ngrok hostname to cloudserver's restEndpoints

Open `$CLOUDSERVER_REPO/config.json` (your local cloudserver checkout), find `restEndpoints`, add the new S3 ngrok hostname mapped to `us-east-1`. Restart cloudserver in its tmux:

```bash
tmux send-keys -t cloudserver C-c
sleep 2
tmux send-keys -t cloudserver 'S3VAULT=scality REMOTE_MANAGEMENT_DISABLE=1 yarn start' Enter
```

(If the S3 hostname is missing, cloudserver returns `Could not parse the specified URI. Check your restEndpoints configuration.` This is doc gap #N/A — local-dev specific, captured here.)

## Step 3 — Update `configs/lab.yaml`

Edit `dev/vcd-ose-lab/configs/lab.yaml` `endpoints.osis_url` and `endpoints.s3_url` to the new values. Update `_capture/session-state.local.md` too.

## Step 4 — Re-wire OSE on the VM

Read `instance private_ip` from `_capture/session-state.local.md` (or `terraform -chdir=terraform output -json`).

SCP and run `scripts/vm/wire-osis.sh`. Run it over plain SSH rather than `tmux send-keys` (the tmux scroll buffer and `pipe-pane` log would capture the secret), and feed the secret over stdin so it never appears in the remote process listing — only the non-secret values go on the command line:

```bash
scp -i <key> dev/vcd-ose-lab/scripts/vm/wire-osis.sh rocky@<ip>:/tmp/
ssh -i <key> rocky@<ip> 'IFS= read -r OSIS_SECRET_KEY; export OSIS_SECRET_KEY OSIS_URL="<osis_url>" S3_URL="<s3_url>" OSIS_ACCESS_KEY="<access_key>"; sudo -E bash /tmp/wire-osis.sh' <<< '<secret_key>'
```

(`wire-osis.sh` reads the secret from the env and feeds it to the `ose` prompt via stdin itself.)

The script sends, in order:
1. `ose osis admin set --name scality --url $OSIS_URL --user $OSIS_ACCESS_KEY` (interactive: provides secret + `y` for complexity warning)
2. `ose osis s3 set --name scality --url $S3_URL`
3. `ose platforms enable osis --name scality`
4. `ose service restart`

## Step 5 — Validate

```bash
ssh -i <key> rocky@<ip> 'tmux send-keys -t lab "ose config validate" Enter'
sleep 4
ssh -i <key> rocky@<ip> 'tmux capture-pane -t lab -p -S -60' | tail -30
```

All seven rows should be `Valid`. If any are not, jump to `references/troubleshooting.md`.
</process>

<success_criteria>
- `ose config validate` all rows `Valid`.
- `_capture/session-state.local.md` updated with new URLs.
- VCD UI's Object Storage panel still loads, S3 credential operations still succeed.
</success_criteria>
