# Publishing Secrets

## Generate a PGP key

Key generation occurs locally in your browser using [OpenPGP.js](https://github.com/openpgpjs/openpgpjs).

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

### Public key

Copy-paste and submit your public key to the [Ubuntu key server](https://keyserver.ubuntu.com/).

<textarea id="publicKey" readonly rows="16" cols="64" style="resize: none;"></textarea>

### Private key

Copy-paste and set your private key as the `PGP_SECRET` in your repository secrets.

<textarea id="privateKey" readonly rows="16" cols="64" style="resize: none;"></textarea>
