# Capture

Local-only scratch space for ride-along artifacts from manual lab runs. Everything in this directory except this README is gitignored — nothing here gets committed, so raw notes with instance IDs, IPs, and timings are fine to keep.

Typical contents:

- `session-state.local.md` — live lab state (instance ID, IPs, ngrok URLs); written by the `spin-up` workflow, updated by `refresh-endpoints`.
- `SHELL_HISTORY.md` — every command run during a manual ride-along, in order, with outputs and timings.
- `aws-notes.md` — AMI IDs, region quirks, instance behavior.

Durable knowledge does not live here. When a run surfaces a new gotcha or doc gap, fold it into `.claude/skills/vcd-ose-lab/references/install-sequence.md` or `references/troubleshooting.md` — those are the committed ground truth.
