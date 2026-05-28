//go:build mage

package main

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/scality/osis/dev/vcd-ose-lab/internal/awsx"
	"github.com/scality/osis/dev/vcd-ose-lab/internal/config"
	"github.com/scality/osis/dev/vcd-ose-lab/internal/preflight"
)

func Preflight() error {
	wd, err := os.Getwd()
	if err != nil {
		return err
	}
	checks := preflight.Run(wd)
	failed := 0
	for _, c := range checks {
		mark := "[OK]"
		if !c.OK {
			mark = "[FAIL]"
			failed++
		}
		line := fmt.Sprintf("%s %s", mark, c.Name)
		if c.Detail != "" {
			line += "  (" + c.Detail + ")"
		}
		fmt.Println(line)
		if !c.OK && c.Action != "" {
			fmt.Println("       -> " + c.Action)
		}
	}
	if failed > 0 {
		return fmt.Errorf("%d preflight check(s) failed", failed)
	}
	return nil
}

// Tfvars renders configs/lab.yaml into terraform/terraform.tfvars.
func Tfvars() error {
	wd, err := os.Getwd()
	if err != nil {
		return err
	}
	cfg, err := config.Load(filepath.Join(wd, "configs", "lab.yaml"))
	if err != nil {
		return err
	}
	tfDir := filepath.Join(wd, "terraform")
	if err := awsx.WriteTfvars(tfDir, cfg); err != nil {
		return err
	}
	fmt.Println("wrote", filepath.Join(tfDir, "terraform.tfvars"))
	return nil
}

func Up() error {
	if err := Preflight(); err != nil {
		return err
	}
	return fmt.Errorf("up: not yet implemented (preflight passed)")
}

func Down() error {
	return fmt.Errorf("down: not yet implemented")
}

func Refresh() error {
	return fmt.Errorf("refresh: not yet implemented")
}

func Status() error {
	return fmt.Errorf("status: not yet implemented")
}

func SSH() error {
	return fmt.Errorf("ssh: not yet implemented")
}
