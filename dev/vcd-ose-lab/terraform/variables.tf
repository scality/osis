variable "aws_profile" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "security_group_id" {
  type = string
}

variable "key_name" {
  type = string
}

variable "instance_type" {
  type    = string
  default = "c5.4xlarge"
}

variable "root_volume_gb" {
  type    = number
  default = 150
}

# Rocky AMI lookup. Defaults target Rocky 9 community AMIs published by
# Rocky Enterprise Software Foundation. Override ami_id_override during the
# ride-along once a working AMI is confirmed for the chosen region.
variable "rocky_ami_owner" {
  type    = string
  default = "792107900819"
}

variable "rocky_ami_name_pattern" {
  type    = string
  default = "Rocky-9-EC2-Base-*"
}

variable "ami_id_override" {
  type    = string
  default = ""
}
