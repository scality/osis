output "instance_id" {
  value = aws_instance.lab.id
}

output "private_ip" {
  value = aws_instance.lab.private_ip
}

output "ami_id" {
  value = aws_instance.lab.ami
}
