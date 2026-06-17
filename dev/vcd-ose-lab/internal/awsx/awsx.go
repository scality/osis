package awsx

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/scality/osis/dev/vcd-ose-lab/internal/config"
)

// WriteTfvars generates terraform.tfvars from the lab config. The file
// is gitignored. Called before every apply/destroy so changes to lab.yaml
// flow through without manual sync.
//
// Terraform itself (init/plan/apply/destroy/output) is driven directly by
// the vcd-ose-lab skill workflows, not from here — see SKILL.md ("There is
// no mage up"). Mage's only jobs are preflight and rendering this file.
func WriteTfvars(tfDir string, cfg *config.Config) error {
	sgList := "["
	for i, id := range cfg.AWS.SecurityGroupIDs {
		if i > 0 {
			sgList += ", "
		}
		sgList += fmt.Sprintf("%q", id)
	}
	sgList += "]"

	rootVolumeType := cfg.AWS.RootVolumeType
	if rootVolumeType == "" {
		rootVolumeType = "gp2"
	}
	rootVolumeGB := cfg.AWS.RootVolumeGB
	if rootVolumeGB == 0 {
		rootVolumeGB = 400
	}
	instanceType := cfg.AWS.InstanceType
	if instanceType == "" {
		instanceType = "c5.4xlarge"
	}
	// aws.os selects the Rocky AMI generation; rocky-9 is the default.
	amiPattern := "Rocky-9-EC2-Base-*"
	if cfg.AWS.OS == "rocky-8" {
		amiPattern = "Rocky-8-EC2-Base-*"
	}

	content := fmt.Sprintf(`aws_profile            = %q
aws_region             = %q
subnet_id              = %q
security_group_ids     = %s
key_name               = %q
instance_type          = %q
root_volume_gb         = %d
root_volume_type       = %q
rocky_ami_name_pattern = %q
`,
		cfg.AWS.Profile,
		cfg.AWS.Region,
		cfg.AWS.SubnetID,
		sgList,
		cfg.AWS.KeyName,
		instanceType,
		rootVolumeGB,
		rootVolumeType,
		amiPattern,
	)
	return os.WriteFile(filepath.Join(tfDir, "terraform.tfvars"), []byte(content), 0o600)
}
