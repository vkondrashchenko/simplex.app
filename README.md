# SYMPLEX

Web application for building simplex-lattice composition-property diagrams. Evaluates polynomial mixture models on a 2D equilateral-triangle simplex and renders the result as an SVG diagram.

**Stack:** Quarkus 3.17 · Java 21 · AWS Lambda + API Gateway · React 18 · Vite 5 · AWS SAM

---

## Repository layout

```
new.java.version/
├── backend/
│   ├── pom.xml                     Maven parent (Java 21, Quarkus BOM 3.17.4)
│   ├── core/                       Pure-Java library — parser, evaluator, renderer
│   └── lambda/                     Quarkus Lambda HTTP handler
├── frontend/                       React + Vite SPA
└── deploy/
    ├── Dockerfile                  Multi-stage image for ECR/Lambda
    ├── template.yaml               AWS SAM template (arm64/Graviton2)
    ├── samconfig.toml              SAM profiles (local + prod)
    ├── deploy.sh                   Build + deploy wrapper (reads .env.deploy)
    └── .env.deploy.example         Template for the gitignored .env.deploy file
```

---

## Prerequisites

| Tool | Version | Required for |
|---|---|---|
| Java (JDK) | 21+ | Backend (always); `JAVA_HOME` must be set |
| Maven | 3.9+ | Backend (always) |
| Node.js | 20+ | Frontend (always) |
| Docker | 24+ | SAM Local and prod image build only |
| AWS SAM CLI | 1.120+ | SAM Local and prod deploy only (`brew install aws-sam-cli`) |
| AWS CLI | 2.x | Prod deploy only |

---

## Running locally

There are two ways to run the backend locally. **Option A is recommended** for day-to-day development — it requires only Java and Maven, no Docker.

---

### Option A — Quarkus dev mode (recommended, no Docker required)

Quarkus dev mode starts the HTTP server directly on your machine with live reload.

#### 1 — Start the backend

```bash
cd new.java.version/backend/lambda
mvn quarkus:dev
```

The server starts on **port 8080**. You should see:

```
Listening on: http://0.0.0.0:8080
```

Verify it is up:

```bash
curl http://localhost:8080/api/health
# → {"status":"UP"}
```

#### 2 — Start the frontend

```bash
cd new.java.version/frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies `/api` to `http://localhost:8080` by default.

---

### Option B — SAM Local (Docker required, closer to Lambda runtime)

SAM Local runs the function inside a container that matches the production Lambda environment. Use this to test Lambda-specific behaviour or to verify the Docker image before deploying.

**Requires Docker Desktop (or Finch) to be running.**

#### 1 — Build the local container image

```bash
cd new.java.version/deploy
sam build
```

`sam build` reads the `Metadata` block in `template.yaml` to locate the Dockerfile and build a local image. The Maven compile happens inside the container — first build takes ~3 minutes; subsequent builds use Docker layer caching and finish in ~30 seconds.

#### 2 — Start the API

```bash
sam local start-api
```

The API is available on **port 3000**.

```bash
curl http://localhost:3000/api/health
# → {"status":"UP"}
```

#### 3 — Start the frontend pointed at port 3000

```bash
cd new.java.version/frontend
npm install
VITE_BACKEND_URL=http://localhost:3000 npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

---

### Acceptance test

Use the control example from the PDF specification:

**Equation (ALPHA.TXT):**
```
Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4
```

**Steps in the UI:**
1. Paste the equation into the *Equation* panel and click **Add**.
2. In the *Variables* panel, tick **fix** for `x1` and set its value to `0`.
3. In *Render settings*, choose **Isoline range**, set From=25, To=65, Step=5, Precision=1, Pixel step=1.
4. Click **Calculate & Render**.

**Expected result:** the diagram shows 9 isolines (25, 30, 35, … 65) matching the reference figure in the PDF specification.

You can also call the API directly and save the SVG (substitute port 8080 if using Option A, 3000 if using Option B):

```bash
curl -s -X POST http://localhost:8080/api/render \
  -H 'Content-Type: application/json' \
  -d '{
    "equation": "Y=5.76*x1+7.20*x2+13.36*x3+68.16*x4-3.66*x1*x2+32.13*x1*x3+62.8*x1*x4+42.80*x2*x3+56.86*x2*x4+58.65*x3*x4",
    "fixedVars": {"1": 0},
    "mode": "ISOLINE_RANGE",
    "isoFrom": 25, "isoTo": 65, "isoStep": 5,
    "precision": 1,
    "pixelStep": 1
  }' | python3 -c "
