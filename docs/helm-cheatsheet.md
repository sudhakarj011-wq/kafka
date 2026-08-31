# Helm Commands Cheat Sheet

Here is a comprehensive list of important Helm commands to manage your Kubernetes applications.

## 0. Installation Commands
Alag-alag OS aur package managers ke mutabiq Helm install karne ki commands:

- **Windows (Winget, sabse aasan Windows 10/11 me)**: `winget install Helm.Helm`
- **Windows (Scoop)**: `scoop install helm`
- **Windows (Chocolatey)**: `choco install kubernetes-helm`
- **Mac (Homebrew)**: `brew install helm`
- **Linux (Script method)**: `curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 && bash get_helm.sh`

## 1. Basic Information
Commands to check if Helm is working correctly.
- **`helm version`** : Helm ka current version check karta hai.
- **`helm env`** : Helm ki environment variables batata hai.

## 2. Repository Management (Store dhoondna)
Dusre developers/companies duara banaye gaye charts ke collection ko repo (repository) kehte hain.
- **`helm repo add [name] [url]`** : Naya repo apne system me add karna (jaise: `helm repo add bitnami https://charts.bitnami.com/bitnami`).
- **`helm repo list`** : Dekhna ki aapne kitne repos add kiye hain.
- **`helm repo update`** : Add kiye huye repos ki latest information internet se update/sync karna (`apt-get update` jaisa).
- **`helm repo remove [name]`** : Kisi repo ko delete karna.
- **`helm search repo [keyword]`** : Kisi software ka chart dhundna (jaise: `helm search repo nginx`).
- **`helm search hub [keyword]`** : Helm ke Artifact Hub (global store) par chart dhundna.

## 3. Chart Creation & Debugging (Khud ka Chart banana)
Apne microservices ya custom deployments ke liye.
- **`helm create [chart-name]`** : Ek naya chart folder banana (basic structure ke sath).
- **`helm lint [chart-path]`** : Chart ki files me koi YAML ya syntax error check karna bina deploy kiye.
- **`helm template [chart-path]`** : Dekhna ki `values.yaml` aur `templates/` mix hone ke baad exactly kaunsi final Kubernetes YAML generate hogi (dry-run mode, kuch bhi deploy nahi hota).

## 4. Install, Upgrade & Rollback (Deployment)
Kisi chart ko Kubernetes par bhej kar run karna ek "Release" kehlata hai.
- **`helm install [release-name] [chart-path]`** : Chart ko pehli baar Kubernetes cluster me deploy karna. (Jaise: `helm install my-app ./mera-pehle-chart`).
- **`helm upgrade [release-name] [chart-path]`** : Agar aapne chart me ya code me changes kiye hain, toh apply karna. Naya version release hota hai.
- **`helm history [release-name]`** : Ek application ki deployment history check karna ki kitni baar upgrade hui hai.
- **`helm rollback [release-name] [revision-number]`** : Agar naya update crash ho gaya, toh pichhle kisi purane version par easily waapas jana.

## 5. Release Management (Bani banayi apps ko sambhalna)
- **`helm ls`** ya **`helm list`** : Dekhna ki aapke cluster me konsi-konsi apps (releases) chal rahi hain.
- **`helm list --all-namespaces`** : Poore Kubernetes me sabhi namespaces ke releases check karna.
- **`helm status [release-name]`** : Kisi particular app/release ki current status aur information dekhna.
- **`helm get values [release-name]`** : Dekhna ki is chalte huye app par kaunse values/settings apply ho rakhe hain.
- **`helm uninstall [release-name]`** : Application ko puri tarah Kubernetes se delete/destroy karna (Deployments, Services sab ek baar me hatega).

## 6. Helpful Flags (Command lagate waqt additional options)
- **`--dry-run`** : (Jaise `helm install my-app ./my-chart --dry-run`) Ye asal mein deploy nahi karega, bas test karega ki sab successful hoga ya nahi.
- **`--namespace [name]`** or **`-n [name]`** : Kisi specific Kubernetes namespace me deploy/check karne ke liye. (Kubernetes ke 'rooms' ki tarah).
- **`--set key=value`** : Command line se hi seedha `values.yaml` ki value change karna (Jaise: `helm install my-app ./my-chart --set replicaCount=3`).
