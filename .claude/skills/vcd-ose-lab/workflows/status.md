<process>
Read `instance private_ip` from `_capture/session-state.local.md` (or `terraform -chdir=terraform output -json`). If neither yields a value, tell the user "no lab is currently provisioned" and stop.

## On the VM

```bash
ssh -q -i <key> rocky@<ip> 'tmux send-keys -t lab "ose config validate && systemctl is-active postgresql vmware-vcd voss-keeper" Enter'
sleep 4
ssh -q -i <key> rocky@<ip> 'tmux capture-pane -t lab -p -S -80' | tail -30
```

Expected: seven `Valid` rows from `ose config validate`, three `active` lines from `systemctl is-active`.

Also peek at the VCD cell health:
```bash
ssh -q -i <key> rocky@<ip> 'sudo tail -5 /opt/vmware/vcloud-director/logs/cell.log'
```

The last cell startup line should say `Cell startup completed in ...`.

## On the Mac

Verify local services + ngrok still up:

```bash
for s in vault cloudserver osis ngrok; do
  tmux has-session -t $s 2>/dev/null && echo "$s: tmux present" || echo "$s: MISSING"
done

curl -sf http://localhost:9333/_/healthcheck | python3 -m json.tool

curl -s http://127.0.0.1:4040/api/tunnels | python3 -c \
  'import sys,json; d=json.load(sys.stdin); [print(t["name"], t["public_url"]) for t in d["tunnels"]]'
```

Verify the ngrok URLs in the live API match what's in `configs/lab.yaml`. If they differ, the user needs to run the `refresh-endpoints` workflow.

## Report

Render a compact table:

```
Lab: i-XXXXX (10.160.96.15 / FQDN)
ose config validate : all VALID  | (or list failing rows)
postgresql          : active
vmware-vcd          : active
voss-keeper         : active
Local osis health   : UP (vault, redis, s3, ping all UP)
Local services      : vault ✓  cloudserver ✓  osis ✓  ngrok ✓
ngrok URLs match    : yes / NO — run refresh-endpoints
```
</process>

<success_criteria>
User has a clear picture of which component, if any, is degraded, and the next workflow to run if so.
</success_criteria>
