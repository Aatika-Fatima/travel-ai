#!/usr/bin/env bash
# One-time setup for the GitHub Actions -> AWS OIDC -> SSM deploy path (docs/amazon.html P6).
# Run LOCALLY with the AWS CLI configured for account 638071549119 (see P4 substep 0).
# Idempotent-ish: safe to re-run; steps that already exist are skipped or error harmlessly.
set -euo pipefail

ACCOUNT_ID=638071549119
REGION=ap-south-1
INSTANCE_ID=i-0325ae90b67963722
REPO=Aatika-Fatima/travel-ai
HERE="$(cd "$(dirname "$0")" && pwd)"

echo "== 1. GitHub OIDC identity provider =="
if ! aws iam get-open-id-connect-provider \
      --open-id-connect-provider-arn "arn:aws:iam::${ACCOUNT_ID}:oidc-provider/token.actions.githubusercontent.com" >/dev/null 2>&1; then
  aws iam create-open-id-connect-provider \
    --url https://token.actions.githubusercontent.com \
    --client-id-list sts.amazonaws.com
else
  echo "   already exists"
fi

echo "== 2. Instance role so SSM Agent can register (AmazonSSMManagedInstanceCore) =="
aws iam create-role --role-name ota-ssm-instance-role \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' \
  2>/dev/null || echo "   role exists"
aws iam attach-role-policy --role-name ota-ssm-instance-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam create-instance-profile --instance-profile-name ota-ssm-instance-profile 2>/dev/null || echo "   profile exists"
aws iam add-role-to-instance-profile --instance-profile-name ota-ssm-instance-profile \
  --role-name ota-ssm-instance-role 2>/dev/null || echo "   role already in profile"
# give IAM a moment to propagate before the association
sleep 10
aws ec2 associate-iam-instance-profile --region "$REGION" \
  --instance-id "$INSTANCE_ID" \
  --iam-instance-profile Name=ota-ssm-instance-profile \
  2>/dev/null || echo "   instance already has a profile"

echo "== 3. GitHub Actions deploy role (OIDC trust, SSM send-command only) =="
aws iam create-role --role-name github-actions-travel-ai-deploy \
  --assume-role-policy-document "file://${HERE}/github-actions-trust-policy.json" \
  2>/dev/null || \
aws iam update-assume-role-policy --role-name github-actions-travel-ai-deploy \
  --policy-document "file://${HERE}/github-actions-trust-policy.json"
aws iam put-role-policy --role-name github-actions-travel-ai-deploy \
  --policy-name ssm-deploy --policy-document "file://${HERE}/github-actions-deploy-policy.json"

ROLE_ARN=$(aws iam get-role --role-name github-actions-travel-ai-deploy --query Role.Arn --output text)

echo
echo "== 4. wait for the instance to show up as an SSM Managed Instance =="
for i in $(seq 1 30); do
  PING=$(aws ssm describe-instance-information --region "$REGION" \
    --filters "Key=InstanceIds,Values=${INSTANCE_ID}" \
    --query "InstanceInformationList[0].PingStatus" --output text 2>/dev/null || echo None)
  [ "$PING" = "Online" ] && { echo "   SSM: Online"; break; }
  echo "   SSM ping: $PING (waiting...)"
  sleep 10
done

echo
echo "== 5. GitHub repo secrets to set =="
echo "   gh secret set AWS_DEPLOY_ROLE_ARN --body \"${ROLE_ARN}\" -R ${REPO}"
echo "   gh secret set EC2_INSTANCE_ID     --body \"${INSTANCE_ID}\" -R ${REPO}"
echo
echo "Done. Neither secret is sensitive (role ARN + instance id); no private key leaves your machine."
