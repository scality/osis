#!/bin/bash
# Runs on the VM as root. The VCD installer skips creating the systemd unit
# on Rocky 9 because /sbin/chkconfig is absent. This script writes the unit,
# runs daemon-reload, and enables it. Assumes `initscripts` is already
# installed so the VCD init script can source /etc/init.d/functions.

set -euo pipefail

cat > /etc/systemd/system/vmware-vcd.service <<'EOF'
[Unit]
Description=VMware Cloud Director
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=forking
ExecStart=/opt/vmware/vcloud-director/bin/vmware-vcd start
ExecStop=/opt/vmware/vcloud-director/bin/vmware-vcd stop
ExecReload=/opt/vmware/vcloud-director/bin/vmware-vcd restart
RemainAfterExit=yes
TimeoutStartSec=600

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable vmware-vcd
echo 'vmware-vcd.service installed and enabled. Start with: systemctl start vmware-vcd'
