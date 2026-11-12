# common-passwords.txt

Blocklist for `PasswordPolicy`: lowercase, one per line, `#` lines ignored. Only entries of 10+ characters are kept,
because shorter passwords are already refused by the request validation (`@Size(min = 10)`).

Source: [SecLists](https://github.com/danielmiessler/SecLists) by Daniel Miessler, MIT license:
- `Passwords/Common-Credentials/100k-most-used-passwords-NCSC.txt`
- `Passwords/Common-Credentials/10k-most-common.txt`

Fetched 2026-11-12, merged, lowercased, filtered to length >= 10, de-duplicated and sorted.
Refresh: repeat the same steps and replace the entries below the header.
