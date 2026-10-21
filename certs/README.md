# NagarSetu V2.0 — TLS Certificates Directory
#
# Place your TLS certificate files here for HTTPS:
#   nagarsetu.crt  — Certificate (PEM format)
#   nagarsetu.key  — Private key (PEM format)
#
# For development, the deploy_edge.sh script auto-generates
# a self-signed certificate using openssl.
#
# For production, obtain a certificate from:
#   - Let's Encrypt (certbot)
#   - Your organization's CA
#
# IMPORTANT: Never commit real certificates to version control.
# These files are listed in .gitignore.
