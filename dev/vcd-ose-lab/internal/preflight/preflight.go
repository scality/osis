package preflight

import (
	"crypto/md5"
	"encoding/hex"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/scality/osis/dev/vcd-ose-lab/internal/config"
)

type Check struct {
	Name   string
	OK     bool
	Detail string
	Action string
}

// Run executes every prerequisite check and returns them in order.
// repoRoot is the absolute path to dev/vcd-ose-lab.
func Run(repoRoot string) []Check {
	var checks []Check

	cfgPath := filepath.Join(repoRoot, "configs", "lab.yaml")
	if _, err := os.Stat(cfgPath); err != nil {
		return []Check{{
			Name:   "config file",
			OK:     false,
			Detail: "configs/lab.yaml not found",
			Action: "Copy configs/lab.example.yaml to configs/lab.yaml and fill in the required fields.",
		}}
	}

	cfg, err := config.Load(cfgPath)
	if err != nil {
		return []Check{{
			Name:   "config file",
			OK:     false,
			Detail: err.Error(),
			Action: "Fix the YAML syntax in configs/lab.yaml.",
		}}
	}
	checks = append(checks, Check{Name: "config file", OK: true})

	if missing := cfg.MissingRequired(); len(missing) > 0 {
		checks = append(checks, Check{
			Name:   "config fields",
			OK:     false,
			Detail: "missing: " + strings.Join(missing, ", "),
			Action: "Populate the listed fields in configs/lab.yaml.",
		})
	} else {
		checks = append(checks, Check{Name: "config fields", OK: true})
	}

	checks = append(checks, checkTool("terraform", "brew install terraform"))
	checks = append(checks, checkTool("aws", "brew install awscli"))
	checks = append(checks, checkTool("ssh", "ssh is required (preinstalled on macOS/Linux)"))
	checks = append(checks, checkTool("scp", "scp is required (preinstalled on macOS/Linux)"))

	checks = append(checks, checkAWSCreds(cfg.AWS.Region))

	checks = append(checks, checkSSHKey(cfg.AWS.KeyPath))

	binDir := filepath.Join(repoRoot, "binaries")
	checks = append(checks, checkBinary(binDir, cfg.VMware.VCDBinGlob, cfg.VMware.VCDBinMD5, "VCD installer (.bin)"))
	checks = append(checks, checkBinary(binDir, cfg.VMware.OSERPMGlob, cfg.VMware.OSERPMMD5, "OSE package (.rpm)"))

	return checks
}

func checkTool(name, action string) Check {
	if _, err := exec.LookPath(name); err != nil {
		return Check{
			Name:   name + " on PATH",
			OK:     false,
			Detail: "not found",
			Action: action,
		}
	}
	return Check{Name: name + " on PATH", OK: true}
}

func checkAWSCreds(region string) Check {
	if _, err := exec.LookPath("aws"); err != nil {
		return Check{Name: "AWS credentials", OK: false, Detail: "aws CLI not installed", Action: "brew install awscli, then aws sso login"}
	}
	cmd := exec.Command("aws", "sts", "get-caller-identity", "--region", region)
	if out, err := cmd.CombinedOutput(); err != nil {
		return Check{
			Name:   "AWS credentials",
			OK:     false,
			Detail: strings.TrimSpace(string(out)),
			Action: "Run `aws sso login` (or configure credentials another way) and retry.",
		}
	}
	return Check{Name: "AWS credentials", OK: true}
}

func checkSSHKey(path string) Check {
	if path == "" {
		return Check{Name: "SSH key", OK: false, Detail: "aws.key_path is empty", Action: "Set aws.key_path in configs/lab.yaml."}
	}
	info, err := os.Stat(path)
	if err != nil {
		return Check{Name: "SSH key", OK: false, Detail: err.Error(), Action: fmt.Sprintf("Place the EC2 key pair PEM at %s.", path)}
	}
	if info.Mode().Perm()&0o077 != 0 {
		return Check{
			Name:   "SSH key",
			OK:     false,
			Detail: fmt.Sprintf("%s has permissions %v (too open)", path, info.Mode().Perm()),
			Action: fmt.Sprintf("Run `chmod 600 %s`.", path),
		}
	}
	return Check{Name: "SSH key", OK: true}
}

func checkBinary(dir, glob, expectedMD5, label string) Check {
	if glob == "" {
		return Check{Name: label, OK: false, Detail: "glob not configured", Action: "Set vmware.* in configs/lab.yaml."}
	}
	matches, err := filepath.Glob(filepath.Join(dir, glob))
	if err != nil || len(matches) == 0 {
		return Check{
			Name:   label,
			OK:     false,
			Detail: fmt.Sprintf("no file matching %s in binaries/", glob),
			Action: fmt.Sprintf("Download the %s from Scality Drive (see binaries/README.md) and place it in dev/vcd-ose-lab/binaries/.", label),
		}
	}
	if len(matches) > 1 {
		return Check{Name: label, OK: false, Detail: "multiple matches: " + strings.Join(matches, ", "), Action: "Keep only one matching file in binaries/."}
	}
	if expectedMD5 != "" {
		sum, err := md5File(matches[0])
		if err != nil {
			return Check{Name: label, OK: false, Detail: err.Error(), Action: "Check file permissions on binaries/."}
		}
		if !strings.EqualFold(sum, expectedMD5) {
			return Check{
				Name:   label,
				OK:     false,
				Detail: fmt.Sprintf("md5 %s != expected %s", sum, expectedMD5),
				Action: "Re-download the binary or update the expected md5 in configs/lab.yaml.",
			}
		}
	}
	return Check{Name: label, OK: true, Detail: filepath.Base(matches[0])}
}

func md5File(path string) (string, error) {
	f, err := os.Open(path)
	if err != nil {
		return "", err
	}
	defer f.Close()
	h := md5.New()
	if _, err := io.Copy(h, f); err != nil {
		return "", err
	}
	return hex.EncodeToString(h.Sum(nil)), nil
}
