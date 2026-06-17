terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile
}

data "aws_ami" "rocky" {
  most_recent = true
  owners      = [var.rocky_ami_owner]
  filter {
    name   = "name"
    values = [var.rocky_ami_name_pattern]
  }
  filter {
    name   = "architecture"
    values = ["x86_64"]
  }
  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "lab" {
  ami                         = var.ami_id_override != "" ? var.ami_id_override : data.aws_ami.rocky.id
  instance_type               = var.instance_type
  key_name                    = var.key_name
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = var.security_group_ids
  associate_public_ip_address = false

  root_block_device {
    volume_size           = var.root_volume_gb
    volume_type           = var.root_volume_type
    encrypted             = true
    delete_on_termination = true
  }

  # IMDSv2 only — IMDSv1 is vulnerable to SSRF-based credential theft.
  metadata_options {
    http_tokens                 = "required"
    http_endpoint               = "enabled"
    http_put_response_hop_limit = 1
  }

  tags = {
    Name    = "vcd-ose-lab"
    Project = "OSIS-VCD-Lab"
  }
}
