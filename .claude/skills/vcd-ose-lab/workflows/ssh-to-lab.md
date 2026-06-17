<process>
Read `instance private_ip` and `key_path` from `_capture/session-state.local.md`.

Print the user-facing command (do not execute — interactive shells can't be driven cleanly by the agent):

```bash
ssh -i <key_path> -t rocky@<private_ip> 'tmux attach -t lab'
```

If they want a fresh shell instead of the lab session:

```bash
ssh -i <key_path> rocky@<private_ip>
```

If they want a shared, recoverable session for new long-running commands and there's no `lab` tmux yet (or it was killed):

```bash
ssh -i <key_path> -t rocky@<private_ip> 'tmux new-session -A -s lab'
```
</process>

<success_criteria>
User has a copy-pasteable SSH command they can run. No tool calls modify the lab state.
</success_criteria>