import json, sys
r = json.load(sys.stdin)
print(f'min={r[\"min\"]:.4f}  max={r[\"max\"]:.4f}')
open('/tmp/symplex.svg', 'w').write(r['svgContent'])
print('Saved /tmp/symplex.svg')
"
```

### Run the unit tests

```bash
cd new.java.version/backend
mvn test -pl core
# 36 tests, 0 failures
```

---

## API reference

### `POST /api/parse`

Parses the equation and returns variable metadata. Use this to populate the variable list before rendering.

**Request:**
```json
{ "equation": "Y=5.76*x1+7.20*x2+13.36*x3" }
```

**Response:**
```json
{
  "varCount": 3,
  "monomialCount": 3,
  "variableNames": ["x1", "x2", "x3"]
}
```

---

### `POST /api/render`

Sweeps the simplex triangle, evaluates the polynomial at each pixel, and returns a 457×457 SVG diagram.

**Request fields:**

| Field | Type | Description |
|---|---|---|
| `equation` | string | Polynomial (e.g. `Y=5.76*x1+7.20*x2`) |
| `fixedVars` | `{int: double}` | 1-based variable index → fixed value; omitted variables are free |
| `mode` | enum | `REGION`, `ISOLINE_SINGLE`, or `ISOLINE_RANGE` |
| `regionMin` / `regionMax` | double | Bounds for `REGION` mode |
| `isoValue` | double | Target value for `ISOLINE_SINGLE` mode |
| `isoFrom` / `isoTo` / `isoStep` | double | Range for `ISOLINE_RANGE` mode |
| `precision` | int | Stroke width control (0–3): 0 = coarsest/thickest, 3 = finest/thinnest |
| `pixelStep` | int | Sweep stride — 1 = every pixel (full res), 10 = fastest/coarsest |

Exactly 3 variables must be left unfixed (free); they map to the three simplex axes.

**Response:**
```json
{
  "svgContent": "<svg xmlns=...>...</svg>",
  "min": 5.76,
  "max": 68.16
}
```

---

### `GET /api/health`

Returns `{"status":"UP"}` with HTTP 200. Used by load balancers and smoke tests.

---

## Deploying to AWS

### One-time setup

#### 1 — Configure AWS credentials

```bash
aws configure
# Enter: Access Key ID, Secret Access Key, default region (e.g. us-east-1), output format (json)
```

#### 2 — Create an ECR repository

```bash
aws ecr create-repository --repository-name symplex --region us-east-1
```

Note the `repositoryUri` from the output — you will use it in the next step.

#### 3 — Create the Lambda execution role

The deploy user must not have `iam:CreateRole`. Create the role once from an admin identity, then the deploy user only needs `iam:PassRole` scoped to this role.

```bash
# Create role with Lambda trust policy
aws iam create-role \
  --role-name symplex-lambda-role \
  --assume-role-policy-document '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"lambda.amazonaws.com"},"Action":"sts:AssumeRole"}]}'

# Attach basic execution policy (CloudWatch Logs write access)
aws iam attach-role-policy \
  --role-name symplex-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

Then grant the deploy user permission to pass this role to Lambda:

```bash
aws iam put-user-policy \
  --user-name <deploy-user> \
  --policy-name symplex-pass-lambda-role \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": "arn:aws:iam::<account-id>:role/symplex-lambda-role",
      "Condition": {"StringEquals": {"iam:PassedToService": "lambda.amazonaws.com"}}
    }]
  }'
```

#### 4 — Configure the deploy environment file

Copy the example file and fill in your ECR repository URI:

```bash
cp deploy/.env.deploy.example deploy/.env.deploy
# Edit deploy/.env.deploy and set IMAGE_REPOSITORY to your ECR URI
```

`.env.deploy` is gitignored and never committed.

#### 5 — Authenticate Docker with ECR

```bash
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin \
    <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

### Deploy the backend

Run the deploy script from the repository root:

```bash
cd new.java.version/deploy
./deploy.sh
```

The script:
1. Builds the Docker image using SAM (arm64/Graviton2, Docker legacy builder for Lambda-compatible manifests)
2. Pushes to ECR
3. Creates or updates the CloudFormation stack `symplex`

On the first deploy you will be prompted to confirm the changeset — review it and type `y`.

The deploy output includes the API URL:

```
Outputs
-------
ApiUrl   https://<id>.execute-api.us-east-1.amazonaws.com
```

---

### Deploy the frontend to AWS (Amplify)

Amplify Hosting serves the built SPA over HTTPS from CloudFront edge nodes. The default URL is `https://main.<appid>.amplifyapp.com`.

