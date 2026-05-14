#!/bin/sh
awslocal s3 mb s3://clouddesk-company-assets || true
awslocal s3api put-bucket-policy --bucket clouddesk-company-assets --policy '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*","Action":"s3:GetObject","Resource":"arn:aws:s3:::clouddesk-company-assets/*"}]}'
