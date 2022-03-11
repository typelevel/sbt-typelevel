# Secrets

To publish to [Sonatype/Maven Central](https://central.sonatype.org/) you must obtain and install the following secrets on your repository:

- `SONATYPE_USERNAME` and `SONATYPE_PASSWORD`
- `PGP_SECRET`

Note that `PGP_PASSPHRASE` is not required for publishing with **sbt-typelevel**, although it is supported if you already have a passphrase-protected key.

The instructions provided here are adapted from [sbt/sbt-ci-release](https://github.com/sbt/sbt-ci-release/blob/9625d103cfe9fc0f727ee7903957acdf3ef85fcf/readme.md#sonatype) and [@SystemFw/base.g8
](https://github.com/SystemFw/base.g8/blob/6319421465450cd6033a92f9ade5c6fe1feafdb6/src/main/g8/dev-flow.md#ci-setup).

## Sonatype Credentials

If this is your first time publishing, first follow the [Initial Setup](https://central.sonatype.org/publish/publish-guide/#initial-setup) directions in Sonatype's [Publishing Guide](https://central.sonatype.org/publish/publish-guide/) to create an account and request publishing rights for your domain name. If you do not have a domain, you may use `io.github.your_gh_handle` as your **Group Id**.

After you've been granted publishing rights for your domain, log in to either:

- https://s01.oss.sonatype.org (all newly-registered domains)
- https://oss.sonatype.org (domains registered before February 2021)

Then:

1. Click your username in the top right, then click **Profile**
2. In the drop-down menu in the top left, select **User Token**
3. Click the **Access User Token** button to obtain your Sonatype credentials
4. Set these as the `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` secrets on your repository

## PGP Key

[Sonatype requires](https://central.sonatype.org/publish/requirements/#sign-files-with-gpgpgp) that all artifacts published to Central are cryptographically signed. **sbt-typelevel** automatically signs your artifacts in CI during publishing but you must provide a secret key.

### Option 1: Generate a key pair in your browser

You can use the following form to easily generate a key pair locally in your browser with [OpenPGP.js](https://github.com/openpgpjs/openpgpjs).

<div>
  <script src="https://cdn.jsdelivr.net/npm/openpgp@5.2.0/dist/openpgp.min.js"></script>
  <script type="text/javascript">
    async function tlGenerateKey() {
      const project = document.getElementById('project').value
      const email = document.getElementById('email').value
      const { publicKey, privateKey } = await openpgp.generateKey({
        userIDs: [{ name: `${project} bot`, email }]
      });
      document.getElementById('publicKey').value = publicKey;
      document.getElementById('privateKey').value = btoa(privateKey);
    }
  </script>

  <label for="project"><b>Your project name:</b></label><br/>
  <input type="text" id="project" name="project" size="64"/><br/>
  <label for="email"><b>Your email:</b></label><br/>
  <input type="text" id="email" name="email" size="64"/><br/>
  <button onClick="tlGenerateKey()">Generate Key</button>

</div>

#### Public key

Copy-paste and submit your public key to the [Ubuntu key server](https://keyserver.ubuntu.com/).

<textarea id="publicKey" readonly rows="16" cols="64" style="resize: none;"></textarea>

#### Private key

Copy-paste and set your private key as the `PGP_SECRET` in your repository secrets.

<textarea id="privateKey" readonly rows="16" cols="64" style="resize: none;"></textarea>

That's it!

### Option 2: Generate a key pair using GPG

First, follow the directions provided by [Sonatype](https://central.sonatype.org/publish/requirements/gpg/) to generate a key pair and submit the public key to a key server.

Then, export your secret key with the following command and set it as the `PGP_SECRET` repository secret.
```
gpg --armor --export-secret-keys $LONG_ID | base64
```
If your key is passphrase-protected, you should also set the `PGP_PASSPHRASE` secret.
