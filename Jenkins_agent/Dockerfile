FROM ubuntu:latest

# Set environment variables to avoid interactive installations
ENV DEBIAN_FRONTEND=noninteractive

# Update the repository sources to avoid using security.ubuntu.com
RUN sed -i 's|http://security.ubuntu.com/ubuntu|http://archive.ubuntu.com/ubuntu|g' /etc/apt/sources.list

# Install necessary packages
RUN apt-get update && \
    apt-get install -y \
    curl \
    unzip \
    gnupg \
    lsb-release \
    apt-transport-https \
    ca-certificates \
    software-properties-common

RUN apt install -y openjdk-17-jdk

# Install AWS CLI v2
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" && \
    unzip awscliv2.zip && \
    ./aws/install && \
    rm -rf awscliv2.zip aws

# Install Trivy
RUN curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh

# Add Docker’s official GPG key and setup the repository
RUN apt install docker.io -y
#RUN systemctl start docker

# Define ARG variables for AWS credentials
ARG ACCESS_KEY
ARG SECRET_ACCESS_KEY

# Create AWS configuration script
RUN echo '#!/bin/bash\n' > /usr/local/bin/aws_configure.sh && \
    echo 'aws configure set aws_access_key_id ${ACCESS_KEY}' >> /usr/local/bin/aws_configure.sh && \
    echo 'aws configure set aws_secret_access_key ${SECRET_ACCESS_KEY}' >> /usr/local/bin/aws_configure.sh && \
    echo 'aws configure set region ${AWS_DEFAULT_REGION}' >> /usr/local/bin/aws_configure.sh && \
    chmod +x /usr/local/bin/aws_configure.sh

# Entry point for the container
CMD ["/bin/bash"]

