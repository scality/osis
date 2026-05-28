package awsx

import (
	"encoding/json"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/scality/osis/dev/vcd-ose-lab/internal/config"
)

type Outputs struct {
	InstanceID string `json:"instance_id"`
	PrivateIP  string `json:"private_ip"`
	AMIID      string `json:"ami_id"`
}

// WriteTfvars generates terraform.tfvars from the lab config. The file
// is gitignored. Called before every apply/destroy so changes to lab.yaml
// flow through without manual sync.
func WriteTfvars(tfDir string, cfg *config.Config) error {
	content := fmt.Sprintf(`aws_region        = %q
subnet_id         = %q
security_group_id = %q
key_name          = %q
instance_type     = %q
root_volume_gb    = %d
`,
		cfg.AWS.Region,
		cfg.AWS.SubnetID,
		cfg.AWS.SecurityGroupID,
		cfg.AWS.KeyName,
		cfg.AWS.InstanceType,
		cfg.AWS.RootVolumeGB,
	)
	return os.WriteFile(filepath.Join(tfDir, "terraform.tfvars"), []byte(content), 0o600)
}

func Init(tfDir string) error {
	return runTF(tfDir, "init", "-input=false")
}

func Apply(tfDir string) error {
	return runTF(tfDir, "apply", "-auto-approve", "-input=false")
}

func Destroy(tfDir string) error {
	return runTF(tfDir, "destroy", "-auto-approve", "-input=false")
}

func ReadOutputs(tfDir string) (*Outputs, error) {
	cmd := exec.Command("terraform", "-chdir="+tfDir, "output", "-json")
	out, err := cmd.Output()
	if err != nil {
		return nil, fmt.Errorf("terraform output: %w", err)
	}
	var raw map[string]struct {
		Value interface{} `json:"value"`
	}
	if err := json.Unmarshal(out, &raw); err != nil {
		return nil, err
	}
	str := func(k string) string {
		if v, ok := raw[k]; ok {
			if s, ok := v.Value.(string); ok {
				return s
			}
		}
		return ""
	}
	return &Outputs{
		InstanceID: str("instance_id"),
		PrivateIP:  str("private_ip"),
		AMIID:      str("ami_id"),
	}, nil
}

func runTF(tfDir string, args ...string) error {
	full := append([]string{"-chdir=" + tfDir}, args...)
	cmd := exec.Command("terraform", full...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}
