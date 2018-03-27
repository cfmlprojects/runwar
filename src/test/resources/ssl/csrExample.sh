#csr example
openssl req -nodes -newkey rsa:2048 -keyout domain.key -out domain.csr -subj "/C=US/ST=NM/L=Albuquerque/O=Global Security/OU=IT Department/CN=example.com"

#we send away the CSR, this is what they do, and send us back a cert
openssl x509 \
       -signkey domain.key \
       -in domain.csr \
       -req -days 365 -out domain.crt

# create a key with a passphrase of 'password'
openssl rsa -des3 -in selfsign.key -out selfsign.key.password

# create a key with a passphrase of 'notpassword'
openssl rsa -des3 -in selfsign.key -out selfsign.key.notpassword
