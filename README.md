# SYMPLEX

Web application for building simplex-lattice composition-property diagrams. Evaluates polynomial mixture models on a 2D equilateral-triangle simplex and renders the result as a PNG diagram.

**Stack:** Quarkus 3.17 · Java 21 · AWS Lambda + API Gateway · React 18 · Vite 5 · AWS SAM

---

## Repository layout

```
new.java.version/
├── backend/
│   ├── pom.xml                 Maven parent (Java 21, Quarkus BOM 3.17.4)
│   ├── core/                   Pure-Java library — parser, evaluator, renderer
│   └── lambda/                 Quarkus Lambda HTTP handler
├── frontend/                   React + Vite SPA
└── deploy/
    ├── Dockerfile              Multi-stage image for ECR/Lambda
    ├── template.yaml           AWS SAM template
    └── samconfig.toml          SAM profiles (local + prod)
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

You can also call the API directly and save the PNG (substitute port 8080 if using Option A, 3000 if using Option B):

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
import json, sys, base64
r = json.load(sys.stdin)
print(f'min={r[\"min\"]:.4f}  max={r[\"max\"]:.4f}')
open('/tmp/symplex.png','wb').write(base64.b64decode(r['imageBase64']))
print('Saved /tmp/symplex.png')
"
```

### 5 — Run the unit tests

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

Sweeps the simplex triangle, evaluates the polynomial at each pixel, and returns a 457×457 PNG encoded as Base64.

**Request fields:**

| Field | Type | Description |
|---|---|---|
| `equation` | string | Polynomial (e.g. `Y=5.76*x1+7.20*x2`) |
| `fixedVars` | `{int: double}` | 1-based variable index → fixed value; omitted variables are free |
| `mode` | enum | `REGION`, `ISOLINE_SINGLE`, or `ISOLINE_RANGE` |
| `regionMin` / `regionMax` | double | Bounds for `REGION` mode |
| `isoValue` | double | Target value for `ISOLINE_SINGLE` mode |
| `isoFrom` / `isoTo` / `isoStep` | double | Range for `ISOLINE_RANGE` mode |
| `precision` | int | Decimal digits for isoline matching (0–6) |
| `pixelStep` | int | Sweep stride — 1 = every pixel (full res), 10 = fastest/coarsest |

Exactly 3 variables must be left unfixed (free); they map to the three simplex axes.

**Response:**
```json
{
  "imageBase64": "<base64-encoded PNG>",
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

Note the `repositoryUri` from the output — you will use it in the following steps.

#### 3 — Update `samconfig.toml`

Open `deploy/samconfig.toml` and uncomment the `image_repository` line under `[default.deploy.parameters]`, replacing the placeholder with your actual ECR URI:

```toml
[default.deploy.parameters]
image_repository = "123456789012.dkr.ecr.us-east-1.amazonaws.com/symplex"
```

### Deploy

#### 1 — Authenticate Docker with ECR

```bash
aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin \
    123456789012.dkr.ecr.us-east-1.amazonaws.com
```

#### 2 — Build the Docker image

Run this from the `new.java.version/` directory (Dockerfile COPY paths are relative to it):

```bash
cd new.java.version
docker build -f deploy/Dockerfile -t symplex:latest .
```

The image is built in two stages: Maven compiles the Quarkus fast-jar in a `maven:3.9-eclipse-temurin-21` container, then the artifacts are copied into the AWS Lambda Java 21 base image. First build takes ~3 minutes; subsequent builds use Docker layer caching and finish in ~30 seconds.

#### 3 — Push the image to ECR

```bash
docker tag symplex:latest \
  123456789012.dkr.ecr.us-east-1.amazonaws.com/symplex:latest

docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/symplex:latest
```

#### 4 — Deploy with SAM

```bash
cd new.java.version/deploy
sam deploy
```

SAM will:
- Create (or update) a CloudFormation stack named `symplex`
- Create the Lambda function from the ECR image
- Create an HTTP API Gateway with routes `ANY /{proxy+}` and `ANY /`
- Print the public API URL in `Outputs`

On the first deploy you will be prompted to confirm the changeset — review it and type `y`.

#### 5 — Note the API URL

The deploy output includes:

```
Outputs
-------
ApiUrl   https://abc123.execute-api.us-east-1.amazonaws.com
```

#### 6 — Build and host the frontend

Build the frontend pointing at the live API:

```bash
cd new.java.version/frontend
VITE_API_URL=https://abc123.execute-api.us-east-1.amazonaws.com/api npm run build
```

The `dist/` directory contains a fully static site that can be served from any host — S3 + CloudFront, Netlify, Vercel, or any CDN. No server-side rendering is required.

### Updating after code changes

Rebuild and redeploy in three commands:

```bash
# From new.java.version/
docker build -f deploy/Dockerfile -t symplex:latest . && \
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/symplex:latest

cd deploy && sam deploy
```

### Teardown

Remove all AWS resources:

```bash
cd new.java.version/deploy
sam delete --stack-name symplex
```

This deletes the Lambda function and API Gateway. The ECR repository and its images are **not** deleted automatically; remove them separately if needed:

```bash
aws ecr delete-repository --repository-name symplex --force --region us-east-1
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
| `VITE_API_URL` | `/api` | Backend base URL; set to the API Gateway URL for production builds |
| `VITE_BACKEND_URL` | `http://localhost:8080` | Vite dev-server proxy target; set to `http://localhost:3000` when using SAM Local |

### Lambda sizing (`deploy/template.yaml`)

| Parameter | Value | Notes |
|---|---|---|
| `MemorySize` | 512 MB | A full-resolution sweep (pixelStep=1) finishes in <200ms at this size |
| `Timeout` | 30 s | Well above worst-case render time |

Increase `MemorySize` to 1024 MB for faster cold starts at higher per-invocation cost.

---

## Troubleshooting

**`sam local` error: "ECR image will not be built" or "no container runtime found"**  
You must run `sam build` before `sam local start-api` — SAM cannot pull from ECR for local runs. Also make sure Docker Desktop (or Finch) is running (`docker ps` should succeed) before invoking SAM. If you do not need Lambda-environment parity, use `mvn quarkus:dev` instead (Option A above), which requires no Docker at all.

**`sam local start-api` exits immediately after `sam build`**  
Make sure Docker is running and that you are in the `deploy/` directory when invoking SAM.

**`JAVA_HOME` not set error when running Maven**  
Set it explicitly: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` on macOS, or `export JAVA_HOME=/usr/lib/jvm/java-21` on Linux.

**`Exactly 3 free variables required` error from `/api/render`**  
The polynomial must have exactly 3 variables left unfixed. If your equation has 4 variables (x1–x4), fix exactly 1 of them in `fixedVars`.

**Blank canvas in the browser**  
Open the browser DevTools Network tab and inspect the `/api/render` response. A `400 Bad Request` usually means a variable count mismatch. A `500` means a parser error — check the equation string for typos (variable names must be `x1`, `x2`, … and the equation must start with `Y=`).

**Cold start latency on Lambda**  
The JVM image has a cold start of ~1–2 seconds at 512 MB. Subsequent invocations within the warm window respond in under 200ms. To eliminate cold starts in production, add a `ProvisionedConcurrencyConfig` entry to the function in `template.yaml`.
