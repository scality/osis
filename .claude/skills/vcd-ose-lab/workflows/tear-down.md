<process>
## Step 1 — Confirm with the user

The lab is about to be **destroyed**. Quote the current `_capture/session-state.local.md` instance ID and cost-to-date if obvious, then ask: "Destroy AWS resources for VCD lab `i-XXXXX`?"

Wait for explicit yes.

## Step 2 — terraform destroy

```bash
cd dev/vcd-ose-lab
terraform -chdir=terraform destroy -auto-approve
```

Expected output ends with `Destroy complete! Resources: 1 destroyed.`

## Step 3 — Verify

```bash
terraform -chdir=terraform state list   # should be empty
```

Optionally, double-check via AWS:
```bash
aws ec2 describe-instances --profile sso --region eu-north-1 \
  --filters Name=tag:Project,Values=OSIS-VCD-Lab \
  --query 'Reservations[*].Instances[?State.Name!=`terminated`].[InstanceId,State.Name]' \
  --output table
```

Should show no non-terminated instances.

## Step 4 — Local cleanup (optional)

Ask the user if they want to also stop their local services:

```bash
tmux kill-session -t osis
tmux kill-session -t cloudserver
tmux kill-session -t vault
tmux kill-session -t ngrok
# Sentinels (daemonized, not in tmux):
pkill -f 'redis-sentinel /tmp/osis-local/sentinels'
```

And whether to remove `_capture/session-state.local.md` (gitignored so it doesn't matter for git, but stale info can mislead next time).

Do NOT touch `_capture/SHELL_HISTORY.md`, `aws-notes.md`, or `DOC_GAPS.md` — those are committed knowledge.

## Step 5 — Report

Print: "Lab `i-XXXXX` destroyed. Local services [stopped|left running]. Ready for next `spin-up`."
</process>

<success_criteria>
- `terraform state list` empty.
- AWS console / CLI confirms no non-terminated instances tagged `Project=OSIS-VCD-Lab`.
- User confirms they're done; no surprise bills.
</success_criteria>