#### One-time setup

##### 1 — Create the Amplify app and branch

```bash
APP_ID=$(aws amplify create-app --name symplex --platform WEB \
  --query 'app.appId' --output text)
echo "AMPLIFY_APP_ID=$APP_ID"

aws amplify create-branch --app-id "$APP_ID" --branch-name main
```

##### 2 — Configure SPA routing

Amplify must rewrite unknown paths to `index.html` so React Router works on direct URL navigation:

```bash
aws amplify update-app --app-id "$APP_ID" \
  --custom-rules '[{"source":"/<*>","target":"/index.html","status":"404-200"}]'
```

##### 3 — Add values to `.env.deploy`

```
AMPLIFY_APP_ID=<value printed in step 1>
VITE_API_URL=https://<id>.execute-api.us-east-1.amazonaws.com/api
```

`VITE_API_URL` is the API Gateway URL from the backend deploy output. It is baked into the frontend bundle at build time.

#### Deploy the frontend

```bash
cd new.java.version/deploy
./deploy-frontend.sh
```

The script builds the frontend locally with `VITE_API_URL` set, packages `dist/`, uploads the artifact to Amplify, and starts the deployment. It prints the live URL and a status-check command when done.

#### Frontend URL

```
https://main.<appid>.amplifyapp.com
```

**Custom domain:** run `aws amplify create-domain-association --app-id <appid> --domain-name example.com` or use the Amplify console — Amplify provisions the ACM certificate automatically.

---

### Updating after code changes

**Backend:**
```bash
cd new.java.version/deploy
./deploy.sh
```

**Frontend:**
```bash
cd new.java.version/deploy
./deploy-frontend.sh
```

### Teardown

Remove all AWS resources:

```bash
cd new.java.version/deploy
sam delete --stack-name symplex
```

This deletes the Lambda function and API Gateway. The ECR repository, S3 bucket, and IAM role are **not** deleted automatically; remove them separately if needed:

```bash
aws ecr delete-repository --repository-name symplex --force --region us-east-1
aws amplify delete-app --app-id <amplify-app-id>
aws iam detach-role-policy --role-name symplex-lambda-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam delete-role --role-name symplex-lambda-role
```

---

## Configuration reference

### Backend (`backend/lambda/src/main/resources/application.properties`)

| Property | Default | Description |
|---|---|---|
| `quarkus.http.cors` | `true` | Enable CORS (required for browser access) |
| `quarkus.http.cors.origins` | `*` | Allowed origins — restrict to your domain in production |
| `quarkus.log.level` | `INFO` | Root log level |
| `quarkus.log.category."com.symplex".level` | `DEBUG` | Application log level |

### Frontend environment variables

| Variable | Default | Description |
|---|---|---|
| `VITE_API_URL` | `/api` | Backend base URL used at **build time** for production; set to the API Gateway URL |
| `VITE_BACKEND_URL` | `http://localhost:8080` | Vite dev-server proxy target; set to `http://localhost:3000` when using SAM Local |

### Lambda sizing (`deploy/template.yaml`)

| Parameter | Value | Notes |
|---|---|---|
| `MemorySize` | 512 MB | A full-resolution sweep (pixelStep=1) finishes in <200ms at this size |
| `Timeout` | 30 s | Well above worst-case render time |
| `Architectures` | arm64 | Graviton2 — ~20% cheaper than x86_64; builds natively on Apple Silicon |
| `ReservedConcurrentExecutions` | 2 | Hard cap on simultaneous executions; excess requests receive HTTP 429 |

Increase `MemorySize` to 1024 MB for faster cold starts at higher per-invocation cost.

### API Gateway throttling (`deploy/template.yaml`)

Default route throttle applied to all endpoints:

| Setting | Value | Notes |
|---|---|---|
| `ThrottlingRateLimit` | 10 req/s | Steady-state maximum across all callers |
| `ThrottlingBurstLimit` | 20 | Concurrent request spike limit; excess receives HTTP 429 |

### CORS (`backend/lambda/src/main/resources/application.properties`)

The default CORS origin is `*` (allow all). After deploying the frontend, restrict it to the Amplify URL:

```properties
quarkus.http.cors.origins=https://main.<appid>.amplifyapp.com
```

Then redeploy the backend with `./deploy/deploy.sh`.
