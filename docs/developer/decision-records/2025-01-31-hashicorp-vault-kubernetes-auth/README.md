# Hashicorp Vault Kubernetes Auth

## Decision

The [HashiCorp Vault Kubernetes Auth Method](https://developer.hashicorp.com/vault/docs/auth/kubernetes) is implemented for the HashiCorp Vault Extension.

## Rationale

Easy Provisioning
The authentication through a Kubernetes Service Account enables easy and automatic provisioning of new applications/services/components.
Components and their Service accounts will automatically receive certain roles and policies within Hashicorp Vault.
In this way, no manual configuration like token creation is needed any longer.

Easy Administration
Using the Kubernetes auth method, it is possible to do authentication exclusively through Kubernetes and you no longer have to interact with Hashicorp Vault, once the initial setup is complete.
This means that there no longer is any need to create and manage a token in Hashicorp Vault for each Component that uses it.
A Kubernetes Service account will suffice instead.

## Approach

### HashiCorp Vault Kubernetes Auth Extension

The `HashicorpVaultKubernetesAuthExtension` is encapsulating the HashiCorp Vault Kubernetes Auth implementaton.
The extension is serving as an injection point for the `EdcHttpClient` and `HashicorpVaultKubernetesSettings`.
An instance of `HashicorpVaultTokenProviderKubernetesAuthImpl` is provided by `HashicorpVaultKubernetesAuthExtension` and overwrites the default `HashicorpVaultTokenProvider`

### Configuration

`edc.vault.hashicorp.url`
URL of the vault instance for communication.

`edc.vault.hashicorp.auth.kubernetes.role`
The role that should be requested while using the kubernetes authentication method.

`edc.vault.hashicorp.auth.kubernetes.service.account.token`
The token associated with the Kubernetes service account, use for demo/testing purposes only.

`edc.vault.hashicorp.auth.kubernetes.ca.certificate`
The Certificate used to sign the jwt token, use for demo/testing purposes only.

`edc.vault.hashicorp.auth.kubernetes.service.account.token.path`
The Path to the Kubernetes Service Account Token, which should be used when the Connector is running inside a Kubernetes cluster and has access to the secrets.

`edc.vault.hashicorp.auth.kubernetes.expiration.threshold.seconds`
The expiration threshold for token renewal is deducted from the maximum time-to-live of the token, to avoid the token expiring during a call.

### Client

The `HashicorpVaultKubernetesAuthClient` will encapsulate the outgoing HTTP calls towards HashiCorp Vault.
The calls towards vault consist of a POST request against the `auth/kubernetes/login` endpoint of HashiCorp Vault and contain the Kubernetes Service Account Role together with its jwt Token.
The POST request adheres to the following format as described in the [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs/auth/kubernetes#authentication):

```` http request
$ curl \
    --request POST \
    --data '{"jwt": "<your service account jwt>", "role": "demo"}' \
    http://127.0.0.1:8200/v1/auth/kubernetes/login
````

The response will then contain the `client_token` which can be used to authenticate with HashiCorp Vault.

````json
{
  "auth": {
    "client_token": "38fe9691-e623-7238-f618-c94d4e7bc674",
    "accessor": "78e87a38-84ed-2692-538f-ca8b9f400ab3",
    "policies": ["default"],
    "metadata": {
      "role": "demo",
      "service_account_name": "myapp",
      "service_account_namespace": "default",
      "service_account_secret_name": "myapp-token-pd21c",
      "service_account_uid": "aa9aa8ff-98d0-11e7-9bb7-0800276d99bf"
    },
    "lease_duration": 2764800,
    "renewable": true
  }
}
````

Both the login request and response will be mapped in respective classes.
The `HashicorpVaultKubernetesAuthClient` will contain an instance of `EdcHttpClient` to make the request and receive the response.

### Hashicorp Vault Auth Implementation

The `HashicorpVaultKubernetesAuthImpl` is the centerpiece of the extension.
It uses the `HashicorpVaultTokenProviderKubernetesAuthImpl` to authenticate with HashiCorp Vault, using the Kubernetes Service Account Role and Token.
The `client_token` is then read from the response received by the `HashicorpVaultTokenProviderKubernetesAuthImpl` and stored for later use.
`HashicorpVaultKubernetesAuthImpl` also contains the logic for determining if the token needs to be renewed.

### Token Renewal

The renewal of tokens is handled in a lazy fashion on demand.

During the creation of `HashicorpVaultAuthKubernetesImpl` the first `login()` is executed and with it, a `client_token` is generated.
The time-to-live of the generated `client_token` is then used to generate an expiration timestamp.
This expiration timestamp is then used to evaluate whether the `client_token` needs to be renewed in any subsequent calls of `vaultToken()`.
If the `client_token` is about to expire, or already expired, a new `client_token` is generated with `login()` and the new expiration timestamp is set.

### Testing

For Integration Testing, the [HashiCorp Vault Testcontainer Module](https://java.testcontainers.org/modules/vault/) and the [Kind Testcontainer Module](https://testcontainers.com/modules/kindcontainer/) of the [Testcontainer Library](https://testcontainers.com/) are used.
The documentation of the Kind Testcontainer module can be found [here](https://github.com/dajudge/kindcontainer).

Both testcontainers will be run in a demo setup and configured with the calls to their command lines.

Kubernetes needs to be configured first via the `kubectl` provided by the Kind testcontainer.
For the Kubernetes auth method to work, multiple steps are needed here.
First a service account needs to be created.
Next, a secret has to be generated for the service account, which contains a signed jwt token.
Lastly, the jwt token from the secret, the Kubernetes CA certificate and the Kubernetes host URL have to be retrieved.

The HashiCorp Vault testcontainer is configured according to the [configuration section](https://developer.hashicorp.com/vault/docs/auth/kubernetes#configuration) of the Kubernetes Auth Method documentation.
This configuration enables the Kubernetes authentication functionality in HashiCorp Vault, tells HashiCorp Vault how to reach the Kubernetes Cluster Host and creates a role binding for the service account in HashiCorp Vault.
The configuration will be done through the `vault cli` provided by the HashiCorp Vault testcontainer.
First, the Kubernetes auth method has to be activated.
Then, a role binding for the service account has to be created.
Lastly, for the Kubernetes auth method, the jwt token, Kubernetes CA certificate and Kubernetes host URL collected during the Kubernetes configuration are needed to enable HashiCorp Vault to communicate with the Kubernetes `TokenReview` API.

After both testcontainers have been set up and configured, a JUnit test runtime will be started with the `extensions:common:vault:vault-hashicorp` and `:extensions:common:vault:vault-hashicorp-auth:vault-hashicorp-auth-kubernetes` modules.
A `HashicorpVaultKubernetesAuthImpl` can then be created to perform to fetch a token from vault and thereby verify the functionality of the Kubernetes auth method.