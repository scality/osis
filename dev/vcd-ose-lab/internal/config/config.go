package config

import (
	"fmt"
	"os"
	"path/filepath"

	"gopkg.in/yaml.v3"
)

type Config struct {
	AWS       AWSConfig       `yaml:"aws"`
	VMware    VMwareConfig    `yaml:"vmware"`
	Endpoints EndpointsConfig `yaml:"endpoints"`
	Secrets   SecretsConfig   `yaml:"secrets"`
}

type AWSConfig struct {
	Profile          string   `yaml:"profile"`
	Region           string   `yaml:"region"`
	SubnetID         string   `yaml:"subnet_id"`
	SecurityGroupIDs []string `yaml:"security_group_ids"`
	KeyName          string   `yaml:"key_name"`
	KeyPath          string   `yaml:"key_path"`
	InstanceType     string   `yaml:"instance_type"`
	RootVolumeGB     int      `yaml:"root_volume_gb"`
	RootVolumeType   string   `yaml:"root_volume_type"`
	OS               string   `yaml:"os"`
}

type VMwareConfig struct {
	VCDBinGlob string `yaml:"vcd_bin_glob"`
	VCDBinMD5  string `yaml:"vcd_bin_md5"`
	OSERPMGlob string `yaml:"ose_rpm_glob"`
	OSERPMMD5  string `yaml:"ose_rpm_md5"`
}

type EndpointsConfig struct {
	OSISURL      string `yaml:"osis_url"`
	VaultURL     string `yaml:"vault_url"`
	S3URL        string `yaml:"s3_url"`
	PlatformName string `yaml:"platform_name"`
	Region       string `yaml:"region"`
}

type SecretsConfig struct {
	VCDAdminPassword        string `yaml:"vcd_admin_password"`
	PostgresPassword        string `yaml:"postgres_password"`
	OSISSuperAdminAccessKey string `yaml:"osis_super_admin_access_key"`
	OSISSuperAdminSecretKey string `yaml:"osis_super_admin_secret_key"`
	VCDCertPassphrase       string `yaml:"vcd_cert_passphrase"`
}

func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var c Config
	if err := yaml.Unmarshal(data, &c); err != nil {
		return nil, fmt.Errorf("parse %s: %w", path, err)
	}
	c.AWS.KeyPath = expandHome(c.AWS.KeyPath)
	return &c, nil
}

func expandHome(p string) string {
	if len(p) > 1 && p[:2] == "~/" {
		home, err := os.UserHomeDir()
		if err == nil {
			return filepath.Join(home, p[2:])
		}
	}
	return p
}

// MissingRequired returns a list of human-readable field names that are required but empty.
func (c *Config) MissingRequired() []string {
	var missing []string
	check := func(field, value string) {
		if value == "" {
			missing = append(missing, field)
		}
	}
	check("aws.profile", c.AWS.Profile)
	check("aws.region", c.AWS.Region)
	check("aws.subnet_id", c.AWS.SubnetID)
	if len(c.AWS.SecurityGroupIDs) == 0 {
		missing = append(missing, "aws.security_group_ids")
	}
	check("aws.key_name", c.AWS.KeyName)
	check("aws.key_path", c.AWS.KeyPath)
	check("endpoints.osis_url", c.Endpoints.OSISURL)
	check("endpoints.vault_url", c.Endpoints.VaultURL)
	check("endpoints.s3_url", c.Endpoints.S3URL)
	check("secrets.vcd_admin_password", c.Secrets.VCDAdminPassword)
	check("secrets.osis_super_admin_access_key", c.Secrets.OSISSuperAdminAccessKey)
	check("secrets.osis_super_admin_secret_key", c.Secrets.OSISSuperAdminSecretKey)
	return missing
}
